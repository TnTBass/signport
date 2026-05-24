param(
    [string] $ProjectId = "",
    [string] $Slug = "signport",
    [string] $Version = "",
    [string] $JarPath = "",
    [string] $ChangelogPath = ""
)

$ErrorActionPreference = "Stop"

$token = $env:CURSEFORGE_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "CURSEFORGE_TOKEN is not set."
}

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    $ProjectId = $env:CURSEFORGE_PROJECT_ID
}

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    throw "CURSEFORGE_PROJECT_ID is not set and -ProjectId was not provided."
}

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Version)) {
    $gradleProperties = Get-Content -LiteralPath (Join-Path $root "gradle.properties")
    $Version = ($gradleProperties | Where-Object { $_ -like "mod_version=*" } | Select-Object -First 1) -replace "^mod_version=", ""
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    throw "Version was not provided and could not be read from gradle.properties."
}

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = "build/libs/signport-$Version.jar"
}

$minecraftVersion = $Version -replace "^.*\+mc", ""
$displayVersion = $Version -replace "\+mc.*$", ""
$uploadDir = Join-Path $root "build/curseforge-upload"
$metadataPath = Join-Path $uploadDir "curseforge-metadata.json"
New-Item -ItemType Directory -Force -Path $uploadDir | Out-Null
$curl = if ($IsWindows) { "curl.exe" } else { "curl" }

function Get-Changelog {
    param(
        [string] $Version,
        [string] $Path
    )

    if (![string]::IsNullOrWhiteSpace($Path)) {
        $resolvedPath = Join-Path $root $Path
        if (Test-Path -LiteralPath $resolvedPath) {
            $content = Get-Content -Raw -LiteralPath $resolvedPath
            if ($null -eq $content) {
                $content = ""
            }
            $content = $content.Trim()
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
        return "SignPort $Version"
    }

    return $changelog
}

function Get-CurseForgeGameVersionId {
    param(
        [object[]] $GameVersions,
        [string] $Name
    )

    # Try exact match first (case-insensitive), then fall back to partial match.
    # The CurseForge API occasionally uses names like "Fabric Loader" instead of
    # "Fabric", so a partial match prevents silent failures on name drift.
    $match = $GameVersions | Where-Object { $_.name -ieq $Name } | Select-Object -First 1
    if ($null -eq $match) {
        $match = $GameVersions | Where-Object { $_.name -ilike "*$Name*" } | Select-Object -First 1
    }
    if ($null -eq $match) {
        $available = ($GameVersions | Where-Object { $_.name -ilike "*$Name*" } | ForEach-Object { $_.name }) -join ", "
        if ([string]::IsNullOrWhiteSpace($available)) {
            $available = ($GameVersions | Select-Object -First 20 | ForEach-Object { $_.name }) -join ", "
        }
        throw "CurseForge game version '$Name' was not found. Available (sample): $available"
    }

    return $match.id
}

$headers = @{
    "X-Api-Token" = $token
    "Accept" = "application/json"
}
$gameVersions = Invoke-RestMethod `
    -Uri "https://minecraft.curseforge.com/api/game/versions" `
    -Headers $headers

$metadata = @{
    changelog = Get-Changelog -Version $Version -Path $ChangelogPath
    changelogType = "markdown"
    displayName = "SignPort $displayVersion for Minecraft $minecraftVersion"
    gameVersions = @(
        Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name $minecraftVersion
        Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name "Fabric"
    )
    releaseType = "release"
    relations = @{
        projects = @(
            @{
                slug = "fabric-api"
                type = "requiredDependency"
            }
        )
    }
} | ConvertTo-Json -Depth 10
$metadata | Set-Content -LiteralPath $metadataPath -Encoding UTF8

$jar = Get-Item -LiteralPath (Join-Path $root $JarPath)
$uploadResponse = & $curl -sS `
    -X POST "https://minecraft.curseforge.com/api/projects/$ProjectId/upload-file" `
    -H "X-Api-Token: $token" `
    -H "Accept: application/json" `
    -F "metadata=<$metadataPath;type=application/json" `
    -F "file=@$($jar.FullName)"

if ($LASTEXITCODE -ne 0) {
    throw "curl failed while uploading CurseForge file."
}

$curseForgeFile = $uploadResponse | ConvertFrom-Json
if (
    ($curseForgeFile.PSObject.Properties.Name -contains "error") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errors") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errorCode") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errorMessage")
) {
    throw "CurseForge file upload failed: $uploadResponse"
}

if (
    ($curseForgeFile.PSObject.Properties.Name -notcontains "id") -or
    [string]::IsNullOrWhiteSpace([string] $curseForgeFile.id)
) {
    throw "CurseForge file upload did not return a file ID: $uploadResponse"
}

$curseForgeFileId = [string] $curseForgeFile.id
if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable("GITHUB_OUTPUT"))) {
    Add-Content -LiteralPath $env:GITHUB_OUTPUT -Value "curseforge_file_id=$curseForgeFileId"
}

[pscustomobject]@{
    ProjectId = $ProjectId
    ProjectSlug = $Slug
    ProjectUrl = "https://www.curseforge.com/minecraft/mc-mods/$Slug"
    CurseForgeFileId = $curseForgeFileId
    VersionNumber = $Version
} | ConvertTo-Json -Depth 4
