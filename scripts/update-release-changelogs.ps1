param(
    [string] $Version = "",
    [string] $ChangelogPath = "",
    [string] $ModrinthVersionId = "",
    [string] $CurseForgeProjectId = "",
    [string] $CurseForgeFileId = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$curl = if ($IsWindows) { "curl.exe" } else { "curl" }

if ([string]::IsNullOrWhiteSpace($Version)) {
    $gradleProperties = Get-Content -LiteralPath (Join-Path $root "gradle.properties")
    $Version = ($gradleProperties | Where-Object { $_ -like "mod_version=*" } | Select-Object -First 1) -replace "^mod_version=", ""
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    throw "Version was not provided and could not be read from gradle.properties."
}

if ([string]::IsNullOrWhiteSpace($CurseForgeProjectId)) {
    $CurseForgeProjectId = $env:CURSEFORGE_PROJECT_ID
}

function Get-Changelog {
    param(
        [string] $Version,
        [string] $Path
    )

    if (![string]::IsNullOrWhiteSpace($Path)) {
        $resolvedPath = Join-Path $root $Path
        if (Test-Path -LiteralPath $resolvedPath) {
            $content = ((Get-Content -Raw -LiteralPath $resolvedPath) ?? "").Trim()
            if (![string]::IsNullOrWhiteSpace($content)) {
                return $content
            }
        }
    }

    $lines = Get-Content -LiteralPath (Join-Path $root "CHANGELOG.md")
    $capturing = $false
    $captured = New-Object System.Collections.Generic.List[string]
    foreach ($line in $lines) {
        if ($line -eq "## $Version" -or $line -like "## $Version - *") {
            $capturing = $true
            continue
        }

        if ($capturing -and $line -like "## *") {
            break
        }

        if ($capturing) {
            $captured.Add($line)
        }
    }

    $changelog = ($captured -join "`n").Trim()
    if ([string]::IsNullOrWhiteSpace($changelog)) {
        throw "Could not find changelog notes for '$Version'."
    }

    return $changelog
}

function Write-JsonFile {
    param(
        [string] $Path,
        [object] $Value
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
    $Value | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Path -Encoding UTF8
}

$changelog = Get-Changelog -Version $Version -Path $ChangelogPath
$updateDir = Join-Path $root "build/changelog-update"

if (![string]::IsNullOrWhiteSpace($ModrinthVersionId)) {
    $token = $env:MODRINTH_TOKEN
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "MODRINTH_TOKEN is not set."
    }

    $headers = @{
        "Authorization" = $token
        "User-Agent" = "TnTBass/signport Modrinth changelog update"
        "Accept" = "application/json"
    }
    $payloadPath = Join-Path $updateDir "modrinth-changelog.json"
    Write-JsonFile -Path $payloadPath -Value @{ changelog = $changelog }

    Invoke-RestMethod `
        -Uri "https://api.modrinth.com/v2/version/$ModrinthVersionId" `
        -Method PATCH `
        -Headers $headers `
        -ContentType "application/json" `
        -InFile $payloadPath | Out-Null

    Write-Host "[SignPort] Updated Modrinth changelog for version $ModrinthVersionId."
}

if (![string]::IsNullOrWhiteSpace($CurseForgeFileId)) {
    $token = $env:CURSEFORGE_TOKEN
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "CURSEFORGE_TOKEN is not set."
    }
    if ([string]::IsNullOrWhiteSpace($CurseForgeProjectId)) {
        throw "CURSEFORGE_PROJECT_ID is not set and -CurseForgeProjectId was not provided."
    }

    $metadataPath = Join-Path $updateDir "curseforge-changelog.json"
    Write-JsonFile -Path $metadataPath -Value @{
        fileID = [int]$CurseForgeFileId
        changelog = $changelog
        changelogType = "markdown"
    }

    $response = & $curl -sS `
        -X POST "https://minecraft.curseforge.com/api/projects/$CurseForgeProjectId/update-file" `
        -H "X-Api-Token: $token" `
        -H "Accept: application/json" `
        -F "metadata=<$metadataPath;type=application/json"

    if ($LASTEXITCODE -ne 0) {
        throw "curl failed while updating CurseForge file."
    }

    $curseForgeFile = $response | ConvertFrom-Json
    if ($curseForgeFile.error -or $curseForgeFile.errors) {
        throw "CurseForge file update failed: $response"
    }

    Write-Host "[SignPort] Updated CurseForge changelog for file $CurseForgeFileId."
}

if ([string]::IsNullOrWhiteSpace($ModrinthVersionId) -and [string]::IsNullOrWhiteSpace($CurseForgeFileId)) {
    throw "No platform version/file IDs were provided."
}
