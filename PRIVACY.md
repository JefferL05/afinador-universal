# Política de privacidade

**Resumo:** o app não coleta nada, não compartilha nada e não tem como fazer
nenhuma das duas coisas.

## O que o app coleta

**Nada.**

## O que o app compartilha

**Nada.**

## Por que isso é verificável, e não uma promessa

Qualquer política de privacidade pode dizer que não coleta dados. Esta tem uma
razão a mais:

- **O app não declara a permissão `INTERNET`.** Está no manifesto, e o build
  falha se alguém declarar sem querer. Sem permissão de internet, não existe
  caminho de código capaz de enviar dados para um servidor.
- **Não há biblioteca de analytics, crash reporting ou anúncios** entre as
  dependências.

O `AndroidManifest.xml` deste repositório tem uma única permissão:
`RECORD_AUDIO`.

## O microfone

O app usa o microfone para **detectar a frequência da nota que você tocou**.

- O áudio é analisado em tempo real e convertido em um número de frequência.
- **Nada é gravado em disco.** O áudio é descartado a cada quadro de análise.
- **Nada é transmitido.** Não há para onde transmitir.
- O acesso só ocorre com a tela de afinação aberta e visível.

O gravador de ideias é a única parte do app que escreve áudio, e só quando você
aperta o botão de gravar. As gravações ficam no armazenamento interno do app
(`filesDir`), não aparecem na galeria e não são visíveis para outros apps. São
apagadas quando você apaga o app.

## Armazenamento local

O app guarda no seu aparelho:

| O quê | Onde | Por quê |
|---|---|---|
| Instrumento, afinação e A4 escolhidos | DataStore | Para lembrar suas escolhas |
| Afinações favoritas | Banco SQLite interno | Você as salvou |
| Gravações de ideias | Arquivos internos | Você as gravou |

Nada disso sai do aparelho. Apagar o app apaga tudo.

## Dados de diagnóstico

O app **não** usa Firebase Crashlytics, Sentry, nem qualquer outro serviço de
coleta de falhas. Não há envio de relatórios de erro.

## Permissões

| Permissão | Para quê | É obrigatória |
|---|---|---|
| `RECORD_AUDIO` | Ouvir a corda para detectar a nota | Sim, para o afinador funcionar |

Nenhuma outra permissão é solicitada. O app não acessa contatos, localização,
fotos, armazenamento externo ou câmera.

## Anúncios e compras

**Não existem.** Não há anúncios, e o app não usa Play Billing. Não há conta,
não há login e não há rastreamento.

## Conformidade

- **GDPR / LGPD:** o app não trata dados pessoais. Não há base legal a declarar
  porque não há dado.
- **Google Play Data Safety:** "Nenhum dado coletado, nenhum dado compartilhado."

## Crianças

O app não coleta dados de ninguém, incluindo crianças. Por isso não coletamos
idade nem pedimos consentimento parental — não há dado pessoal de ninguém.

## Mudanças nesta política

Se o app algum dia passar a enviar dados, esta política **mudará antes** de o
fazer, e a mudança será visível aqui. O primeiro sinal disso no código seria a
permissão `INTERNET` aparecendo no manifesto — e a CI falha quando isso
acontece sem revisão explícita.

## Contato

Abra uma issue no repositório. É o canal mais rápido e o mais público.