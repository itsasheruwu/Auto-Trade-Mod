## Auto Trade Mod v1.0.3

### Fixes
- Implemented true villager trade click emulation using MerchantScreen internal sync logic.
- Auto-trade now sets selected trade the same way as manual row clicks.
- Improved trade selection reliability on servers where previous selection methods desynced.

### Technical
- Added `MerchantScreenAccessor` mixin to invoke `syncRecipeIndex()` directly.
- Updated auto-trade engine to use click-emulation path for trade targeting.
