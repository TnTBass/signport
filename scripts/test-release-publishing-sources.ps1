Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot

function Get-Text {
    param([string] $RelativePath)

    $path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        throw "$RelativePath is missing."
    }
    return Get-Content -Raw -LiteralPath $path
}

function Assert-Contains {
    param(
        [string] $Text,
        [string] $Needle,
        [string] $Message
    )

    if (-not $Text.Contains($Needle)) {
        throw $Message
    }
}

function Assert-NotContains {
    param(
        [string] $Text,
        [string] $Needle,
        [string] $Message
    )

    if ($Text.Contains($Needle)) {
        throw $Message
    }
}

function Test-CurseForgeUploadReportsVerifiedFileId {
    $script = Get-Text 'scripts/upload-curseforge.ps1'

    Assert-Contains $script 'errorCode' 'CurseForge upload must detect CurseForge API errorCode payloads.'
    Assert-Contains $script 'errorMessage' 'CurseForge upload must detect CurseForge API errorMessage payloads.'
    Assert-Contains $script '$curseForgeFile.id' 'CurseForge upload must require the returned CurseForge file ID before reporting success.'
    Assert-Contains $script 'curseforge_file_id' 'CurseForge upload must write the returned file ID to GITHUB_OUTPUT.'
    Assert-Contains $script 'CurseForgeFileId' 'CurseForge upload logs must print the returned file ID for verification.'
}

function Test-ReleaseWorkflowUsesRealCurseForgeSlug {
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow '-Slug "signport"' 'Release workflow must report the real CurseForge slug.'
    Assert-Contains $workflow 'id: curseforge' 'Release workflow must capture the CurseForge upload step output.'
    Assert-Contains $workflow 'CurseForge file ID: ${{ steps.curseforge.outputs.curseforge_file_id }}' 'Release workflow must print the CurseForge file ID.'
    Assert-NotContains $workflow '-Slug "modern-signport"' 'Release workflow must not report the old constructed CurseForge slug.'
}

function Test-CurseForgeOnlyPublishWorkflow {
    $workflow = Get-Text '.github/workflows/publish-curseforge.yml'

    Assert-Contains $workflow 'workflow_dispatch' 'CurseForge-only workflow must be manually runnable.'
    Assert-Contains $workflow '-Slug "signport"' 'CurseForge-only workflow must report the real CurseForge slug.'
    Assert-Contains $workflow 'id: curseforge' 'CurseForge-only workflow must capture the CurseForge upload step output.'
    Assert-Contains $workflow 'CurseForge file ID: ${{ steps.curseforge.outputs.curseforge_file_id }}' 'CurseForge-only workflow must print the CurseForge file ID.'
    Assert-Contains $workflow './scripts/upload-curseforge.ps1' 'CurseForge-only workflow must use the shared upload script.'
    Assert-Contains $workflow '-JarPath "build/libs/signport-$version.jar"' 'CurseForge-only workflow must upload the release jar.'
    Assert-NotContains $workflow 'upload-modrinth.ps1' 'CurseForge-only workflow must not republish Modrinth.'
    Assert-NotContains $workflow 'gh release create' 'CurseForge-only workflow must not create another GitHub Release.'
    Assert-NotContains $workflow '-Slug "modern-signport"' 'CurseForge-only workflow must not report the old constructed CurseForge slug.'
}

function Test-PowerShellPublishingScriptsParse {
    [scriptblock]::Create((Get-Text 'scripts/upload-curseforge.ps1')) | Out-Null
}

Test-CurseForgeUploadReportsVerifiedFileId
Test-ReleaseWorkflowUsesRealCurseForgeSlug
Test-CurseForgeOnlyPublishWorkflow
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
