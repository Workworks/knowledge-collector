[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$files = @(
    Get-Item (Join-Path $root 'README.md'), (Join-Path $root 'CHANGELOG.md'),
        (Join-Path $root 'CONTRIBUTING.md'), (Join-Path $root 'SECURITY.md')
) + @(Get-ChildItem (Join-Path $root 'docs') -Recurse -Filter '*.md' -File)
$pattern = '!?(?:\[[^\]]*\])\((?<target>[^)]+)\)'
$errors = [System.Collections.Generic.List[string]]::new()

foreach ($file in $files) {
    $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    foreach ($match in [regex]::Matches($content, $pattern)) {
        $target = $match.Groups['target'].Value.Trim()
        if ($target.StartsWith('<') -and $target.EndsWith('>')) {
            $target = $target.Substring(1, $target.Length - 2)
        }
        if ($target -match '^(https?://|mailto:|#)') { continue }
        $pathPart = [Uri]::UnescapeDataString(($target -split '[#?]', 2)[0])
        if ([string]::IsNullOrWhiteSpace($pathPart)) { continue }
        $candidate = if ($pathPart.StartsWith('/')) {
            Join-Path $root $pathPart.TrimStart('/')
        } else {
            Join-Path $file.DirectoryName $pathPart
        }
        if (-not (Test-Path -LiteralPath $candidate)) {
            $relative = [IO.Path]::GetRelativePath($root, $file.FullName)
            $errors.Add("$relative -> $target")
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Error ("Broken documentation links:`n" + ($errors -join "`n"))
}

Write-Host "Documentation links OK: $($files.Count) Markdown files."
