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
    Assert-Contains $workflow 'id: curseforge_fabric' 'Release workflow must capture the Fabric CurseForge upload step output.'
    Assert-Contains $workflow 'id: curseforge_neoforge' 'Release workflow must capture the NeoForge CurseForge upload step output.'
    Assert-Contains $workflow 'NeoForge CurseForge file ID: ${{ steps.curseforge_neoforge.outputs.curseforge_file_id }}' 'Release workflow must print the NeoForge CurseForge file ID.'
    Assert-NotContains $workflow '-Slug "modern-signport"' 'Release workflow must not report the old constructed CurseForge slug.'
}

function Test-ReleaseWorkflowUsesLoaderQualifiedMultiloaderArtifacts {
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow 'build/libs/signport-fabric-${version}.jar' 'Release workflow must verify and attach the loader-qualified Fabric jar.'
    Assert-Contains $workflow 'build/libs/signport-fabric-${version}-sources.jar' 'Release workflow must verify and attach the loader-qualified Fabric sources jar to GitHub releases.'
    Assert-Contains $workflow 'neoforge/build/libs/signport-neoforge-${version}.jar' 'Release workflow must verify and attach the loader-qualified NeoForge jar.'
    Assert-Contains $workflow 'neoforge/build/libs/signport-neoforge-${version}-sources.jar' 'Release workflow must verify and attach the loader-qualified NeoForge sources jar to GitHub releases.'
    Assert-Contains $workflow '-Loader "fabric"' 'Release workflow must publish Fabric marketplace metadata separately.'
    Assert-Contains $workflow '-Loader "neoforge"' 'Release workflow must publish NeoForge marketplace metadata separately.'
    Assert-Contains $workflow '-JarPath "build/libs/signport-fabric-$version.jar"' 'Release workflow must upload the loader-qualified Fabric jar.'
    Assert-Contains $workflow '-JarPath "neoforge/build/libs/signport-neoforge-$version.jar"' 'Release workflow must upload the loader-qualified NeoForge jar.'
    Assert-NotContains $workflow '-SourcesJarPath "build/libs/signport-fabric-$version-sources.jar"' 'Marketplace uploads must not upload Fabric sources jars.'
    Assert-NotContains $workflow '-SourcesJarPath "neoforge/build/libs/signport-neoforge-$version-sources.jar"' 'Marketplace uploads must not upload NeoForge sources jars.'
    Assert-NotContains $workflow 'build/libs/signport-${version}.jar' 'Release workflow must not use unqualified Fabric jar names.'
    Assert-NotContains $workflow 'build/libs/signport-${version}-sources.jar' 'Release workflow must not use unqualified Fabric sources jar names.'
}

function Test-CurseForgeOnlyPublishWorkflow {
    $workflow = Get-Text '.github/workflows/publish-curseforge.yml'

    Assert-Contains $workflow 'workflow_dispatch' 'CurseForge-only workflow must be manually runnable.'
    Assert-Contains $workflow '-Slug "signport"' 'CurseForge-only workflow must report the real CurseForge slug.'
    Assert-Contains $workflow 'id: curseforge' 'CurseForge-only workflow must capture the CurseForge upload step output.'
    Assert-Contains $workflow 'CurseForge file ID: ${{ steps.curseforge.outputs.curseforge_file_id }}' 'CurseForge-only workflow must print the CurseForge file ID.'
    Assert-Contains $workflow './scripts/upload-curseforge.ps1' 'CurseForge-only workflow must use the shared upload script.'
    Assert-Contains $workflow 'inputs.loader' 'CurseForge-only workflow must choose which loader artifact to retry.'
    Assert-Contains $workflow '-Loader $loader' 'CurseForge-only workflow must pass the selected loader to the shared upload script.'
    Assert-Contains $workflow 'build/libs/signport-fabric-$version.jar' 'CurseForge-only workflow must support the loader-qualified Fabric jar.'
    Assert-Contains $workflow 'neoforge/build/libs/signport-neoforge-$version.jar' 'CurseForge-only workflow must support the loader-qualified NeoForge jar.'
    Assert-NotContains $workflow '-JarPath "build/libs/signport-$version.jar"' 'CurseForge-only workflow must not use the unqualified Fabric jar name.'
    Assert-NotContains $workflow 'upload-modrinth.ps1' 'CurseForge-only workflow must not republish Modrinth.'
    Assert-NotContains $workflow 'gh release create' 'CurseForge-only workflow must not create another GitHub Release.'
    Assert-NotContains $workflow '-Slug "modern-signport"' 'CurseForge-only workflow must not report the old constructed CurseForge slug.'
}

function Test-UploadScriptDefaultsUseLoaderQualifiedFabricArtifacts {
    $modrinth = Get-Text 'scripts/upload-modrinth.ps1'
    $curseforge = Get-Text 'scripts/upload-curseforge.ps1'

    Assert-Contains $modrinth 'build/libs/signport-fabric-$Version.jar' 'Modrinth upload default jar path must use the loader-qualified Fabric jar.'
    Assert-Contains $modrinth 'neoforge/build/libs/signport-neoforge-$Version.jar' 'Modrinth upload default jar path must support the loader-qualified NeoForge jar.'
    Assert-Contains $modrinth '$Loader' 'Modrinth upload script must accept a loader argument.'
    Assert-Contains $modrinth 'file_parts = @("file")' 'Modrinth upload must default to publishing only the mod jar.'
    Assert-NotContains $modrinth 'build/libs/signport-fabric-$Version-sources.jar' 'Modrinth upload default must not upload Fabric sources jars.'
    Assert-NotContains $modrinth 'neoforge/build/libs/signport-neoforge-$Version-sources.jar' 'Modrinth upload default must not upload NeoForge sources jars.'
    Assert-Contains $curseforge 'build/libs/signport-fabric-$Version.jar' 'CurseForge upload default jar path must use the loader-qualified Fabric jar.'
    Assert-Contains $curseforge 'neoforge/build/libs/signport-neoforge-$Version.jar' 'CurseForge upload default jar path must support the loader-qualified NeoForge jar.'
    Assert-Contains $curseforge '$Loader' 'CurseForge upload script must accept a loader argument.'
    Assert-NotContains $modrinth 'build/libs/signport-$Version.jar' 'Modrinth upload default must not use unqualified jar names.'
    Assert-NotContains $modrinth 'build/libs/signport-$Version-sources.jar' 'Modrinth upload default must not use unqualified sources jar names.'
    Assert-NotContains $curseforge 'build/libs/signport-$Version.jar' 'CurseForge upload default must not use unqualified jar names.'
}

function Test-MavenPublicationKeepsStableArtifactId {
    $build = Get-Text 'build.gradle'

    Assert-Contains $build 'artifactId = project.archives_base_name' 'Maven publication artifactId must stay stable even when archive filenames are loader-qualified.'
    Assert-NotContains $build 'artifactId = project.base.archivesName.get()' 'Maven publication artifactId must not inherit the loader-qualified archive name.'
}

function Test-PowerShellPublishingScriptsParse {
    [scriptblock]::Create((Get-Text 'scripts/upload-curseforge.ps1')) | Out-Null
}

Test-CurseForgeUploadReportsVerifiedFileId
Test-ReleaseWorkflowUsesRealCurseForgeSlug
Test-ReleaseWorkflowUsesLoaderQualifiedMultiloaderArtifacts
Test-CurseForgeOnlyPublishWorkflow
Test-UploadScriptDefaultsUseLoaderQualifiedFabricArtifacts
Test-MavenPublicationKeepsStableArtifactId
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
