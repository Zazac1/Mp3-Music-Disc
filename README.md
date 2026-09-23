# MP3 Music Discs — Minecraft 26.3

Fabric mod that adds configurable music discs which play MP3 audio.

## Development setup

Requirements:

- JDK 25
- Internet access on the first Gradle run (to download Minecraft and Gradle dependencies)

Clone and select the `26.3` branch:

```powershell
git clone --branch 26.3 https://github.com/Zazac1/Mp3-Music-Disc.git
cd Mp3-Music-Disc
.\gradlew.bat build
```

To launch the development client and server together on Windows, run:

```powershell
.\run-local-test.ps1
```

The script detects JDK 25 in `C:\Program Files\Eclipse Adoptium` or `C:\Program Files\Java`, maintains an isolated local Gradle cache, then launches a local Fabric server and client. Its generated directories (`run-client`, `run-server`, `build`, and Gradle caches) are deliberately not committed.

The bundled `libs/jlayer-1.0.1.jar` is required at both compile time and runtime and is tracked in Git.

## Build output

After a successful build, the mod JAR is in `build/libs/`.
