# Changelog

## 1.1.3-pre (1.20.1) / 1.1.3 (1.21.1) — 2026-07-21

### Bug Fixes

- **Contact & chat limits removed** — phone contacts and chat friends are now unbounded (previously capped at 64). All related `MAX_CONTACTS` constants and sync caps removed.
- **Double-digit number input** — fixed digit rolling logic so typing "11" correctly produces 11 instead of 02 or similar corruption.
- **Spacebar widget focus bug** — `clearWidgets()` in Minecraft's `Screen` does not clear the focused child reference. Calling `setFocused(null)` after `rebuildWidgets()` prevents orphaned widgets from receiving SPACE/ENTER key events (`PhoneScreen.java:491`).
- **Camera stuck inputs** — `keyUse` and `keyAttack` are now properly reset when exiting camera movement mode, preventing the player from being stuck in attack/use state.
- **Contacts/chat list scrolling** — scroll behavior was missing from Contacts and Chat Friends lists. Reimplemented following the Call Recents scroll pattern (scroll offset, `mouseScrolled` handler, row geometry helpers, proportional scrollbar).
- **Mouse sensitivity formula** — camera sensitivity now uses vanilla formula `base³ * 8.0` with no extra multiplier. Removed the erroneous `* 0.15` factor that made sensitivity 15% of normal.

### Features

- **Thai font support** — embedded TTF font at `assets/minedevice/font/font.ttf`. Font definition at `assets/minecraft/font/default.json` injects the TTF into `minecraft:default` for automatic CJK/Thai glyph rendering.
- **ATM cardless mode** — new `/minedevice atm require-card <true|false>` command to toggle whether the ATM requires a physical card. When disabled, players can access their account directly from the ATM block.

### Technical Changes

- **PhoneMediaSurfaceRenderer refactor** — monolithic media renderer split into focused and overview sub-renderers for maintainability.
- **PhoneCallStatePayload network sync** — 64-item cap applied to friend/conversation sync payloads to prevent oversized packets.
- **1.21.1 API migration fixes** (MCD-2 only):
  - `ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath(...)`
  - `renderBackground(guiGraphics)` → `renderBackground(guiGraphics, mouseX, mouseY, partialTick)`
  - Added `MapCodec CODEC` + `codec()` override to `LabtopBlock`
  - `use(...)` → `useWithoutItem(...)`; removed `playerWillDestroy` overrides
  - `vertex()` → `addVertex()`, `setColor()` API updates for 1.21.1 rendering
  - `SavedData.Factory<>` constructor and `HolderLookup.Provider` parameters for persistence
  - Missing `VoicechatConnection` import added to `SvcMegaphoneVoiceHook`
