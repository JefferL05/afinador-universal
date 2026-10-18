# Afinador Universal

Afinador de instrumentos musicais para Android. **Violão, guitarra, baixo e mais
33 instrumentos** — com foco em afinar rápido, em qualquer lugar, sem internet.

> **Estado atual:** Fases 1 a 5 concluídas. Restam onboarding/acessibilidade
> (6) e CI/release/assinatura (7) — ver [Fases](#fases-restantes).

---

## O problema técnico deste app

Tudo aqui se resume a uma pergunta: **com que precisão e que atraso o app
descobre que a corda tocou 82,41 Hz?**

O algoritmo é **YIN** (Cheveigné & Kawahara, 2002). Três decisões definem o
comportamento:

| Decisão | Valor | Por quê |
|---|---|---|
| Janela de análise | 4096 (85 ms) · 8192 para instrumentos graves | 2048 não alcança o E1 (41 Hz) do baixo. O período do E1 tem 1165 amostras a 48 kHz e a janela precisa cobrir ~2 períodos. |
| Fonte de áudio | `UNPROCESSED` → `MIC` → `VOICE_RECOGNITION` | Processadores de voz do fabricante aplicam passa-banda e eco que **distorcem o fundamental** — o afinador lê errado justamente em graves e agudos. |
| Correção de oitava | só sobe, e só se o vale em `tau/2` for >2x mais profundo | Onda quadrada e dente de serra também têm vale em `tau/2`. Sem esse critério o app lê uma oitava acima e fica inutilizável para guitarra. |

### Precisão medida

Medido a 48 kHz com o algoritmo exato que está no código:

| Sinal | Janela | Erro |
|---|---|---|
| Senóide limpa (440 Hz) | 4096 | **0,07 cents** |
| Corda (5 harmônicos) 82,41 Hz | 4096 | **0,00 cents** |
| Corda + 5% de ruído, 130,81 Hz | 4096 | **1,05 cents** |
| Corda + 5% de ruído, 41,20 Hz | 8192 | **0,14 cents** |
| Onda quadrada 82,41 Hz | 4096 | −0,46 cents (sem pular oitava) |

### A armadilha da recorrência O(1)

A função de diferença do YIN costuma ser "otimizada" para
`d(tau) = d(tau-1) + remove - add`. **Isso está errado.** A correlação cruzada
não é atualizável em tempo constante; só os termos `r0` e `rTau` são.

A consequência é silenciosa: funciona em senóides curtas e **colapsa em silêncio
e ruído**, com valores negativos numa soma de quadrados. Foi exatamente o que
aconteceu durante o desenvolvimento — o detector "validava" em 440 Hz e falhava
em 12 de 12 testes reais.

A implementação correta está em [`YinDetector.kt`](app/src/main/java/com/afinu/app/core/audio/YinDetector.kt)
e os testes que travam o comportamento estão em
[`YinDetectorTest.kt`](app/src/test/java/com/afinu/app/core/audio/YinDetectorTest.kt).

---

## Como compilar

### Requisitos
- **JDK 17** (obrigatório — o Gradle 8.x recusa versões anteriores)
- **Android SDK 35**
- Gradle é baixado automaticamente pelo *wrapper*

### Passos

```bash
# 1. Clone e entre na pasta
cd afinador-universal

# 2. Aponte o SDK (ou abra o projeto no Android Studio, que faz isso por você)
echo "sdk.dir=C:/Users/SEU_USUARIO/AppData/Local/Android/Sdk" > local.properties

# 3. Compilar e instalar
./gradlew installDebug
```

### Rodar os testes

```bash
./gradlew testDebugUnitTest      # JVM puro, sem emulador - roda em segundos
./gradlew connectedAndroidTest  # precisa de aparelho conectado
```

---

## Gerar o APK release assinado

```bash
# 1. Criar a keystore (faça UMA vez, guarde em local seguro)
keytool -genkey -v \
  -keystore afinador.jks \
  -alias afinador \
  -keyalg RSA -keysize 4096 -validity 10000

# 2. Copiar o modelo e preencher
cp keystore.properties.example keystore.properties
# (editar com as senhas e o caminho absoluto do .jks)

# 3. Gerar
./gradlew assembleRelease    # -> app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease      # -> app/build/outputs/bundle/release/app-release.aab
```

> `keystore.properties` e `*.jks` estão no `.gitignore`. **Perder a keystore
> impede de atualizar o app na Play Store** — faça backup em dois lugares.

---

## Publicar na Play Store

1. `app-release.aab` em **Produção → Novo lançamento**
2. **Política de privacidade**: obrigatória. Este app não coleta nada — declare
   "Sem coleta de dados" no Data Safety
3. **Classificação de conteúdo**: consulte o IARC
4. **Palavras-chave sugeridas**: *afinador, violão, guitarra, contrabaixo,
   metrônomo, afinação, viola, ukulele, sax, flauta*
5. Envie também o APK para distribuição fora da loja, se quiser

---

## Instrumentos suportados

### Cordas
| | |
|---|---|
| Violão (12 afinações) | Guitarra 7 e 8 cordas |
| Violão 12 cordas | Baixo 4, 5 e 6 cordas |
| Contrabaixo acústico | Ukulele e cavaquinho |
| Violino, viola, violoncelo | Banjo, bandolim, mandolino |
| Sitar, laúd, balalaika | Guitarra portuguesa, guzzar, lap steel |

### Modo cromático (sem afinação fixa)
Flauta doce, flauta, recorder, oboé, clarinete, saxofone, trompa, trombone,
tuba, harmônica, gaita, piano/teclado, acordeão, bateria.

Todos os intervalos entre cordas são **calculados** a partir das notas MIDI
declaradas — nunca digitados à mão. Digitar "+5, +5, +5, +4, +5" em algum
lugar é a forma mais comum de um afinador errar a 4ª corda.

---

## O simulador de capo

Um capo **não** desafina o instrumento: encurta o vibrador em todas as cordas
pelo mesmo tanto. Isso equivale a uma translação de N semitons aplicada à
afinação inteira, e os intervalos entre cordas ficam intactos — há um teste que
verifica isso explicitamente para as casas mais usadas.

A consequência prática é que **o medidor mira a nota com o capo aplicado**:

| | Sem capo | Capo na 3ª casa |
|---|---|---|
| 6ª corda do violão | E2 (82,41 Hz) | **G2 (98,00 Hz)** |
| 1ª corda do violão | E4 (329,63 Hz) | **G4 (392,00 Hz)** |

O músico afina o que ele **ouve**, e é com isso que compara com outro
instrumento. Mostrar E2 seria mostrar a nota física, não a musical.

---

## O metrônomo não pode derivar

Um metrônomo que "parece funcionar" e vai arrastando é **pior do que nenhum**:
o músico acelera junto sem perceber, porque todo mundo toca apoiado na
referência que o metrônomo dá.

O relógio mestre é o `AudioTrack`, não o sistema. Cada clique é escrito N
amostras depois do anterior. A armadilha é o arredondamento: a 127 BPM um tempo
mede 22 677,16 amostras, e só existem posições inteiras. Medi as três formas de
escolher, simulando 10 minutos:

| Forma de calcular a posição do clique | Erro em 10 min |
|---|---|
| Somar o inteiro arredondado a cada clique | **262 ms** |
| Andar em compassos inteiros (4 × 22 677) | **262 ms** |
| `round(contador × 22 677,16)` — contador absoluto | **0 ms** |

As duas primeiras derivam porque cada passo carrega um erro próprio. Só a
terceira fica presa em meio amostra, para sempre. O motor usa um contador de
subdivisões desde o início e recalcula a posição a partir dele —
nunca `posição + passo`.

`MetronomeTimingTest` simula 360 000 cliques (10 minutos a 300 BPM com
subdivisão quádrupla) e exige erro < 0,5 amostra. O pior caso medido nos testes:
**0,4965 amostra, ou 10 microssegundos**.

---

## Acordes derivados da afinação, não guardados

Um diagrama de acorde pronto só existe para afinação padrão. Com drop D, capo ou
ukulele, a mesma figura passa a representar outro acorde — e aí a imagem está
errada. Este app não guarda diagramas: ele **deriva** as formas a partir da
afinação efetiva, buscando por janela deslizante de 5 casas (todo mundo alcança
uma casa 8 acima da menor, então toda forma tocável cabe em alguma janela).

Ao escolher um acorde, o app encontra as formas válidas: todas as notas
pertencem ao acorde, o baixo é uma nota do acorde e a pegada cabe na mão.

### Um detalhe que quebraria tudo

As classes de altura de um acorde são **relativas à tônica**. Em Si menor, o
offset 0 é a nota Si, de classe 11 — não 0. Comparar a nota absoluta contra os
offsets rejeitaria a própria tônica de qualquer acorde que não seja de Dó, e o
buscador devolveria formas erradas ou nenhuma. `ChordQuality.contains(rootMidi,
midi)` converte para intervalo antes de comparar.

---

## Arquitetura

```
core/
  audio/     YinDetector, AudioCapture, PitchSmoother, NoteMapper
  model/     Note, Instrument, TuningPreset
  util/      MusicTheory (nota <-> frequência), Capo
data/
  catalog/   InstrumentCatalog (dados puros de todos os instrumentos)
  prefs/     SettingsRepository (DataStore)
  db/        Room: AppDatabase, FavoriteTuningDao
  repository/FavoritesRepository, FavoritesCodec (import/export JSON)
feature/
  tuner/     TunerScreen, TunerViewModel, TunerLogic, componentes Compose
  settings/  SettingsScreen, SettingsViewModel
  notes/     Cartão de notas
  metronome/ MetronomeEngine, MetronomeTiming, MetronomeScreen
  chords/    ChordsViewModel, ChordsScreen (diagrama e braço)
  recorder/  IdeaRecorder, RecorderScreen
core/util/   MusicTheory, Capo, Fretboard, ChordFinder, ChordNames
ui/
  navigation/MainScreen (bottom bar), Destination
  components/ChipRow
  theme/     Material 3 + Material You
```

Regras que o código segue:

- **Zero alocação no caminho quente.** `AudioCapture` e `YinDetector` reutilizam
  buffers. O detector dispara ~12x por segundo.
- **Regras de decisão isoladas de Android.** `TunerLogic`, `Capo` e `NoteMapper`
  são Kotlin puro, testáveis sem emulador nem microfone.
- **UI como função do estado.** As telas desenham `UiState` e não calculam nada.
- **Favoritos guardam IDs, não notas.** Corrigir um erro no catálogo corrige
  também os favoritos já salvos pelo usuário.
- **Uma permissão só.** `RECORD_AUDIO`. O manifesto **não declara `INTERNET`** —
  não há caminho de código capaz de enviar áudio para fora.
- **Trocar de instrumento reinicia a captura; trocar de afinação ou capo não.**
  Só o instrumento muda o tamanho da janela de análise.

---

## Fases restantes

| Fase | Conteúdo | Estado |
|---|---|---|
| 1 | Fundação Gradle, Compose, Hilt, tema | ✅ |
| 2 | Motor de áudio (YIN) + testes | ✅ |
| 3 | Tela de afinador, medidor, feedback | ✅ |
| 4 | Navegação, ajustes, favoritos, capo, cartão de notas | ✅ |
| 5 | Metrônomo, acordes, braço, gravador | ✅ |
| 6 | Onboarding, acessibilidade, widgets | ⬜ |
| 7 | CI/release, assinatura, Play Store | ⬜ |

---

## Privacidade

- Nenhum dado sai do aparelho
- Nenhuma telemetria, nenhum analytics, nenhum crash reporting
- O app não tem permissão `INTERNET`, então **não é tecnicamente possível**
  enviar áudio a lugar nenhum
- Nada é gravado em disco

---

## Licença

A definir pelo autor.