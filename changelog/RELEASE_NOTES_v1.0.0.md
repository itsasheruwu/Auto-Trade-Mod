## Auto Trade Mod v1.0.0

### What's new
- Added automatic villager trading loop for Fabric 1.21.11.
- Added in-screen controls in the villager trading UI:
  - `Auto: ON/OFF`
  - `Set Target = Current Selected Trade`
- Added settings UI with Mod Menu + Cloth Config:
  - Trade scope: Global / Per Profession / Per Villager
  - Trade speed: Conservative / Moderate / Fast
  - Status text toggle and mapping reset actions
- Added context-aware trade target storage (global/profession/villager UUID).
- Added safety behavior for unavailable trades and inventory-full pauses.
- Added GitHub auto-updater:
  - Checks latest release from this repository
  - Downloads and swaps the mod jar automatically
  - New version loads on next Minecraft launch

### Notes
- This is a client-side QoL mod for Fabric.
- Auto-update can be configured from mod settings.
