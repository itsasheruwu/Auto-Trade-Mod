# Auto Trade Mod (Fabric 1.21.11)

Client-only Fabric mod that repeats a selected villager trade while the merchant UI is open.

## Features
- Mod Menu + Cloth Config settings screen.
- Trade selection scope modes:
  - Global
  - Per profession
  - Per villager
- Rate modes:
  - Conservative (5 ticks)
  - Moderate (2 ticks)
  - Fast (1 tick)
- Merchant UI buttons:
  - `Auto: ON/OFF`
  - `Set Target = Selected`
- Auto loop pauses/retries when the result is unavailable or inventory is full.
- Optional: keep Auto armed when the merchant screen closes and reopens.
- GitHub auto-updater:
  - Checks latest release from GitHub during startup.
  - Downloads and swaps jar automatically.
  - Updated code still requires restarting the game process.

## Build
```bash
./gradlew build
```

## Run Client
```bash
./gradlew runClient
```

## Config File
Stored at:
- `config/autotrade.json`

Key fields:
- `selectionScope`
- `globalTradeIndex` (1-based)
- `tradeIndexByProfession`
- `tradeIndexByVillager`
- `rateMode`
- `showInScreenStatusText`
- `pauseRetryOnUnavailable`
- `keepEnabledAcrossScreens`
- `autoUpdateEnabled`
- `autoUpdateGithubOwner`
- `autoUpdateGithubRepo`
