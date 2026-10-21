<#
.SYNOPSIS
    Verificações estáticas do projeto que o Gradle não faz.

.DESCRIPTION
    Roda sem JDK, sem Gradle e sem Android SDK, em segundos. Existe porque
    algumas falhas só apareceriam tarde — depois de instalar JDK 17, baixar
    dependências e esperar o Gradle configurar — e porque a CI precisa de um
    portão rápido antes de gastar minutos de build.

    Cada verificação tem uma razão de existir:

    1. Delimitadores balanceados      — erro de digitação que o compilador
                                       apontaria, mas que trava o build antes
                                       de qualquer mensagem útil
    2. Recursos R.* existem          — uma string renomeada quebra o APK em
                                       runtime, não em tempo de compilação
    3. Idiomas sincronizados          — o projeto tem 3 locales mantidos à mão;
                                       esquecer de translating uma chave faz o
                                       app cair para o português no meio da tela
    4. Import não usado               — aviso, não erro
    5. Segredos no índice do git      — a keystore de release no repositório
                                       é irreversível
    6. Manifesto sem INTERNET         - promessa de privacidade verificavel
    7. Dados corrompidos no codigo    - caracteres fora do alfabeto latino
                                       quebram a compilação do arquivo
    8. local.properties legivel      - barras invertidas viram um caminho
                                        inexistente e o AGP acusa erro de
                                        nome de arquivo em vez de SDK ausente

.PARAMETER Root
    Raiz do projeto. Padrão: a pasta deste script, dois níveis acima.

.EXAMPLE
    pwsh tools/preflight.ps1
    pwsh tools/preflight.ps1 -Verbose
#>
[CmdletBinding()]
param(
    # `$PSScriptRoot` nao existe durante o binding de parametros, e
    # `$PSCommandPath` tambem nao em todas as versoes. Por isso a raiz e
    # resolvida dentro do corpo.
    [string]$Root
)

if (-not $Root) {
    $aqui = $PSScriptRoot
    if (-not $aqui) { $aqui = $MyInvocation.MyCommand.Path }
    if (-not $aqui) { $aqui = Join-Path (Get-Location) 'tools' }
    # tools/ -> raiz do projeto (um nivel acima)
    $Root = Split-Path -Parent $aqui
}

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$falhas = 0
$avisos = 0

function Titulo([string]$t) { Write-Host "`n== $t" -ForegroundColor Cyan }
function Ok([string]$m) { Write-Host "   ok    $m" -ForegroundColor DarkGreen }
function Falha([string]$m) {
    Write-Host "   FALHA $m" -ForegroundColor Red
    $script:falhas++
}
function Aviso([string]$m) {
    Write-Host "   aviso $m" -ForegroundColor Yellow
    $script:avisos++
}

function Arquivos([string]$ext) {
    Get-ChildItem $Root -Recurse -Include $ext -File |
        Where-Object { $_.FullName -notmatch '\\build\\|\\\.git\\' }
}

# ---------------------------------------------------------------------------
Titulo '1. Delimitadores balanceados'

