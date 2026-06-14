# Internal Changelog

Internal repo, build, workflow, and release-process changes are documented here. These notes are for maintainers and are not published to GitHub Releases, Modrinth, or CurseForge.

## Unreleased

- Wired the NeoForge client payload/status/anchor-sync transport runtime through loader-native client handlers and capability-gated sends.
- Wired the NeoForge payload/status/anchor-sync transport slice through loader-native payload registration and capability-gated server sends.
- Wired NeoForge portal sign use/break events and enabled the shared sign create/edit protection mixin for the sign-event adapter slice, with source-contract tests until a NeoForge runtime harness is added.
- Wired the first NeoForge runtime bootstrap slice for config/version setup, command/lifecycle hooks, and native permission node registration.
- Added the initial NeoForge source-root, metadata, and build skeleton for the multiloader baseline.
- Added common permission policy and payload channel contract seams for the multiloader boundary.
- Began the multiloader transition with Fabric-preserving common/Fabric source roots and loader-boundary checks.
- Hardened CurseForge upload verification to detect API error payloads, report the returned file ID, use the real SignPort CurseForge slug, and gate the CurseForge-only retry workflow.
- Added a release recovery workflow for retrying CurseForge uploads after upstream API failures.
