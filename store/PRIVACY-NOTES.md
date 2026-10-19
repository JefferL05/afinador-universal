# Propriedades do GitHub (repo público: qualquer um pode Rodar Actions)

O app coleta zero dados, mas o *pipeline* coleta os logs de build. Se o repositório
for público, esses logs são visíveis para qualquer pessoa.

Por isso `store/CHECKLIST.md` e os textos da PlayStore **não devem** ir para um
repositório público sem passar por esta revisão. Eles contêm informação sobre o
posicionamento do produto, que não é público por acidente.

##Recomendação

Mantenha este repositório **privado** enquanto o app não for publicado. Ou, se
precisar ser público, remova `store/` do índice:

```bash
echo "store/" >> .gitignore
git rm -r --cached store
git commit -m "Remove textos da loja do repositorio publico"
```

Os textos continuam no disco — só saem do histórico recente. Para apagar também
do histórico, é preciso reescrever o histórico, o que é outra operação.

## O que NÃO é segredo e pode ir para o git

- `keystore.properties.example` — só mostra o formato, sem valores
- `.github/workflows/` — não contém segredo algum; os valores vêm de
  `secrets.KEYSTORE_BASE64`, que nunca é lido pelo repositório
- Os arquivos `.gradle.kts` e `libs.versions.toml` — só nomes e versões

## O que É segredo e nunca deve ser commitado

| Arquivo | Por quê |
|---|---|
| `keystore.properties` | senha do keystore e caminho da chave |
| `*.jks`, `*.keystore` | a chave privada de assinatura |
| `secrets` da CI | base64 do keystore |

`tools/preflight.ps1` verifica os três na CI e falha o build se algum entrar no
índice. É a rede de segurança: o `.gitignore` evita a maioria dos casos, o
preflight pega o resto.

## Permissões dos workflows

O `ci.yml` declara `permissions: contents: read` — só leitura. O `release.yml`
declara `contents: write`, que é o mínimo para criar uma GitHub Release.

Se um dia alguém pedir uma chave de upload do Fastlane, ela também é segredo:
mais uma entrada na tabela acima.