function Get-CodigoLimpo([string]$t) {
    # Percorre o texto uma vez, de fora para dentro, e devolve só o código:
    # sem comentários e sem o conteúdo das strings.
    #
    # ## Por que não por regex
    # A versão anterior removia comentários antes das strings, e aí um `/`
    # dentro de uma string abria um bloco de comentário falso. O caso real é o
    # mime type "audio/*": o `/` da string virava início de comentário, o
    # bloco engolia o código até o próximo `*/`, e a contagem de chaves dava
    # negativo em arquivos que **compilam**. Falso positivo no portão de
    # qualidade é pior que nenhum portão: ele treina a gente a ignorar o
    # portão.
    $sb = [System.Text.StringBuilder]::new($t.Length)
    $i = 0
    $n = $t.Length
    while ($i -lt $n) {
        $c = $t[$i]
        $prox = if ($i + 1 -lt $n) { $t[$i + 1] } else { [char]0 }

        if ($c -eq '/' -and $prox -eq '/') {
            # Comentário de linha: consome até o fim da linha.
            while ($i -lt $n -and $t[$i] -ne "`n") { $i++ }
            continue
        }
        if ($c -eq '/' -and $prox -eq '*') {
            # Comentário de bloco: consome até o `*/`.
            $i += 2
            while ($i -lt $n -and -not ($t[$i] -eq '*' -and $i + 1 -lt $n -and $t[$i + 1] -eq '/')) { $i++ }
            $i += 2
            continue
        }
        if ($c -eq '"' -and $prox -eq '"' -and $i + 2 -lt $n -and $t[$i + 2] -eq '"') {
            # String bruta: aspas triplas. Consome até a próxima sequência.
            $i += 3
            while ($i -lt $n) {
                if ($t[$i] -eq '"' -and $i + 2 -lt $n -and $t[$i + 1] -eq '"' -and $t[$i + 2] -eq '"') { $i += 3; break }
                $i++
            }
            continue
        }
        if ($c -eq '"') {
            # String normal: consome até o aspas de fechamento, pulando escapes.
            $i++
            while ($i -lt $n) {
                if ($t[$i] -eq '\') { $i += 2; continue }
                if ($t[$i] -eq '"') { $i++; break }
                if ($t[$i] -eq "`n") { break }
                $i++
            }
            continue
        }
        if ($c -eq "'") {
            # Caractere: 'a' ou '\n'. Kotlin não tem string de aspas simples,
            # então uma aspa solta no código é erro de digitação mesmo.
            $i++
            while ($i -lt $n) {
                if ($t[$i] -eq '\') { $i += 2; continue }
                if ($t[$i] -eq "'") { $i++; break }
                if ($t[$i] -eq "`n") { break }
                $i++
            }
            continue
        }
        $null = $sb.Append($c)
        $i++
    }
    return $sb.ToString()
}

$p = 0; $ch = 0; $co = 0
foreach ($f in Arquivos '*.kt') {
    $t = Get-CodigoLimpo ([System.IO.File]::ReadAllText($f.FullName))
    $a = ([regex]::Matches($t, '\(')).Count - ([regex]::Matches($t, '\)')).Count
    $b = ([regex]::Matches($t, '\{')).Count - ([regex]::Matches($t, '\}')).Count
    $c = ([regex]::Matches($t, '\[')).Count - ([regex]::Matches($t, '\]')).Count
    if ($a -ne 0 -or $b -ne 0 -or $c -ne 0) {
        Falha "$($f.Name): parenteses $a, chaves $b, colchetes $c"
    }
    $p += $a; $ch += $b; $co += $c
}
if ($falhas -eq 0) { Ok "$((Arquivos '*.kt').Count) arquivos Kotlin balanceados" }

# ---------------------------------------------------------------------------
Titulo '2. Recursos R.* existem'

$existem = @{}
Get-ChildItem "$Root\app\src\main\res" -Recurse -Include strings.xml -File | ForEach-Object {
    foreach ($m in [regex]::Matches([System.IO.File]::ReadAllText($_.FullName), 'name="([^"]+)"')) {
        $existem["string/$($m.Groups[1].Value)"] = $true
    }
}
Get-ChildItem "$Root\app\src\main\res" -Recurse -Include *.xml -File | ForEach-Object {
    $txt = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($m in [regex]::Matches($txt, '<(color|style|string|dimen|integer|bool)\s+name="([^"]+)"')) {
        $existem["$($m.Groups[1].Value)/$($m.Groups[2].Value)"] = $true
    }
    foreach ($m in [regex]::Matches($txt, '@\+id/(\w+)')) { $existem["id/$($m.Groups[1].Value)"] = $true }
    foreach ($m in [regex]::Matches($txt, '@(\w+)/(\w+)')) {
        $existem["$($m.Groups[1].Value)/$($m.Groups[2].Value)"] = $true
    }
}

$refs = 0
foreach ($f in @(Arquivos '*.kt')) {
    $txt = [System.IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($txt, 'R\.(string|layout|drawable|xml|color|id|style)\.(\w+)')) {
        $refs++
        $k = "$($m.Groups[1].Value)/$($m.Groups[2].Value)"
        if (-not $existem.ContainsKey($k)) { Falha "$($f.Name): $k nao existe" }
    }
}
if ($falhas -eq 0) { Ok "$refs referencias a recursos, todas resolvem" }

# ---------------------------------------------------------------------------
Titulo '3. Idiomas sincronizados'

$locales = @{}
Get-ChildItem "$Root\app\src\main\res" -Recurse -Include strings.xml -File | ForEach-Object {
    $locales[$_.Directory.Name] = @(
        [regex]::Matches([System.IO.File]::ReadAllText($_.FullName), 'name="([^"]+)"') |
            ForEach-Object { $_.Groups[1].Value }
    )
}
if ($locales.Count -lt 2) {
    Falha "menos de dois locales encontrados"
} else {
    $antes = $falhas
    $base = $locales['values']
    foreach ($k in $locales.Keys) {
        $faltam = @($base | Where-Object { $locales[$k] -notcontains $_ })
        $sobram = @($locales[$k] | Where-Object { $base -notcontains $_ })
        if ($faltam.Count -gt 0) { Falha "$k sem $($faltam.Count) chaves: $($faltam -join ', ')" }
        if ($sobram.Count -gt 0) { Falha "$k com $($sobram.Count) chaves a mais: $($sobram -join ', ')" }
    }
    if ($falhas -eq $antes) {
        Ok "$($locales.Count) locales com $($base.Count) chaves identicas"
    }
}

# ---------------------------------------------------------------------------
Titulo '4. Import declarado e nao usado'

foreach ($f in Arquivos '*.kt') {
    $txt = [System.IO.File]::ReadAllText($f.FullName)
    $imports = [regex]::Matches($txt, '(?m)^import\s+([\w\.]+)\s*$') | ForEach-Object { $_.Groups[1].Value }
    $body = $txt -replace '(?m)^(package|import)\s+[\w\.]+\s*$', ''
    foreach ($i in $imports) {
        $s = ($i -split '\.')[-1]
        # getValue/setValue sao operators de delegacao: nao aparecem no corpo
        if ($s -in @('*', 'getValue', 'setValue', 'OptIn', 'file')) { continue }
        if (-not [regex]::IsMatch($body, "\b$([regex]::Escape($s))\b")) {
            Aviso "$($f.Name): $i"
        }
    }
}
if ($avisos -eq 0) { Ok 'nenhum import ocioso' }

# ---------------------------------------------------------------------------
Titulo '5. Segredos no indice do git'

$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) {
    Aviso 'git indisponivel: verificacao de segredos pulada'
} else {
    $rastreados = & git -C $Root ls-files 2>$null
    if ($LASTEXITCODE -ne 0) { Aviso 'repositorio git nao inicializado' }
    else {
        $vazamento = @($rastreados | Where-Object {
            $_ -match 'keystore\.properties$' -or $_ -match '\.jks$' -or $_ -match '\.keystore$' -or $_ -match '\.m4a$'
        })
        if ($vazamento.Count -gt 0) {
            Falha "segredos versionados: $($vazamento -join ', ')"
        } else { Ok 'nenhuma keystore ou gravacao versionada' }
    }
}

# ---------------------------------------------------------------------------
Titulo '6. Manifesto'

$manifesto = Get-Content "$Root\app\src\main\AndroidManifest.xml" -Raw
$semComentarios = [regex]::Replace($manifesto, '(?s)<!--.*?-->', '')
$perms = @([regex]::Matches($semComentarios, 'uses-permission android:name="([\w\.]+)"') |
    ForEach-Object { $_.Groups[1].Value })
if ($semComentarios -match 'android\.permission\.INTERNET') {
    Falha 'o manifesto declara INTERNET: o app nao envia nada para fora'
} else { Ok 'sem permissao de INTERNET' }
$esperadas = @(
    'android.permission.RECORD_AUDIO',
    # Normal: nao aparece na tela de permissao e o usuario nao pode negar.
    # Sem ela, Vibrator.vibrate() lanca SecurityException e a confirmacao
    # tatil do afinador nunca acontece, sem nenhuma mensagem visivel.
    'android.permission.VIBRATE'
)
foreach ($e in $esperadas) {
    if ($perms -notcontains $e) { Falha "falta a permissao $e" }
}
$extras = @($perms | Where-Object { $esperadas -notcontains $_ })
if ($extras.Count -gt 0) {
    # Permissao inesperada e regressao de privacidade, nao aviso.
    Falha "permissoes fora da lista esperada: $($extras -join ', ')"
} elseif ($falhas -eq 0) {
    Ok "apenas $($perms -join ', ')"
}

# ---------------------------------------------------------------------------
Titulo '7. Texto corrompido'

$cjk = 0
foreach ($f in @(Arquivos '*.kt') + @(Arquivos '*.xml')) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '[\u4E00-\u9FFF\u3040-\u30FF\uAC00-\uD7AF]') {
            Falha "$($f.Name):$($i + 1) caracteres fora do alfabeto latino"
            $cjk++
        }
    }
}
if ($cjk -eq 0) { Ok 'nenhum caractere corrompido' }

