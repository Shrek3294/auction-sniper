# Auction Sniper Implementation Plan

Goal: A mod that automatically refreshes the Auction House page and scans for valid items under a certain price. If found, it alerts the user via Ntfy and sound.

## Phase 1: Project Setup
- Standard Fabric 1.21.1 setup.
- Dependencies: Fabric API.

## Phase 2: Configuration
- Use `auction_sniper.json`.
- Fields:
    - `targetItemName`: String (e.g., "Diamond")
    - `maxPrice`: double (e.g., 1000.0)
    - `ntfyTopic`: String
    - `refreshIntervalMin`: int (milliseconds, default 1000)
    - `refreshIntervalMax`: int (milliseconds, default 2000)

## Phase 3: The Scanner (Mixin)
- **Target**: `GenericContainerScreen` (or `HandledScreen`).
- **Logic**:
    - Iterate through `screen.getScreenHandler().getSlots()`.
    - Check Item Name vs `targetItemName`.
    - detailed check: Read Item Lore for "Price: $XXX".
    - If Match:
        - `NtfyService.send()`
        - Play `SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP`.

## Phase 4: Auto-Refresh
- **Logic**:
    - `ClientTickEvents` or `Screen.tick()`.
    - Counter decreases by `delta`.
    - When 0:
        - Reset counter to `Random(min, max)`.
        - Simulate click on "Refresh" slot (usually the last slot or specific ID).
        - **Constraint**: Only if title is "Auction House".

## Safe Mode
- No auto-buy for now.
- Random jitter enabled by default.


NEW CHANGES
Proposed Changes
[Component] Timing & Randomization Logic
I will introduce a HumanActionHandler (or similar utility) to manage delayed actions. Instead of executing actions immediately, we will queue them with a random delay.

[NEW] 
HumanActionHandler.java
A utility to queue tasks with random delays (e.g., human reaction time).
[Component] UI Interactions (
HandledScreenMixin
)
[MODIFY] 
HandledScreenMixin.java
Replace instant calls to 
attemptPurchase
 and 
attemptConfirmPurchase
 with queued actions through HumanActionHandler.
Add a random delay (e.g., 150-500ms) for the initial click.
Add a random delay (e.g., 200-600ms) for the confirmation click.
[Component] Auto-Refresh (
AutoRefreshHandler
)
[MODIFY] 
AutoRefreshHandler.java
Add a small random "jitter" to the AH_COMMAND_COOLDOWN_MS and other fixed timings.
Ensure the click interaction uses HumanActionHandler to avoid "perfect" timing relative to the tick.
[Component] Auto-Relist (
AutoRelistHandler
)
[MODIFY] 
AutoRelistHandler.java
Add delays between moving items in the inventory.
Add a delay before sending the /ah sell command after equipping the item.
[Component] Interaction Patterns
[MODIFY] 
SniperConfig.java
Add configuration settings for "Human-like delays" (enabled by default).
Add configurable ranges for reaction times.
[Component] Long-Term Safety & Behavior
[NEW] 
SessionManager.java
Track active time and enforced "sleep" periods.
Limit max snipes per hour/day to stay within "humanly possible" statistical bounds.
Automatically pause the mod after a certain amount of time (e.g., 2-3 hours) for a mandatory break (e.g., 15-30 mins).
[MODIFY] 
SniperConfig.java
Add configuration for session limits and break durations.
Add "Action Budget" settings.



Long-Term Risk Analysis
Running the mod for hours straight introduces specific risks:

Statistical Outliers: Servers track actions per minute. A consistent 20-30 refreshes/min for 8 hours is impossible for a human.
Session Persistence: Humans need to eat, sleep, and do other things. Constant AH activity for long stretches is a major indicator of automation.
Pattern Consistency: Even with random delays, a bot that only refreshes the AH and nothing else stands out.
To mitigate these, we will implement:

Mandatory Breaks: Enforced downtime where the mod stops entirely.
Action Limits: Stopping once a certain number of snipes are made to avoid looking like a bulk-reselling operation.
Variable Intensity: Randomly changing the "interest level" (refresh speed) throughout the session.
Verification Plan
Automated Tests
Since this is a Fabric mod, automated testing is limited without a full environment. I will rely on manual verification and debug logging to ensure delays are active.
Manual Verification
Enable debugLog in config.
Observe the logs to ensure there is a gap between "match item" and "auto-buy click".
Verify that clicks are not happening in the same tick as the screen opening or item appearing.
Test the auto-refresh and ensure it doesn't look "robotic" (intervals should vary slightly more than they currently do).
Test the relist flow and ensure the item move and sell command have a noticeable pause between them.