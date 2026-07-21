# MineDevice — CLAUDE.md

## Project Overview

MineDevice is an Architectury mod for Minecraft 1.20.1 that adds functional communication devices (mobile phone, home phone, walkie talkie, megaphone) and a virtual economy (ATM, bank, currency items) to the game. Developed by CatLab Design.

- **Mod ID**: `minedevice`
- **Package**: `com.sammy.minedevice`
- **License**: PolyForm Noncommercial 1.0.0
- **Version**: 1.1.0

## Build & Run

```bash
./gradlew build                    # Build all platforms
./gradlew :fabric:runClient        # Run Fabric dev client
./gradlew :forge:runClient         # Run Forge dev client
./gradlew :fabric:build            # Build only Fabric
./gradlew :forge:build             # Build only Forge
./gradlew :common:build            # Build only common module
```

**Requirements**: JDK 21 to build (set in `gradle.properties`), Java 17 bytecode target. Gradle 8.12.1 via wrapper.

## Project Structure

```
common/                          # ~95% of code (shared gameplay, UI, networking, resources)
  src/main/java/com/sammy/minedevice/
    Minedevice.java              # Main mod init class (MOD_ID = "minedevice")
    ModBlocks.java               # Block registry (DeferredRegister pattern)
    ModItems.java                # Item registry
    ModBlockEntities.java        # Block entity registry
    ModSounds.java               # Sound event registry
    ModParticles.java            # Particle type registry
    ModMenus.java                # Menu/screen handler registry
    ModCreativeTabs.java         # Creative tab registry
    block/                       # Block classes & block entities
    item/                        # Item classes (PhoneItem, MegaphoneItem, etc.)
    phone/                       # Core phone system (networking, call manager, chat, DB)
    client/phone/                # Phone UI rendering (PhoneScreen.java ~5412 lines)
    airstrike/                   # Airstrike radio system
    atm/                         # ATM/bank system
    walkie/                      # Walkie talkie system
    homephone/                   # Home phone ring state
    megaphone/                   # Megaphone voice integration
    voice/                       # Voice provider abstraction (Plasmo Voice / SVC)
    command/                     # Admin commands
    mixin/                       # Mixins (PlayerMixin + client mixins)
  src/main/resources/assets/minedevice/   # Textures, models, sounds, lang, blockstates
fabric/                          # Fabric entrypoint (thin wrapper)
  MinedeviceFabric.java          # ModInitializer
  MinedeviceFabricClient.java     # ClientModInitializer
forge/                           # Forge entrypoint (thin wrapper)
  MinedeviceForge.java           # @Mod class with DistExecutor
  MinedeviceSvcForgePlugin.java  # Simple Voice Chat plugin
```

## Architecture & Patterns

### Multi-Platform (Architectury)
- `common/` contains all shared code. Fabric and Forge subprojects are thin entrypoints that call `Minedevice.init()`.
- Fabric: `fabric.mod.json` — Fabric Loader 0.18.4+, requires Fabric API
- Forge: `META-INF/mods.toml` — Minecraft Forge 1.20.1-47.4.16
- SQLite JDBC is bundled via Shadow plugin in both platform JARs.

### Registration Pattern
- Each registry category has a dedicated final class with private constructor.
- Uses `DeferredRegister` from Architectury's registry API (NOT the static Forge or Fabric registries).
- Consistent pattern:
  ```java
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Minedevice.MOD_ID, Registries.ITEM);
  public static final RegistrySupplier<Item> PHONE = ITEMS.register("phone", () -> new PhoneItem(...));
  public static void init() { ITEMS.register(); }
  ```
- Register in `Minedevice.init()` in order: Sounds → Particles → Blocks → BlockEntities → Items → Menus → CreativeTabs.

### Initialization Flow (`Minedevice.init()`)
1. Register all registries (order above)
2. Initialize networking: `PhoneNetworking.init()`, `WalkieNetworking.init()`, `AtmNetworking.init()`
3. Initialize airstrike system: `AirstrikeManager.init()`
4. Register admin commands
5. Register server lifecycle events (chat DB init on SERVER_STARTED, shutdown on SERVER_STOPPING)
6. Detect voice provider (Plasmo Voice → SVC → None) and initialize hooks via reflection

