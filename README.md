# MineDevice Megaphone

Architectury mod (common + fabric + forge) that adds a `Megaphone` item.

When enabled, and while the player is holding the megaphone in either hand, a Plasmo Voice input filter is applied:

- High-pass around `1300 Hz`
- Low-pass around `3400 Hz`
- Soft clipping with threshold near `0.34`

## Build Requirements

- Use JDK `21` to run Gradle (Architectury Loom `1.13-SNAPSHOT` requires it).
- Mod bytecode is still compiled for Java `17`.

## Usage

1. Install this mod and Plasmo Voice on the client.
2. Craft `Megaphone` (placeholder texture uses iron ingot).
3. Hold right click with `Megaphone` and speak.
4. While active, you hear your own processed voice (loopback) and other players hear the same effect.

The filter is removed automatically when you stop using the item and on world/server disconnect.