# ---------------------------------------------------------------------------
Titulo '8. XML bem formado'

foreach ($f in Arquivos '*.xml') {
    try { [xml]$null = Get-Content $f.FullName -Raw }
    catch { Falha "$($f.Name): $($_.Exception.Message)" }
}
if ($falhas -eq 0) { Ok 'todos os XML validos' }

# ---------------------------------------------------------------------------
Titulo '9. local.properties'

# Esta verificacao veio de uma perda de tempo concreta. O formato .properties
# trata '\' como caractere de escape e DESCARTA silenciosamente o que nao
# reconhece: `sdk.dir=C:\Users\fulano` e lido como `C:Usersfulano`, que nao
# existe. O AGP nao diz "SDK nao encontrado" -- ele estoura com
# "IOException: A sintaxe do nome do arquivo... e incorreta", num frame do
# SdkLocator, e nao em nenhum do projeto. Barsas normais resolvem.
$lp = Join-Path $Root 'local.properties'
if (-not (Test-Path $lp)) {
    Aviso 'local.properties ausente: o AGP vai procurar o SDK em ANDROID_HOME'
} else {
    $linha = @(Get-Content $lp | Where-Object { $_ -match '^\s*sdk\.dir\s*=' })[0]
    if (-not $linha) {
        Aviso 'local.properties sem sdk.dir'
    } else {
        $valor = ($linha -split '=', 2)[1].Trim()
        if ($valor.Contains('\')) {
            Falha "sdk.dir com barras invertidas: o Java leria '$valor' sem as barras, um caminho inexistente"
        } elseif (-not (Test-Path $valor)) {
            Aviso "sdk.dir aponta para '$valor', que nao existe nesta maquina (esperado na CI)"
        } else {
            Ok "sdk.dir -> $valor"
        }
    }
}

# ---------------------------------------------------------------------------
Write-Host ''
if ($falhas -gt 0) {
    Write-Host "PREFLIGHT: $($falhas) falha(s), $($avisos) aviso(s)" -ForegroundColor Red
    exit 1
}
Write-Host "PREFLIGHT: tudo certo, $avisos aviso(s)" -ForegroundColor Green
exit 0