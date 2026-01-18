# Auction Sniper (Fabric client mod) (tested on Donut SMP)

Client-side Fabric mod that watches an in-game “Auction House” chest GUI for underpriced listings, plays an alert sound, and can optionally auto-buy / auto-relist. Notifications can also be pushed to `ntfy.sh`.
# See my other mods here  https://discord.gg/b2V2T88V
## What it does

- **Scans auction listings** for an item name substring (`targetItemName`) and parses tooltip lines like `Price: $1,600,000` (also supports `K/M/B` suffixes).
- **Notifies on matches** (sound + optional `ntfy.sh` push) and dedupes repeats for ~60s per listing.
- **Auto-refreshes** the auction GUI at a randomized interval by clicking a “refresh/next/prev” style button item.
  - Default refresh interval is **1000-1500ms** (about **40-60 refreshes/min**). Configure via `refreshIntervalMin` / `refreshIntervalMax`.
- **Optional auto-buy**: clicks the listing, then auto-confirms on a confirm screen.
- **Optional auto-relist**: after purchase, equips the bought item and runs a configurable sell command, then confirms.

## Warning

This is an experimental automation mod. Using auto-refresh/auto-buy on servers may violate rules and can get you flagged by anti-cheat and/or banned. Avoid extended/unattended use; use at your own risk.

## Requirements

- Minecraft **~1.21.10** (see `src/main/resources/fabric.mod.json`)
- **Fabric Loader** and **Fabric API**
- **Java 21** (the project is configured for Java 21)

Note: `gradle.properties` currently pins `org.gradle.java.home` to a local path. If you’re building on another machine, update or remove that line.

## Install (as a mod)

1. Install Fabric Loader for your Minecraft version.
2. Put **Fabric API** in your `mods/` folder.
3. Build this mod and copy the jar to `mods/`:
   - Output jar: `build/libs/auctionsniper-<version>.jar`
4. Launch the game.

## Build & run (development)

Windows:
- Build: `.\gradlew.bat build`
- Run a dev client: `.\gradlew.bat runClient`

macOS/Linux:
- Build: `./gradlew build`
- Run a dev client: `./gradlew runClient`

## Configuration

Config file is created on first launch:
- Normal install: `.minecraft/config/auctionsniper.json`
- Dev run: `run/config/auctionsniper.json`

### Options

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `targetItemName` | string | `Ancient Debris` | Substring match against listing item name. |
| `maxPrice` | number | `1600000.0` | Alert/buy if parsed price is `<= maxPrice`. |
| `ntfyTopic` | string | `minecraft_sniper_test` | Topic name for `https://ntfy.sh/<topic>` (no spaces). |
| `refreshIntervalMin` | int (ms) | `1000` | Minimum delay between refresh clicks. |
| `refreshIntervalMax` | int (ms) | `1500` | Maximum delay between refresh clicks. |
| `autoRefreshEnabled` | boolean | `true` | Enables GUI auto-refresh. |
| `autoBuyEnabled` | boolean | `false` | Enables clicking a matching listing + confirm. |
| `ahCommandFormat` | string | `ah %s` | Command used to return to auction UI (should include `%s` for the search term). |
| `autoRelistEnabled` | boolean | `false` | Enables auto-relist flow after buying. |
| `relistPrice` | number | `0.0` | Sell price used by auto-relist (must be `> 0`). |
| `ahSellCommandFormat` | string | `ah sell %s` | Sell command format (should include `%s` for the price). |

Example `auctionsniper.json`:

```json
{
  "targetItemName": "Ancient Debris",
  "maxPrice": 1600000.0,
  "ntfyTopic": "minecraft_sniper_test",
  "refreshIntervalMin": 1000,
  "refreshIntervalMax": 1500,
  "autoRefreshEnabled": true,
  "autoBuyEnabled": false,
  "ahCommandFormat": "ah %s",
  "autoRelistEnabled": false,
  "relistPrice": 0.0,
  "ahSellCommandFormat": "ah sell %s"
}
```

## In-game commands

All commands are client commands under `/auctionsniper` (see `src/main/java/com/example/auctionsniper/SniperCommand.java`):

- `/auctionsniper help`
- `/auctionsniper status`
- `/auctionsniper toggle` (auto-refresh on/off)
- `/auctionsniper target <item name...>`
- `/auctionsniper price <amount>`
- `/auctionsniper interval <minMs> <maxMs>`
- `/auctionsniper buy <on|off>`
- `/auctionsniper relist <on|off>`
- `/auctionsniper relistprice <amount>`
- `/auctionsniper ahcmd <format...>` (example: `ah %s` or `/ah search %s`)
- `/auctionsniper sellcmd <format...>` (example: `ah sell %s` or `ah sell %s 24h`)
- `/auctionsniper ntfy <topic...>`

## Notifications (ntfy)

Set a topic (no spaces) and subscribe:
- Topic config: `ntfyTopic`
- Subscribe in a browser/app: `https://ntfy.sh/<topic>`

`ntfy.sh` topics are generally public by default; avoid using sensitive topic names.

## Debug logging

The mod writes a simple debug log to:
- Normal install: `.minecraft/logs/auctionsniper-debug.log`
- Dev run: `run/logs/auctionsniper-debug.log`

Useful entries include matches, auto-buy clicks, confirm clicks, relist state, and ntfy send status.

## How it works (code map)

- `src/main/java/com/example/auctionsniper/AuctionSniperMod.java`: client entrypoint; loads config; registers handlers/commands.
- `src/main/java/com/example/auctionsniper/mixin/HandledScreenMixin.java`: scans GUI slots, parses tooltip price/seller, triggers notify/auto-buy/auto-confirm.
- `src/main/java/com/example/auctionsniper/AutoRefreshHandler.java`: randomized refresh clicking + optional “return to AH” command.
- `src/main/java/com/example/auctionsniper/AutoRelistHandler.java`: sells purchased item using configured command, then confirms.
- `src/main/java/com/example/auctionsniper/SniperConfig.java`: JSON config load/save (`config/auctionsniper.json`).
- `src/main/java/com/example/auctionsniper/NtfyService.java`: async HTTP POST to `ntfy.sh`.

## Known limitations / tuning

- **Server-specific UI**: detection relies on GUI titles containing “auction” / “confirm” (with some normalization for encoding artifacts). Different servers/languages may need code changes.
- **Refresh button heuristics**: refresh clicks are based on item name containing “refresh/next page/previous page” or certain items (clock/arrow/anvil). This may misclick on some UIs.
- **Tooltip format assumptions**: price parsing expects a line like `Price: $<number>`; if your server formats price differently, update the regex in `HandledScreenMixin`.
- **Experimental / anti-cheat risk**: this is an experimental mod. Using auto-refresh/auto-buy for extended periods may get you flagged by anti-cheat and/or banned depending on the server rules. Avoid long unattended sessions; use at your own risk.

## Project status / roadmap

See `task.md` and `implementation_plan.md` for current progress and planned improvements.

## License

`src/main/resources/fabric.mod.json` declares `CC0-1.0`. If you plan to distribute this project, consider adding a top-level `LICENSE` file to match that declaration.
