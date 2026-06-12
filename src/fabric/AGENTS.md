# Fabric Source Root

This root owns Fabric server-side adapter code.

- Fabric entrypoints, lifecycle/event registration, Fabric networking transport, Fabric config paths, Fabric permissions, Fabric metadata, and Fabric optional-integration detection belong here.
- Keep shared SignPort behavior in `src/common` and call it through narrow interfaces or facades.
- Do not add NeoForge adapter code or NeoForge resources here.
