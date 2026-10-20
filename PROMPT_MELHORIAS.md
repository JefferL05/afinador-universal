# Prompt — Melhorias do Afinador Universal

> **Como usar este documento.** Ele é uma ordem de trabalho autocontida: todo achado
> abaixo foi **observado em um aparelho**, com o app instalado e o locale em pt-BR, e
> traz o arquivo/linha de origem. Não é lista de suspeitas — é o que foi visto na tela.
>
> **Regra de ouro:** nenhuma mudança pode quebrar as duas promessas verificáveis do app —
> **sem a permissão `INTERNET`** e **análise 100% local**. Qualquer solução que precise
> de rede, SDK de terceiro ou nova permissão está fora de escopo.

---

## 0. Contexto

- **App:** `com.afinu.app` (debug: `com.afinu.app.debug`) — afinador Android nativo
  em Kotlin 2.0.21 + Compose + Material 3 + Hilt, detecção YIN.
- **Estado atual:** build verde — 172 testes, 0 erros de lint, APK debug 17,5 MB e
  release 1,5 MB (R8). CI roda em `master`.
- **`minSdk 24` / `targetSdk 35` / `compileSdk 35`.**
- **Idiomas:** `values/` = pt-BR (padrão), `values-en/`, `values-es/` — 136 strings cada,
  paridade total confirmada.

### Ambiente usado na validação (reproduzir antes de mexer)

```
AVD      android-34, AOSP (default), x86_64, 540x1200 @240dpi, 4 GB RAM
locale   cmd locale set-app-locales com.afinu.app.debug --locales pt-BR
APK      app/build/outputs/apk/debug/app-debug.apk
```

> **Por que imagem AOSP e não `google_apis`:** a imagem com Google Play torna o
> `com.android.systemui` tão lento que dispara ANR constante e o diálogo
> "System UI isn't responding" tampa o app inteiro, tornando teste de UI impossível.
>
> **Por que com janela quando possível:** `-no-window` roda em
> `qemu-system-x86_64-headless`, que não tem aceleração de GPU. Sem isso a
> renderização cai para software e o teste inteiro trava.
>
> **Cuidado com o OneDrive:** o caminho do `local.properties` deve usar barras
> **normais** (`sdk.dir=C:/...`). Barras invertidas viram caminho inexistente porque o
> `java.util.Properties` descarta escapes.

---

## 1. P0 — Bugs funcionais comprovados

### 1.1 O resultado da permissão do microfone é **descartado**

**O que foi visto:** com o microfone revogado, o app mostra
"Não foi possível acessar o microfone" / "Tentar de novo". O botão abre o diálogo do
sistema, o usuário escolhe "Durante o uso do app", o diálogo fecha — **e a tela
continua exatamente igual**, em estado de erro. Só `am force-stop` + relançar recupera
(depois do reinício, mostrou "Ouvindo").

**Causa raiz:**

```kotlin
// MainActivity.kt:22-24   ← callback VAZIO
private val requestPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { /* O estado da tela reflete a ausencia via MicState.hasPermission */ }
```

```kotlin
// TunerViewModel.kt:142   ← definido, mas NUNCA chamado (1 ocorrência no repo: a própria definição)
fun onPermissionResult(granted: Boolean) { ... }
```

A intenção está documentada no comentário, mas ninguém liga o `granted` ao ViewModel.

**Correção:**

1. `MainActivity` deve expor o resultado: trocar o callback vazio por uma chamada que
   alcance o `TunerViewModel` (ou suba um evento que `AppRoot`/`MainScreen` repassa).
2. Verificar se `TunerViewModel.onPermissionResult(granted)` já existe com a lógica
   certa (linhas 142–149) e ligá-la — ela já trata `granted = false` indo para
   `MicState.NeedsPermission`.
3. Garantir que a **transição seja visível sem reiniciar o app**.

**Teste de aceite:** revogar via
`adb shell pm revoke com.afinu.app.debug android.permission.RECORD_AUDIO`,
relançar, tocar em "Tentar de novo", conceder → o estado deve virar **"Ouvindo"**
sem reiniciar o app.

---

### 1.2 O ajuste "Manter tela acesa" **não tem efeito**

**O que foi visto:** o toggle existe em Ajustes → Retorno e grava no DataStore. Mas o
Afinador mantém a tela acesa mesmo com o ajuste **desligado**.

**Causa raiz:**

```kotlin
// MainActivity.kt:39-44  ← liga incondicionalmente, nunca lê a preferência
override fun onResume() {
    super.onResume()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
```