### Voice Provider Abstraction
- `VoiceProviderDetector.detect()` checks for mods via `Platform.isModLoaded("plasmovoice")` or `Platform.isModLoaded("voicechat")`.
- **VoiceProvider enum**: `PLASMO_VOICE`, `SIMPLE_VOICE_CHAT`, `NONE`
- Server hooks initialized via reflection (`Class.forName`, `Method.invoke`) — not compile-time deps.
- Plasmo Voice: home phone, megaphone, and walkie voice hooks.
- Simple Voice Chat (SVC): plugin lifecycle (`MinedeviceSvcPlugin`).

### Networking
- Uses `dev.architectury.networking.NetworkManager` with `ResourceLocation`-based packet IDs.
- C2S receivers registered in platform networking init classes (`PhoneNetworking.init()`, `WalkieNetworking.init()`, etc.).
- Packets use `FriendlyByteBuf` (Netty) with read/write helpers.
- All packet handling wrapped in `context.queue()` for main thread execution.
- Packet ID pattern: `minedevice:{system}_{action}` (e.g., `phone_call_request`, `walkie_ptt`).
- S2C sends use `NetworkManager.sendToPlayer(player, PACKET_ID, buf)`.
- C2S receivers use `NetworkManager.registerReceiver(NetworkManager.c2s(), PACKET_ID, (buf, context) -> { ... })`.

### Call Management
- `PhoneCallManager` (1269 lines): maps active calls in `ConcurrentHashMap<UUID, CallState>` (player UUID → call state) and home phone calls.
- `PhoneCallState` enum: `IDLE → OUTGOING_RINGING → INCOMING_RINGING → CONNECTED → MISSED`
- `Endpoint` interface: `PlayerEndpoint` (UUID-based) or `HomePhoneEndpoint` (address+number).
- Call timeout: 20 seconds (400 ticks) for unanswered calls.
- Speakerphone auto-hangup: 10 seconds if no player nearby.

