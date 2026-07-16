param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$MaxArticles = 500,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$assets = Join-Path $root "mobile-offline\app\src\main\assets\articles.json"
$articles = @()
$page = 0
while ($articles.Count -lt $MaxArticles) {
    $size = [Math]::Min(100, $MaxArticles - $articles.Count)
    $response = Invoke-RestMethod "$BaseUrl/api/v1/articles?page=$page&size=$size"
    foreach ($summary in $response.data.content) {
        $detail = (Invoke-RestMethod "$BaseUrl/api/v1/articles/$($summary.id)").data
        $aiResponse = Invoke-RestMethod "$BaseUrl/api/v1/articles/$($summary.id)/ai"
        $articles += [ordered]@{
            id = $detail.id
            title = $detail.title
            sourceName = $detail.sourceName
            author = $detail.author
            summary = $detail.summary
            contentText = $detail.contentText
            publishTime = $detail.publishTime
            ai = $aiResponse.data
        }
    }
    if ($response.data.content.Count -lt $size) { break }
    $page++
}
$json = ConvertTo-Json -InputObject @($articles) -Depth 8
[IO.File]::WriteAllText($assets, $json, [Text.UTF8Encoding]::new($false))
Write-Host "Exported $($articles.Count) articles to $assets"

if (-not $SkipBuild) {
    Push-Location (Join-Path $root "mobile-offline")
    try {
        $gradle = Get-Command gradle -ErrorAction SilentlyContinue
        if (-not $gradle) {
            throw "Gradle 未安装。请使用 Android Studio 打开 mobile-offline，或安装 Gradle 7.6 后重新执行。"
        }
        gradle assembleDebug
        Write-Host "APK: mobile-offline\app\build\outputs\apk\debug\app-debug.apk"
    } finally {
        Pop-Location
    }
}
