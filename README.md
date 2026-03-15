# MineDevice

MineDevice is an Architectury mod for Minecraft `1.20.1` that adds a usable mobile phone and home phone system.

The project currently targets both `Fabric` and `Forge`, with shared gameplay logic in `common/`.

## Features

- Mobile phone item with in-game app UI
- Calling between mobile phones and home phones
- Home phone block with handset and speakerphone flow
- Chat app with friend list, message history, and offline delivery
- Camera and gallery system on the mobile phone
- Optional Plasmo Voice integration for live phone conversations

## Current Apps

- `Call`: dial numbers, answer calls, hang up, and manage saved contacts
- `Chat`: add friends by number, send messages, review conversation history, and delete conversations
- `Camera`: take photos in-game and save them to the phone
- `Gallery`: browse saved photos inside the phone UI

## Platforms

- Minecraft `1.20.1`
- Fabric Loader `0.18.4+`
- Forge `47.4.16+`
- Architectury API `9.2.14+`

## Optional Dependency

- `Plasmo Voice`

Plasmo Voice is optional, but recommended if you want in-call voice audio for the mobile phone and home phone systems.

## Development Setup

### Requirements

- JDK `21` to run Gradle tasks
- Java `17` target bytecode for the mod output

## Build

```bash
./gradlew build
```

Useful tasks:

```bash
./gradlew :common:build
./gradlew :fabric:build
./gradlew :forge:build
```

## Run Dev Client

```bash
./gradlew :fabric:runClient
```

or

```bash
./gradlew :forge:runClient
```

### Project Structure

- [`common/`](common): shared gameplay code, UI, networking, resources
- [`fabric/`](fabric): Fabric entrypoints and platform glue
- [`forge/`](forge): Forge entrypoints and platform glue

## License

This project is `All Rights Reserved`.

See [`LICENSE.txt`](LICENSE.txt).

## Author

- `SamSu255`