### Database & Persistence
- **SQLite** via `org.xerial:sqlite-jdbc:3.42.0.0` (bundled in JAR).
- Two databases: `minedevice_chat.db` (chat messages + contacts) and `minedevice_call_log.db` (call history).
- Stored in the Minecraft server directory (`user.dir`).
- **Read-write lock** pattern (`ReentrantReadWriteLock`) in `ChatDatabase` and `CallLogDatabase`.
- **ChatCache**: batch operations with periodic flush every 15s via `ScheduledExecutorService`.
- **WAL mode** enabled for SQLite performance.
- **Server SavedData**: `AtmAccountStore`, `CardAccountStore`, `TransactionHistoryStore` — For economy data (Minecraft's native persistence).
- **ItemStack NBT**: Contacts, photos, messages stored in phone ItemStack NBT (alongside SQLite backup for cross-player delivery).

### Mixin Usage
- **`PlayerMixin`** (server): implements `PhoneCallPoseAccess` — adds 6 synced entity data booleans (call pose, camera pose, selfie, bank QR, chat QR, screen-on). Also prevents dropping home phone handset items.
- **Client mixins** (8 total): `CameraMixin`, `GameRendererMixin`, `GuiMixin`, `HumanoidModelMixin`, `ItemInHandLayerMixin`, `ItemInHandRendererMixin`, `KeyboardInputMixin`, `LevelRendererMixin`, `LocalPlayerMixin`.
- Mixin config: `minedevice.mixins.json`.

### GUI System
- **`PhoneScreen.java`** (5412 lines): monolithic phone UI with all app rendering (call, chat, camera, gallery, bank, settings, contacts, call log).
- Sub-renderers: `PhoneScreenDraw`, `PhoneScreenLayout`, `PhoneScreenModels`, `PhoneCallSurfaceRenderer`, `PhoneChatSurfaceRenderer`, `PhoneMediaSurfaceRenderer`, `PhoneSettingsSurfaceRenderer`, `PhoneBankSurfaceRenderer`.
- Other screens: `HomePhoneScreen.java`, `AtmScreen.java`, `BankScreen.java`, `WalkieScreen.java`, `LabtopScreen.java`.

### Player Pose System
- `PhoneCallPoseAccess` interface — synced via entity data on `Player`.
- Poses: phone call (left/right hand), camera mode, selfie mode, bank QR, chat QR, screen-on state.
- Uses Minecraft's `EntityDataAccessor<Boolean>` synced data.

### Economy System
- Virtual currency items: Bills (20, 100, 500, 1000) + Coin — all `Item` instances.
- Bank accounts: `AtmAccountStore` (SavedData), per-player balance via `UUID → long`.
- ATM: physical block with card/PIN system, deposit/withdraw/transfer.
- Bank block: used to unlock locked ATM cards.
- Mobile banking: via phone Bank app (check balance, transfer, QR scan).

## Key Dependencies

| Dependency | Version |
|---|---|
| Minecraft | 1.20.1 |
| Architectury API | 9.2.14 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.92.7+1.20.1 |
| Forge | 1.20.1-47.4.16 |
| Plasmo Voice API (optional) | 2.1.8 |
| Simple Voice Chat API (optional) | 2.5.0 |
| SQLite JDBC | 3.42.0.0 |

Gradle plugins: `dev.architectury.loom` 1.9-SNAPSHOT, `architectury-plugin` 3.4-SNAPSHOT, `com.github.johnrengelman.shadow` 8.1.1.
Mappings: Official Mojang mappings (`loom.officialMojangMappings()`).

## Coding Conventions

- **Language**: Java 17, no external style checker configured.
- **Final classes with private constructors** for utility/registry classes.
- **SLF4J** logging via `com.mojang.logging.LogUtils`.
- **No access wideners** — all bytecode manipulation via Mixin.
- **No data generators** — assets are hand-authored.
- **No test framework** configured — no test source sets.
- **Client-only code** under `client/` subpackage — always guarded by `level.isClientSide` or loaded via reflection from common code.
- **Server-only logic** accessed via `context.queue()` in network receivers.
- **Architectury API** used for cross-platform abstractions (registries, networking, events, platform queries).
- **Phone data** stored on `ItemStack` via NBT for single-player state, with SQLite for cross-player delivery.
- **Phone contacts are unbounded** — `MAX_CONTACTS` was removed. Contacts are stored as a `ListTag` on the phone ItemStack NBT with no limit. Chat friends use SQLite with no limit. Network sync caps at 64 friends/conversations via `PhoneChatStatePayload`.
- **Phone camera sensitivity** must use vanilla formula `base³ * 8.0` (no extra factor). Past bug: `* 0.15` factor made sensitivity 15% of normal. Always reset `keyUse` and `keyAttack` when exiting camera move mode to prevent stuck inputs.
- **Stale widget focus bug**: `clearWidgets()` (vanilla `Screen`) does NOT clear the focused child reference. Always call `setFocused(null)` after `clearWidgets()` in `rebuildWidgets()` to prevent orphaned widgets from receiving SPACE/ENTER key events. This affects `PhoneScreen.java:491`.
- **Scroll pattern for phone lists**: Call Recents was the reference pattern. Contacts and Chat Friends replicate it: scroll offset field, `mouseScrolled` handler, helper methods for row geometry, render loop limited to `scrollOffset → scrollOffset + visibleRows`, inline Y position computation, and a proportional scrollbar. See `PhoneScreen.java`, `PhoneCallSurfaceRenderer.java`, `PhoneChatSurfaceRenderer.java`.
- **Reflection** used for optional voice integration (Plasmo Voice / SVC) — no compile-time dependency required.
- **Thai font integration**: TTF font at `assets/minedevice/font/font.ttf`. Font definition at `assets/minecraft/font/default.json` injects the TTF as an additional GlyphProvider into `minecraft:default` — FontManager auto-merges providers across resource packs at reload time. No runtime reflection or `Font` replacement needed.
- **IMPORTANT**: TTF `file` field in font JSON must NOT include the `font/` directory prefix — Minecraft's `TrueTypeGlyphProviderDefinition` automatically prepends it via `location.withPrefix("font/")`. Use `"minedevice:font.ttf"`, NOT `"minedevice:font/font.ttf"`.
