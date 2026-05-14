param(
    [string] $Slug = "signport",
    [string] $Version = "",
    [string] $JarPath = "",
    [string] $SourcesJarPath = "",
    [string] $ChangelogPath = ""
)

$ErrorActionPreference = "Stop"

$token = $env:MODRINTH_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "MODRINTH_TOKEN is not set."
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

if ([string]::IsNullOrWhiteSpace($SourcesJarPath)) {
    $SourcesJarPath = "build/libs/signport-$Version-sources.jar"
}

$minecraftVersion = $Version -replace "^.*\+mc", ""
$displayVersion = $Version -replace "\+mc.*$", ""
$uploadDir = Join-Path $root "build/modrinth-upload"
$versionDataPath = Join-Path $uploadDir "modrinth-version-data.json"
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
        return "SignPort $Version"
    }

    return $changelog
}

# Resolve the slug to a base62 project ID — the version creation endpoint
# does not accept slugs containing characters outside base62 (e.g. '-').
$headers = @{
    "Authorization" = $token
    "User-Agent" = "TnTBass/signport Modrinth upload"
    "Accept" = "application/json"
}
$project = Invoke-RestMethod `
    -Uri "https://api.modrinth.com/v2/project/$Slug" `
    -Headers $headers
if ($null -eq $project -or [string]::IsNullOrWhiteSpace($project.id)) {
    throw "Could not resolve Modrinth project '$Slug' to an ID. Check that the project exists and the slug is correct."
}
$projectId = $project.id

# Sync the project description from docs/modrinth-listing.md.
# This runs on every release so the live page always reflects the repo source.
$listingDoc = Join-Path $root "docs/modrinth-listing.md"
if (Test-Path -LiteralPath $listingDoc) {
    $listingContent = (Get-Content -Raw -LiteralPath $listingDoc) -replace "`r`n", "`n"
    $descMatch = [regex]::Match($listingContent, '## Project Description\n+````markdown\n([\s\S]*?)````')
    if ($descMatch.Success) {
        $descBody = $descMatch.Groups[1].Value.TrimEnd()
        $descPayload = [System.Text.Encoding]::UTF8.GetBytes((@{ body = $descBody } | ConvertTo-Json -Depth 2))
        try {
            Invoke-RestMethod `
                -Uri "https://api.modrinth.com/v2/project/$Slug" `
                -Method PATCH `
                -Headers $headers `
                -ContentType "application/json" `
                -Body $descPayload | Out-Null
            Write-Host "[SignPort] Synced Modrinth project description from docs/modrinth-listing.md."
        } catch {
            Write-Warning "[SignPort] Failed to sync Modrinth project description: $_"
        }
    } else {
        Write-Warning "[SignPort] Could not find '## Project Description' block in docs/modrinth-listing.md — description not synced."
    }
} else {
    Write-Warning "[SignPort] docs/modrinth-listing.md not found — Modrinth project description was not synced."
}

$versionData = @{
    name = "SignPort $displayVersion for Minecraft $minecraftVersion"
    version_number = $Version
    changelog = Get-Changelog -Version $Version -Path $ChangelogPath
    dependencies = @(
        @{
            project_id = "P7dR8mSH"
            version_id = $null
            file_name = $null
            dependency_type = "required"
        }
    )
    game_versions = @($minecraftVersion)
    version_type = "release"
    loaders = @("fabric")
    featured = $true
    status = "listed"
    project_id = $projectId
    file_parts = @("file", "sources")
    primary_file = "file"
} | ConvertTo-Json -Depth 10
$versionData | Set-Content -LiteralPath $versionDataPath -Encoding UTF8

$jar = Get-Item -LiteralPath (Join-Path $root $JarPath)
$sources = Get-Item -LiteralPath (Join-Path $root $SourcesJarPath)

$versionResponse = & $curl -sS `
    -X POST "https://api.modrinth.com/v2/version" `
    -H "Authorization: $token" `
    -H "User-Agent: TnTBass/signport Modrinth upload" `
    -H "Accept: application/json" `
    -F "data=<$versionDataPath;type=application/json" `
    -F "file=@$($jar.FullName)" `
    -F "sources=@$($sources.FullName)"

if ($LASTEXITCODE -ne 0) {
    throw "curl failed while creating Modrinth version."
}

$modrinthVersion = $versionResponse | ConvertFrom-Json
if ($modrinthVersion.error) {
    throw "Modrinth version creation failed: $($modrinthVersion.error): $($modrinthVersion.description)"
}

[pscustomobject]@{
    ProjectSlug = $Slug
    ProjectUrl = "https://modrinth.com/mod/$Slug"
    VersionId = $modrinthVersion.id
    VersionNumber = $modrinthVersion.version_number
    VersionStatus = $modrinthVersion.status
    VersionUrl = "https://modrinth.com/mod/$Slug/version/$($modrinthVersion.id)"
} | ConvertTo-Json -Depth 4
