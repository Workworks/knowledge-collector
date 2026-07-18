param(
    [string]$Model = "deepseek-r1:14b",
    [switch]$Gpu,
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $root
try {
    docker version | Out-Null
    docker compose version | Out-Null
    $env:OLLAMA_MODEL = $Model
    $files = @("-f", "compose.yaml")
    if ($Gpu) {
        $files += @("-f", "compose.gpu.yaml")
    }
    docker compose @files build
    if (-not $BuildOnly) {
        docker compose @files up -d
        Write-Host "Knowledge Collector: http://127.0.0.1:8080"
        Write-Host "Ollama model: $Model"
    }
} finally {
    Pop-Location
}
