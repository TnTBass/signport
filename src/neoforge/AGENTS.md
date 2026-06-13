# NeoForge Source Root

This root owns NeoForge server-side adapter code.

- NeoForge entrypoints, lifecycle/event registration, NeoForge networking transport, config paths, permissions, metadata, and optional-integration detection belong here.
- Keep shared SignPort behavior in `src/common` and call it through narrow interfaces or facades.
- Do not add Fabric adapter code or Fabric resources here.