A preferência atravessa todo o caminho DataStore → `SettingsViewModel` → `SettingsScreen`
(`keep_screen_on`) e **morre ali** — ninguém a aplica.

**Correção:** ler `SettingsRepository.keepScreenOn` no `MainActivity` (ou expô-lo ao
Compose via `LocalContext`/efetivação em `SideEffect`) e aplicar
`window.addFlags` / `window.clearFlags` conforme o valor, reavaliando em `onResume`.

**Teste de aceite:** desligar em Ajustes → a tela apaga normalmente após o timeout;
ligar → permanece acesa. O valor deve sobreviver a um reinício do app.

---

### 1.3 Conteúdo desenha **por baixo** das barras do sistema

**O que foi visto:** em **todas as 6 telas** o título fica dentro da área da barra de
status. No afinador, "Violão" (y=41) sobrepõe o relógio e o botão "Trocar" (y=60)
sobrepõe wifi/bateria. A área da barra vai de y=0 a y=127 nesta densidade.

**Causa raiz:**

```kotlin
// MainActivity.kt:28
enableEdgeToEdge()
```

```xml
<!-- themes.xml -->
<item name="android:statusBarColor">@android:color/transparent</item>
<item name="android:navigationBarColor">@android:color/transparent</item>
```

`enableEdgeToEdge()` está ligado e as barras são transparentes, mas **nenhuma tela
consome `WindowInsets`** — a busca por `WindowInsets`, `statusBarsPadding`,
`navigationBarsPadding` e `safeDrawing` no código inteiro não achou **nenhuma ocorrência**.

Com `targetSdk 35`, o Android **impõe** edge-to-edge — não é opcional.

**Correção:** aplicar os insets no contêiner único que envolve as telas (não em cada
tela) — `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` em `AppRoot`/`MainScreen`,
e no `NavigationBar` respeitar `WindowInsets.navigationBars`.
Evitar `statusBarsPadding` duplicado em telas que já estão dentro do contêiner.

**Teste de aceite:** em qualquer tela, o título começa **abaixo** do relógio do
sistema; o rodapé não fica atrás da barra de navegação; as 6 abas conferidas.

---

## 2. P1 — Defeitos visíveis de layout

### 2.1 Rótulos da barra inferior cortados **no meio da palavra**

**O que foi visto:** "Afinad / or", "Metrôn / omo", "Acorde / s" — três dos seis rótulos
quebram em duas linhas. Motivo: 6 itens em 540 px não cabem.

**Onde:** `ui/navigation/MainScreen.kt:67`
```kotlin
label = { Text(stringResource(destino.labelRes)) }
```

**Correção (escolher uma):**
- **(a)** `Text(..., maxLines = 1, softWrap = false)` — corta com reticências, não parte a palavra;
- **(b)** 5 abas + "Mais" (não recomendado — afasta o afinador da primeira tela);
- **(c)** rótulo curto por aba em telas estreitas (ex.: "Afin.", "Metrô.").

**Aceite:** nenhum rótulo quebra em duas linhas a 360 dp de largura.

---

### 2.2 Chips com texto partido ao meio

**O que foi visto:** no metrônomo, o preset "Moderato" virou três linhas
"Mo / der / ato" numa caixa de 39×72 px. O mesmo padrão afeta qualquer `ChipRow` com
texto longo.

**Causa raiz:**
```kotlin
// ui/components/ChipRow.kt:39-40
Row(modifier = modifier.horizontalScroll(rememberScrollState()))
```
Um `Row` com `horizontalScroll` mede os filhos com constraint de largura limitado →
o `Text` (linha 49: `Text(label)`) encolhe e quebra linha. O scroll nunca chega a
acontecer porque os filhos se espremem para caber.

**Correção:** trocar por `LazyRow` (mede filhos com largura ilimitada), **ou** usar
`Row` + `width(IntrinsicSize.Min)`, **e** `Text(maxLines = 1, softWrap = false)`.

**Aceite:** nenhum chip quebra texto em mais de uma linha, e a linha de chips rola
horizontalmente quando não cabe.

---

### 2.3 O "Braço" aparece **vazio**

**O que foi visto:** a grade do braço desenha só as linhas, sem nenhuma nota marcada.
O usuário não entende o que aquilo representa.

**Causa:**
```kotlin
// ChordsViewModel.kt:35
val highlightSemitones: Int? = null,
```
Só muda quando o usuário toca numa nota (`destacar`). Ao abrir a tela com C maior
selecionado, o braço fica sem marcação.

