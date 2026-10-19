# Publicação na Play Store

> Checklist de publicação. Cada item explica **por que**, não só o que fazer.

## Ordem sugerida

1. `store/listings/` — textos
2. Ícone e capturas de tela (§ Capturas)
3. Data Safety (`store/data-safety-pt-BR.xml`)
4. Classificação de conteúdo
5. Primeiro build de release pela CI
6. Testar o APK assinado no seu aparelho
7. Lançamento interno → produção

---

## 1. Textos

Prontos em `store/listings/`:

| Arquivo | Uso |
|---|---|
| `pt-BR.txt` | Padrão do projeto |
| `en-US.txt` | Serve para pt-BR e en-US |
| `es-419.txt` | Serve para es-419 |

O limite da Play é **4 000 caracteres** na descrição longa. Todos passam com
folga — descrição curta, e longa bem abaixo do teto.

## 2. Ícone e capturas

Faltam **imagens**, que não dá para gerar em código. O que a Play exige:

| Item | Especificação |
|---|---|
| Ícone do app | 512 × 512 PNG, sem transparência, sem bordas arredondadas |
| Captura de destaque | 1024 × 500, usada na página do app |
| Capturas de celular | Mínimo 2, recomendado 4+, proporção 16:9 ou 9:16 |
| Capturas de tablet | 7" e 10", opcionais mas Tyson em muito mais idiomas |

As capturas devem mostrar o app **funcionando**, com o instrumento visível.
Uma captura de tela de banco de dados ou de tela de login não vende um afinador.

Sugestão de sequência, na ordem em que o usuário descobre o valor:

1. Medidor com a nota no verde — o que o app faz
2. Cartão de notas com as frequências — afinar de ouvido
3. Metrônomo com o pulso no tempo
4. Ajustes com o A4 e o capo — prova de que leva a sério

## 3. Data Safety

`store/data-safety-pt-BR.xml` mapeia as respostas do formulário.

**Declaração: nenhum dado coletado, nenhum compartilhado.** Isso não é
promessa — o app não declara `INTERNET` no manifesto, então não há caminho de
código capaz de enviar dados. `tools/preflight.ps1` verifica isso na CI e falha
o build se alguém declarar a permissão sem querer.

## 4. Classificação de conteúdo

Preencha o questionário do IARC. Respostas prováveis:

- Violência: não
- Conteúdo sexual: não
- Linguagem ofensiva: não
- Substâncias: não
- Gambling: não

Um afinador não se encaixa em nenhuma categoria. Se alguma pergunta mencionar
"compras dentro do app", a resposta depende do modelo escolhido — ver
`gradle/libs.versions.toml`: este projeto **não** tem Play Billing.

## 5. Build de release

```bash
git tag v1.0.0
git push origin v1.0.0
```

A GitHub Actions gera o APK assinado e o AAB. **A keystore precisa estar nos
segredos do repositório** (Settings → Secrets):

| Segredo | Conteúdo |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w 0 afinador.jks` |
| `KEYSTORE_PASSWORD` | senha do keystore |
| `KEY_ALIAS` | alias da chave |
| `KEY_PASSWORD` | senha da chave |

A CI falha em menos de um minuto se algum deles faltar, antes de gastar tempo de
build.

## 6. Testar o APK assinado

Antes de subir, instale o artefato da CI no seu próprio aparelho:

```bash
adb install -r app-release.apk
```

Verifique **na mão** o que só aparece em release:

- [ ] O app abre e pede o microfone
- [ ] O afinador mostra a nota
- [ ] O bip toca ao acertar
- [ ] Os favoritos aparecem nos ajustes
- [ ] O metrônomo toca
- [ ] O widget aparece na galeria e toca a nota de referência
- [ ] O cartão de notas mostra as frequências

Estes são exatamente os pontos que o R8 quebra quando as regras do ProGuard
estão erradas: Room abrindo o banco, Hilt injetando, e o receiver do widget
sendo instanciado pelo sistema. Nada disso falha em debug.

## 7. Lançamento

Use **lançamento interno** primeiro. Ele dá ~20 testadores com o caminho de
atualização e aparece no Play Console em minutos, o que permite achar um crash
de OEM antes de chegar a todo mundo.

Depois de 7 dias sem crash no painel, promover para produção.

---

## O que fazer se precisar de novo build

A mesma tag **não pode** ser republicada. Para corrigir algo:

```bash
git tag v1.0.1
git push origin v1.0.1
```

E subir o novo `.aab` como um lançamento novo, com `versionCode` maior.

## Keystore

**A keystore é o documento mais importante do projeto.** Sem ela não há como
publicar atualização — a Play exige que todas as versões do app sejam assinadas
pela mesma chave.

Faça backup em pelo menos dois lugares, e não só na sua máquina.