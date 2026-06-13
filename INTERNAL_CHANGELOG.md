# Internal Changelog

Internal repo, build, workflow, and release-process changes are documented here. These notes are for maintainers and are not published to GitHub Releases, Modrinth, or CurseForge.

## Unreleased

- Wired the first NeoForge runtime bootstrap slice for config/version setup, command/lifecycle hooks, and native permission node registration.
- Added the initial NeoForge source-root, metadata, and build skeleton for the multiloader baseline.
- Added common permission policy and payload channel contract seams for the multiloader boundary.
- Began the multiloader transition with Fabric-preserving common/Fabric source roots and loader-boundary checks.
- Hardened CurseForge upload verification to detect API error payloads, report the returned file ID, use the real SignPort CurseForge slug, and gate the CurseForge-only retry workflow.
- Added a release recovery workflow for retrying CurseForge uploads after upstream API failures.