**Correção:** popular `highlightSemitones` com a **raiz do acorde** ao montar o estado
(inclusive ao trocar de acorde), e marcar também as demais notas do acorde em cor
diferente. As posições já vêm de `Fretboard.positionsForNoteName` (mesma fonte usada
pelo afinador), então a consistência é garantida.

**Aceite:** ao abrir Acordes com "C" selecionado, a grade já mostra as posições de Dó.

---

### 2.4 Cartão de notas mostra "-900 cents"

**O que foi visto:** C4 → "-900 cents", C#4 → "-800", D4 → "-700"… parece uma tabela de
erros, não uma referência.

**Onde:** `feature/notes/NotesCardScreen.kt:96`
```kotlin
centsFromA4 = (midi - MusicTheory.MIDI_A4) * 100,
```
Matematicamente correto (Dó está 9 semitons abaixo de Lá4), mas inútil para o público.

**Correção:** mostrar o que o músico procura — a **distância em relação à nota anterior
dentro da oitava** (0, 100, 200…), ou o **intervalo por nome** ("Segunda menor",
"Terça maior"), ou o **nome da casa no braço**. Mantendo `centsFromA4` se for útil em
testes, mas **não exibir esse número bruto** ao usuário.

**Aceite:** nenhuma linha do cartão mostra valores negativos grandes; a coluna lê como
tabela de referência.

---

### 2.5 Fluxo da permissão: `Failed` quando deveria ser `NeedsPermission`

**O que foi visto:** com a permissão ausente, o app foi direto para
"Não foi possível acessar o microfone" + "Tentar de novo" — em vez de
"O afinador precisa do microfone" + "Permitir".

**Por quê:** `startAudio` tenta abrir a captura, que falha por falta de permissão, e o
falha vira erro genérico:
```kotlin
// TunerViewModel.kt:167-171
val started = audioCapture.start(frameSize)
if (!started) {
    _uiState.update { it.copy(micState = MicState.Failed("Não foi possível acessar o microfone")) }
```
Já existem os dois estados certos (`MicState.NeedsPermission` nas linhas 147 e 153) e as
**4 strings de explicação** — `mic_permission_title`, `mic_permission_body`,
`mic_permission_button`, `mic_permission_later` — escritas exatamente para isso e
**totalmente órfãs**.

**Correção:**
1. Checar a permissão **antes** de chamar `audioCapture.start()` e mapear ausência para
   `MicState.NeedsPermission`, não `Failed`.
2. **Ligar as 4 strings** num diálogo de pré-permissão antes do prompt do sistema — o
   diálogo nativo aparece em inglês (locale do sistema, ignora o locale por app) e sem
   contexto algum. Para um app cuja promessa é privacidade, é o texto mais importante
   do produto.
3. Separar os motivos de `Failed`: "microfone ocupado por outro app" já é detectado
   (`AudioCapture.kt:143`) e merece mensagem própria.

**Aceite:** primeira execução → explicação do app → diálogo do sistema → "Ouvindo";
usuário nega → explicação da negativa + caminho para as Ajustes do sistema.

---

## 3. P2 — Recursos faltando

### 3.1 Ícone do launcher sem fallback (bloqueia a Play)

**O que foi visto:** o APK só contém `mipmap-anydpi-v26`:
```
mipmap/ic_launcher        (anydpi-v26) res/mipmap-anydpi-v26/ic_launcher.xml
mipmap/ic_launcher_round  (anydpi-v26) res/mipmap-anydpi-v26/ic_launcher.xml
```
Não existe **nenhum** PNG. Consequências:

- `minSdk 24` → em Android 7 (API 24/25) o app aparece **sem ícone**;
- a Play **exige** um PNG **512×512**, e `store/` não tem nenhuma imagem (só
  `CHECKLIST.md`, `data-safety-pt-BR.xml`, `PRIVACY-NOTES.md` e 3 `listings/*.txt`).

**Correção:**
1. Gerar os rasters de `mipmap-mdpi` até `xxxhdpi` (a partir do
   `drawable/ic_launcher_foreground.xml` + `@color/ic_launcher_background`);
2. Exportar `store/icon-512.png` e um `store/feature-graphic.png` (1024×500);
3. Confirmar `aapt2 dump badging` listando `application-icon-*` apontando para PNG.

---

### 3.2 Strings órfãs (12)

