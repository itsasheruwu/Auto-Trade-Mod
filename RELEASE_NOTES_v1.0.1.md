## Auto Trade Mod v1.0.1

### Fixes
- Fixed villager trade UI ghosting/flickering caused by client-side trade switching.
- Reworked auto-trade selection to be server-authoritative using synced merchant button packets.
- Added settle/wait logic before taking output to reduce desync on multiplayer servers.
- Improved retry behavior when output is temporarily unavailable.

### Result
- Auto-trading should now execute reliably without manually clicking trades and without visual ghosting.
