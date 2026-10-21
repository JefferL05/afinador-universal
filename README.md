# Afinador Universal

Afinador de instrumentos musicais para Android. **Violão, guitarra, baixo e mais
33 instrumentos** — com foco em afinar rápido, em qualquer lugar, sem internet.

> **Estado atual:** as 7 fases estão implementadas, **compilam, passam nos 172
> testes e geram APK** — debug e release com R8. O que ainda depende de um
> aparelho está em [O que foi verificado](#o-que-foi-verificado).
>
> Esta é a **primeira vez** que o projeto foi compilado. A revisão encontrou
> **12 erros de compilação e 29 testes falhando**, que nenhuma das verificações
> estáticas apanhou. O que eles eram está em
> [O que a compilação encontrou](#o-que-a-compilação-encontrou).

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

| Sinal | Janela | Erro verificado |
|---|---|---|
| Senóide limpa, 440 Hz | 4096 | abaixo de 0,5 cent |
| Corda (5 harmônicos), 82,41 Hz | 4096 | abaixo de 0,2 cent |
| Corda + 5% de ruído, 82 a 330 Hz | 4096 | abaixo de 1 cent |
| Corda + 5% de ruído, 41,20 Hz | 8192 | **1,34 cents** (medido) |
| Onda quadrada e dente de serra | 4096 | sem pular a oitava |

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

### As notas do acorde, e o som de cada uma

O nome do acorde sozinho não diz quais notas o formam, e é exatamente isso que
quem está começando não sabe. A tela mostra uma faixa **"Notas do acorde"** com
a tônica preenchida e em negrito — é ela que dá nome ao acorde — e as demais
notas neutras.

A **oitava** vem da pegada, não de uma média teórica: a nota exibida é a nota
que está na pegada. Para um Dó maior no violão, isso é C3, E3 e G3, e não
"Dó, Mi, Sol" sem registro. `ChordTones` (em `core/util`, com 12 testes) faz
essa conta.

Tocar numa nota a toca, na altura correta: a frequência sai do **A4 das
preferências**, então com o afinador em 432 Hz o Dó que o app mostra é o Dó de
432. A nota que está soando ganha destaque, para o olho acompanhar o ouvido.

O som usa o mesmo `ConfirmTonePlayer` que o widget usa para a nota de
referência, com duas correções que só apareceram aqui: ele deixou de ser
singleton (uma track compartilhada faria o afinador cortar o som do acorde no
meio) e a track em `MODE_STATIC` passa a ser refeita quando o tom seguinte não
cabe no buffer — escrever mais do que cabe falha **em silêncio**, e o som
simplesmente não sai.

---

## Acessibilidade

Um afinador é, por natureza, **visual e sonoro**. Para quem usa TalkBack, o bip
de confirmação existe mas dura 110 ms e passa despercebido — sem um sinal
explícito, não há nenhuma forma de saber que a nota ficou afinada.

O que foi feito:

| Onde | O quê |
|---|---|
| Afinador | Anúncio "… afinado" via `LiveRegion` **só na entrada** da zona, em modo `Polite` (não interrompe o que o leitor estiver falando) |
| Medidor | `stateDescription` em vez de `contentDescription`: o TalkBack anuncia a variação sozinho, sem o usuário navegar até o elemento |
| Sliders (A4, ajuste fino, BPM) | `labelAndState` — sem isso o TalkBack anuncia "controle ajustável" e nada mais, e o usuário mexe no A4 às cegas |
| Botão play/pause | 88 dp, bem acima do mínimo de 48 dp, porque é apertado no meio de uma frase com uma mão ocupada |
| Indicador de páginas | Um nó só ("página 2 de 3"). Três bolinhas separadas fariam o leitor anunciar "círculo, círculo, círculo" |
| Ícones decorativos | `contentDescription = null` quando o texto ao lado já descreve tudo, para o leitor não ler duas vezes |

---

## Widget

O widget **não** mostra a nota que está tocando. Isso exigiria manter o
microfone ligado em segundo plano, o que o Android restringe a apps de gravação
de áudio — o app seria rejeitado na Play Store e, quando permitido, o dreno de
bateria o tornaria inútil.

O que ele faz é o que ajuda de verdade fora do app: mostra a afinação atual e
**toca a nota de referência** ao toque. Afinar "de ouvido" contra a referência
do dedo, longe do instrumento, é o caso em que o celular está no bolso e a mão
ocupada.

Detalhe que evita um bug real: a frequência viaja **pronta** nos `extras` do
`PendingIntent`, calculada com o A4 do usuário. Se o receiver lesse as
preferências de novo, o widget e o som poderiam divergir — a linha marcaria "Dó"
e tocaria Sol. `RemoteViews` também não aceita tema nem `?attr`, então todas as
cores do layout são literais.

---

## O que foi verificado

O projeto **compila, passa nos 172 testes e gera APK**. Isso foi feito nesta
máquina, com JDK 17.0.20 (Temurin) e Android SDK 35.

```
gradlew testDebugUnitTest   BUILD SUCCESSFUL   172 testes, 0 falha
gradlew lintDebug           BUILD SUCCESSFUL   0 erros (69 avisos)
gradlew assembleDebug       BUILD SUCCESSFUL   app-debug.apk          17,6 MB
gradlew assembleRelease     BUILD SUCCESSFUL   app-release-unsigned.apk  1,5 MB
```

O APK de release passou pelo R8. Conferido com `aapt2` no próprio arquivo: as
duas permissões certas, os dois receivers do widget com os nomes preservados e
os inicializadores do Hilt presentes. **O `mapping.txt` sai no lugar certo.**

Além do build, `tools/preflight.ps1` roda em segundos sem JDK e é o portão da
CI antes de qualquer compilação:

```
== 1. Delimitadores balanceados      ok   69 arquivos Kotlin
== 2. Recursos R.* existem          ok   146 referências resolvem
== 3. Idiomas sincronizados          ok   3 locales com 136 chaves idênticas
== 4. Import declarado e não usado   ok   nenhum import ocioso
== 5. Segredos no índice do git      ok   nenhuma keystore versionada
== 6. Manifesto                      ok   sem INTERNET, só RECORD_AUDIO e VIBRATE
== 7. Texto corrompido               ok   nenhum caractere inválido
== 8. XML bem formado                ok   todos válidos
== 9. local.properties legível       ok   sdk.dir com barras normais
```

O item 9 é novo e veio de uma perda de tempo concreta, descrita abaixo.

## O que a compilação encontrou

Antes desta rodada, o projeto tinha **7 fases escritas, 9.733 linhas e 175
testes, e nunca tinha sido compilado**. A análise estática e os testes escritos
sem rodar deixavam passar coisas que o compilador e o JVM pegam na hora:
**12 erros de compilação e 29 testes falhando**.

### Erros que nenhuma verificação estática pega

| Erro | Por que só o compilador acha |
|---|---|
| `val` reatribuído em 3 classes | Um `val` é constante por definição; a leitura estática não é um compilador |
| `continue` dentro de `run { }` | Recurso experimental do Kotlin — compila com opt-in, não sem |
| `stringResource` dentro de lambda de semântica | `@Composable` só pode ser chamado de contexto composable |
| `stateDescription` sem import | Atribuição a propriedade exige o import, não o nome qualificado |
| `ChipRow(selected = Int)` com `List<Pair>` | Os tipos são incompatíveis, mas o erro aponta para a chamada inteira |

### Bugs de lógica que os testes acharam

Estes são os que importam. **Oito deles eram reais; quatro eram o teste
esperando a coisa errada.** Os dois grupos estão misturados abaixo porque é a
ordem em que foram encontrados.

| Onde | O que estava errado |
|---|---|
| `ChipRow.movingSelectedToFront` | Dentro de `buildList`, `this` é a lista **nova**. O `forEach` iterava a lista vazia em construção, e o resultado era `[c]` em vez de `[c, a, b, d, e]` |
| `MetronomeTiming.isAccent` | Faltava a condição "esta é a primeira subdivisão do tempo". Com `accentEvery = 1`, `x % 1 == 0` é sempre verdadeiro: **toda** subdivisão levava acento |
| `ChordFinder.findShapes` | Não exigia mínimo de notas. Formas de **uma** nota passavam em todas as regras e, como a ordem preferia menos dedos, ocupavam as 12 vagas da lista |
| `ChordFinder.findShapes` | O mínimo contava notas MIDI, não **tons**. "Sol2, Ré3, Ré4" são três notas e dois tons — é uma quinta *power*, não um acorde |
| `ChordFinder.findShapes` | A ordem punia a forma canônica. `xx0232` e `320033` nunca apareciam dentro das 12 vagas; o jogador via inversões de três dedos que não reconhece |
| `Fretboard.semitonesFromName` | `♭` virava `B`, a letra da nota Si. `"A♭"` era lido como um acorde de Si |
| `Fretboard.semitonesFromName` | `uppercase()` na string inteira apagava o bemol minúsculo: `"Bb"` virava `"BB"` |
| `Fretboard.midiAt` | Casa negativa não era rejeitada: `E4 + (−3)` devolvia `61` como se fosse nota válida |
| `Fretboard.bestPositionForNote` | Pegava o primeiro da lista ordenada por corda. O G3 saía na **casa 15** quando existe na casa 0 |
| `Capo.effective` | Aplicava `clamp` em silêncio. Casa 24 virava 12 e o afinador mirava a nota errada sem avisar |
| `InstrumentCatalog` (ukulele, cavaquinho) | As cordas estavam na ordem do **acorde** (`67, 60, 64, 69` = Sol-Dó-Mi-Lá), não na ordem das cordas |
| `AndroidManifest.xml` | **`VIBRATE` não estava declarada.** `Vibrator.vibrate()` lançaria `SecurityException` e a confirmação tátil nunca aconteceria, sem mensagem visível |
| `AudioCapture.tryCreate` | Não capturava `SecurityException`. Permissão revogada entre a checagem e a chamada derrubava o app |

E os quatro em que **o teste estava errado**:

| Onde | O que o teste afirmava |
|---|---|
| `MusicTheoryTest` | `labelOf(31)` = "G0". MIDI 31 é G1: o Dó0 é o MIDI 12 |
| `MusicTheoryTest`, `NoteMapperTest` | 440 Hz com A4 = 432 vira "si sustenido". São 31,6 cents acima do A4 — menos da metade de um semitono, então continua sendo A4 |
| `FretboardTest` | `"B#"` = 11. B# é o mesmo som que o Dó: semitonal **0** |
| `FretboardTest` | `"A♭"` = 10. Lá é a 9ª classe; o bemol desce um semitono, para **8** |
| `InstrumentCatalogTest` | Guitarra de 8 cordas, Fá#1, vale 61,7 Hz. Fá#1 é o MIDI 30, que vale **46,25 Hz** — 61,7 Hz é o Si1 |
| `CapoTest` | `soundedNote(0, 12)` = 0. Com capo na 12, o MIDI 0 soa como **12** |
| `MetronomeTimingTest` | `snapBpm(120.4)` = 121. Arredondar ao mais próximo dá **120** |
| `ChordFinderTest` | Com capo na 3ª, a forma de Sol soa como Lá. Sol + 3 semitons é **Si bemol**; de Sol até Lá são 2 |
| `ChordFinderTest` | Casa repetida em 2 cordas não faz batente, mas 3 sim. `3,5,3,5,3,5` tem **dois** batentes, um na 3 e outro na 5 |

### Um detalhe do ambiente que custou tempo

O AGP estourava com

```
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> java.io.IOException: A sintaxe do nome do arquivo, do nome do diretório
  ou do rótulo do volume é incorreta
    at com.android.build.gradle.internal.SdkLocator.validateSdkPath(...)
```

Sem uma única referência ao SDK no corpo do projeto. A causa: **`local.properties`
estava com barras invertidas**. O formato `.properties` trata `\` como caractere
de escape e **descarta silenciosamente** o que não reconhece — `C:\Users\...` era
lido como `C:UserssuporAppDataLocalAndroidSdk`, um caminho que não existe. Barsas
normais resolvem. O `preflight.ps1` agora pega isso (item 9), porque o
diagnóstico do AGP não aponta nem de perto para a causa.

### O que continua sem verificação

O APK **assinado** e o comportamento em aparelhos de OEM reais (Xiaomi,
Samsung, Motorola). São as coisas que só o hardware prova — a lista está em
[`store/CHECKLIST.md`](store/CHECKLIST.md) §6.

A execução em si já foi percorrida: as seis telas foram varridas num emulador
Android 14 (API 34) em pt-BR, e os doze achados de
[`PROMPT_MELHORIAS.md`](PROMPT_MELHORIAS.md) foram reproduzidos antes de
corrigir e conferidos depois de corrigir, incluindo o fluxo completo de
permissão (revogar → conceder → afinador voltar a ouvir sem reiniciar) e o
gravador (gravar → confirmação → tocar no player do sistema).

## Publicação

O fluxo completo está em **[`store/CHECKLIST.md`](store/CHECKLIST.md)**.

```bash
git tag v1.0.0
git push origin v1.0.0
```

A CI gera o APK assinado e o AAB. **Antes de subir, instale o APK assinado no
seu aparelho** e passe pela lista de verificação manual do checklist — os itens
que quebram em release são exatamente os que o R8 afeta: Room abrindo o banco,
Hilt injetando, e o receiver do widget sendo instanciado pelo sistema. Nada
disso falha em debug.

| Item | Onde |
|---|---|
| Textos da Play Store (pt, en, es) | `store/listings/` |
| Declaração de Data Safety | `store/data-safety-pt-BR.xml` |
| Política de privacidade | `PRIVACY.md` |
| Notas sobre repositório público | `store/PRIVACY-NOTES.md` |

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
  onboarding/OnboardingScreen, OnboardingViewModel
widget/         TunerWidgetProvider, ReferenceToneReceiver
ui/accessibility/ Helpers de semantica e alvos de toque
core/util/   MusicTheory, Capo, Fretboard, ChordFinder, ChordNames, ChordTones
ui/
  navigation/MainScreen (casca), Destination, AppShellViewModel
  components/  TuneMasterHeader, TuneMasterBottomBar, StudioComponents, ChipRow
  theme/     Precision Studio Dark: paleta fixa, Inter + JetBrains Mono
```

## A identidade visual

**Precision Studio Dark**: o app é escurecido de propósito — afina-se com o
instrumento no colo, sob luz fraca, e um tema claro seria ofuscante. Não há
versão clara.

A paleta é **fixa**, sem cor dinâmica (Material You). A cor dinâmica troca o
palette pelo papel de parede, e aqui isso seria um erro: o **verde** significa
"afinou" e o **ciano** significa "esta é a nota", e nenhum dos dois pode
depender do wallpaper de quem abriu o app.

| Papel | Cor | Significado |
|---|---|---|
| Fundo | `#0F131C` | a tela absorve luz |
| Superfície | `#1C2029` → `#31353F` | degraus de elevação |
| Verde | `#00E676` | no ponto, dentro de ±1,5 cent |
| Ciano | `#00E3FD` | a nota detectada, e os números |
| Âmbar | `#FFBA7D` | o desvio, o que ainda pede ajuste |
| Vermelho | `#FF3D71` | longe demais para sugerir ação |

A profundidade vem do **tom**, nunca de sombra projetada — em fundo escuro,
sombra aparece como halo sujo. O que separa um plano do outro é um fio de
1 px com 8% de branco.

Duas famílias, com uma regra: **Inter** para notas, títulos e navegação;
**JetBrains Mono** para todo número. O monoespaçado não é enfeite — durante
uma afinação o número muda a cada quadro, e num fonte proporcional o texto
sobe e desce a cada troca, e o olho perde a posição.

## As quatro abas

**Cromático · Violão · Afinações · Ferramentas**

Seis abas em 360 dp dão 60 dp por aba, e nenhum rótulo com duas sílabas
cabia inteiro. Quatro dão 90 dp cada.

O que era aba virou seção dentro de uma das quatro: os acordes e o cartão de
notas dividem **Afinações**; o gravador e o tom de referência dividem
**Ferramentas**; e os Ajustes ficaram atrás do botão de engrenagem da
biblioteca, ao lado da busca — são o lugar de onde se mexe no capo e na
referência de altura.

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

## Fases

Todas as sete concluídas. Cada uma é um commit com o que ela entrega e por quê.

| Fase | Conteúdo | Estado |
|---|---|---|
| 1 | Fundação Gradle, Compose, Hilt, tema | ✅ |
| 2 | Motor de áudio (YIN) + testes | ✅ |
| 3 | Tela de afinador, medidor, feedback | ✅ |
| 4 | Navegação, ajustes, favoritos, capo, cartão de notas | ✅ |
| 5 | Metrônomo, acordes, braço, gravador | ✅ |
| 6 | Onboarding, acessibilidade, widgets | ✅ |
| 7 | CI, release, assinatura, textos da loja | ✅ |

---

## Privacidade

- Nenhum dado sai do aparelho
- Nenhuma telemetria, nenhum analytics, nenhum crash reporting
- O app não tem permissão `INTERNET`, então **não é tecnicamente possível**
  enviar áudio a lugar nenhum
- O afinador não grava nada: o áudio é descartado a cada quadro
- A única exceção é o gravador de ideias, que só escreve quando o usuário
  aperta o botão, no armazenamento interno do app. Detalhes em
  [`PRIVACY.md`](PRIVACY.md)

---

## Licença

A definir pelo autor.