`app_name`, `mic_permission_title`, `mic_permission_body`, `mic_permission_button`,
`mic_permission_later`, `metronome_coming_soon`, `chords_coming_soon`,
`recorder_saved`, `recorder_failed`, `muted_string`, `widget_label`,
`widget_description`.

- **`mic_permission_*`** → usar no item 2.5 (era isso que faltava).
- **`metronome_coming_soon` / `chords_coming_soon`** → **matar**: as duas telas já
  existem e funcionam; é resto de fase anterior.
- **`recorder_saved` / `recorder_failed`** → o gravador **não dá nenhum feedback** ao
  salvar ou falhar. Implementar (snackbar).
- **`widget_label` / `widget_description`** → usadas no manifesto, não são órfãs de
  verdade; só não aparecem via `R.string` em Kotlin.

---

### 3.3 Ajustes: uso de espaço

- **Afinações:** um chip por linha (~84 dp cada) → as 8 afinações de um violão ocupam
  672 dp. Trocar por `FlowRow` ou `ExposedDropdownMenu`.
- **"Favoritos"**: seção aparece espremida no fim do scroll (altura 15 px, cortada).
  Ficaria melhor como tela própria ou num `LazyColumn` com seções reais.
- **Instrumento**: `ChipRow` com 34 itens — combina com o `LazyRow` do item 2.2.

---

### 3.4 Onboarding: vazio vertical

Nas 3 páginas há um buraco de **≈1/3 da tela** entre os indicadores de página e os
botões. O conteúdo está todo no topo. Centralizar verticalmente (ou ancorar o
conteúdo ao centro com os botões em `bottom`) resolve nas três páginas.

---

## 4. Ordem de execução sugerida

| # | Tarefa | Esforço | Risco |
|---|--------|---------|-------|
| 1 | 1.3 — `WindowInsets` em `AppRoot`/`MainScreen` | médio | alto (afeta as 6 telas) |
| 2 | 1.1 — ligar `onPermissionResult` | pequeno | médio |
| 3 | 2.5 — `NeedsPermission` + diálogo de pré-permissão | médio | médio |
| 4 | 1.2 — aplicar `keepScreenOn` | pequeno | baixo |
| 5 | 2.1 + 2.2 — `maxLines`/`LazyRow` em nav e chips | pequeno | baixo |
| 6 | 2.3 — destacar a raiz do acorde no braço | pequeno | baixo |
| 7 | 3.1 — ícones raster + PNG da loja | médio | baixo |
| 8 | 3.2 — feedback do gravador, limpar strings | pequeno | baixo |
| 9 | 2.4 + 3.3 + 3.4 — leitura do cartão, Ajustes, onboarding | médio | baixo |

Fazer **1 por commit**, na ordem, com a verificação da seção 5 antes de seguir.

---

## 5. Critérios de aceite globais

Rodoar o app inteiro depois de cada mudança:

1. **Visual:** as 6 abas (Afinador, Metrônomo, Notas, Acordes, Gravar, Ajustes) —
   título abaixo da barra de status, nenhum texto partido ao meio, nada atrás do
   rodapé.
2. **Permissão:** fluxo do zero → explicação → diálogo do sistema → "Ouvindo", **sem
   reiniciar**; e negando, o app explica e não fica preso em "Tentar de novo".
3. **Preferências:** "Manter tela acesa" e "Vibração" efetivos ao desligar.
4. **Qualidade:** `gradlew test lintDebug` verdes (baseline atual: **172 testes**,
   **0 erros** de lint) e `tools/preflight.ps1` **9/9**.
5. **Privacidade:** `aapt2 dump permissions` continua mostrando **apenas**
   `RECORD_AUDIO` e `VIBRATE` — nenhuma `INTERNET`.
6. **Locales:** 136 strings em `values`, `values-en` e `values-es`, com as mesmas
   chaves em todos.
7. **Teste de regressão:** rodar a pilha de testes de `TunerLogic` e `MetronomeTiming`
   (o metrônomo já mede 10 µs em 360.000 cliques e o YIN acerta 0,07 cent — esses
   números são contrato, não acaso).

---

## 6. Fora de escopo

- Anúncios, pagamentos, telemetria, qualquer SDK de terceiro.
- Qualquer permissão nova além de `RECORD_AUDIO` e `VIBRATE`.
- Conta de usuário, login, nuvem, sincronização.
- Renomear o app ou mudar o `applicationId`.
- Reescrever o motor de detecção YIN (já validado a 0,07 cent).
- Subir para a Play Store — depende do keystore e do passo §6 do `store/CHECKLIST.md`,
  que continua pendente.
