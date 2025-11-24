# Build Instructions
## Legendary Weapons SMP Plugin

This guide explains how to build the plugin from source.

## Prerequisites

- **Java Development Kit (JDK) 21 or higher**
  - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
  - Verify installation: `java -version`

## Quick Build (Windows)

1. Open Command Prompt in the project directory
2. Run:
   ```bash
   gradlew.bat build
   ```
3. Find the compiled JAR in `build/libs/LegendaryWeaponsSMP-1.0.0.jar`

## Quick Build (Linux/Mac)

1. Open Terminal in the project directory
2. Make gradlew executable (first time only):
   ```bash
   chmod +x gradlew
   ```
3. Run:
   ```bash
   ./gradlew build
   ```
4. Find the compiled JAR in `build/libs/LegendaryWeaponsSMP-1.0.0.jar`

## First-Time Setup

If the Gradle wrapper is not fully set up, you may need to:

1. **Install Gradle globally** (optional but helpful):
   - Download from: https://gradle.org/releases/
   - Or use a package manager:
     - Windows: `choco install gradle`
     - Mac: `brew install gradle`
     - Linux: `sudo apt install gradle` or `sudo yum install gradle`

2. **Initialize the Gradle wrapper**:
   ```bash
   gradle wrapper --gradle-version 8.5
   ```

3. **Then build normally**:
   ```bash
   gradlew build
   ```

## Build Commands

### Build the plugin
```bash
gradlew build
```
Compiles the code and creates the plugin JAR in `build/libs/`

### Clean build directory
```bash
gradlew clean
```
Removes all compiled files and build artifacts

### Clean and rebuild
```bash
gradlew clean build
```
Ensures a fresh build from scratch

### Build without running tests
```bash
gradlew build -x test
```
Skips test execution (currently there are no tests)

## Troubleshooting

### "gradlew: command not found" (Linux/Mac)
**Solution:** Make the script executable
```bash
chmod +x gradlew
```

### "JAVA_HOME is not set"
**Solution:** Set the JAVA_HOME environment variable

**Windows:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-21
```

**Linux/Mac:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

Or add to your `.bashrc` or `.zshrc` for persistence.

### "Could not resolve dependencies"
**Solution:** Check your internet connection. Gradle needs to download:
- Paper API (from https://repo.papermc.io)
- Shadow plugin (from Maven Central)

### Build succeeds but JAR doesn't work
**Possible causes:**
1. Wrong Minecraft version - This plugin is for 1.21.8
2. Wrong server software - Use Paper (or Spigot-compatible)
3. Missing dependencies - Ensure Paper API is available

## IDE Setup

### IntelliJ IDEA
1. Open the project folder in IntelliJ
2. IntelliJ should auto-detect the Gradle project
3. Wait for Gradle to sync dependencies
4. To build: View → Tool Windows → Gradle → Tasks → build → build

### Eclipse
1. Install the Gradle plugin (Buildship)
2. File → Import → Gradle → Existing Gradle Project
3. Select the project folder
4. Wait for dependencies to download
5. To build: Right-click project → Gradle → Tasks → build

### VS Code
1. Install the "Gradle for Java" extension
2. Open the project folder
3. VS Code should detect the Gradle project
4. To build: Open Command Palette (Ctrl+Shift+P) → "Gradle: Run Tasks" → "build"

## Build Output

After a successful build:

```
BUILD SUCCESSFUL in 15s
3 actionable tasks: 3 executed
```

The compiled JAR will be at:
```
build/libs/LegendaryWeaponsSMP-1.0.0.jar
```

## Installation

1. Copy the JAR file from `build/libs/`
2. Place it in your Paper server's `plugins/` folder
3. Restart or reload the server
4. Check console for:
   ```
   [LegendaryWeaponsSMP] Legendary Weapons SMP plugin enabled!
   ```

## Development Build

For development and testing, you can use:

```bash
gradlew shadowJar
```

This creates a fat JAR with all dependencies included.

## Custom Build

To change the version number:

1. Edit `build.gradle`
2. Change the `version` line:
   ```gradle
   version = '1.0.1'
   ```
3. Rebuild:
   ```bash
   gradlew clean build
   ```

## Contributing

When contributing code:

1. Fork the repository
2. Make your changes
3. Test locally: `gradlew build`
4. Ensure build succeeds
5. Submit a pull request

## Additional Resources

- **Gradle Documentation:** https://docs.gradle.org/
- **Paper API Documentation:** https://docs.papermc.io/
- **Java Documentation:** https://docs.oracle.com/en/java/

---

**Build successfully? You're ready to deploy your legendary weapons!** ⚔️
