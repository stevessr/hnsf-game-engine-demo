# AGENTS.md

## Project Name
**primary_software** — A Gradle-based Java Swing game prototype with a built-in map editor and SQLite-backed map persistence.

## Overview
This repository is a desktop game project written in Java. The main entry point (`org.example.App`) can launch either a playable game window or a visual map editor. The runtime is built around a 2D world model (`GameWorld`) that manages entities, rendering, physics, input handling, and game state transitions.

The gameplay loop uses Swing (`SwingGamePanel`) with keyboard/mouse input abstraction and a state machine (`DefaultGameStateMachine`) that controls `MENU`, `PLAYING`, `DIALOG`, and `PAUSED` behaviors. Objects such as players, monsters, walls, menus, and dialogs are modeled through a shared object hierarchy and updated/rendered each frame.

Map data can be saved and loaded through `MapRepository` (SQLite) and mapper/factory classes (`MapDataMapper`, `GameObjectFactory`) that serialize object metadata (including JSON extras for object-specific fields). The project also includes a standalone editor UI (`EditorWindow`, `MapEditorController`) for creating and modifying map objects visually.

## Technology Stack
- **Language/Runtime**: Java (Gradle toolchain configured to Java 25), JVM desktop application
- **Framework(s)**: Java Swing/AWT for UI rendering, JUnit 5 for tests
- **Key Dependencies**:
  - `ch.qos.logback:logback-classic` (logging)
  - `org.xerial:sqlite-jdbc` (embedded SQLite persistence)
  - `org.json:json` (object metadata JSON serialization)
  - Lombok plugin (`io.freefair.lombok`) for annotation tooling
- **Build Tools**:
  - Gradle Wrapper (`gradlew`, Gradle 9.4.0)
  - Checkstyle (`config/checkstyle/checkstyle.xml`)
  - SpotBugs (`config/spotbugs/spotbugs-exclude.xml`)

## Project Structure
```text
primary_software/
├── build.gradle.kts                      # Build config, dependencies, application main class, QA plugins
├── settings.gradle.kts                   # Gradle root project name
├── gradlew / gradlew.bat                 # Gradle wrapper scripts
├── gradle/wrapper/                       # Wrapper metadata (Gradle distribution URL/version)
├── config/
│   ├── checkstyle/checkstyle.xml         # Code style checks
│   └── spotbugs/spotbugs-exclude.xml     # SpotBugs exclusion rules
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── org/example/App.java      # Application entry point (game/editor mode)
│   │   │   └── lib/
│   │   │       ├── editor/               # Swing map editor window, controls, overlay, commands
│   │   │       ├── game/                 # Game world and level setup
│   │   │       ├── input/                # Keyboard/mouse managers and input action mapping
│   │   │       ├── manager/              # Entity manager
│   │   │       ├── object/               # Object model hierarchy and object factory
│   │   │       ├── persistence/          # SQLite repository and map/object mappers
│   │   │       ├── physics/              # AABB collision and movement resolution
│   │   │       ├── render/               # Swing game panel and frame loop
│   │   │       └── state/                # State enum, state context/settings, state machine
│   │   ├── resources/logback.xml         # Logging configuration
│   │   └── lib/data/                     # Pair/Triple utility classes (outside default Gradle java source set)
│   └── test/java/
│       ├── org/example/                  # Entry-point unit test
│       └── lib/...                       # Physics/input/state/entity/object tests + optional Robot UI test
├── hnsfGame01/                           # Legacy Eclipse project assets (not part of Gradle build)
├── 初级实作课程素材资源/                # Course material assets (not part of Gradle build)
└── build/                                # Generated build outputs, reports, distributions (generated)
```

## Key Features
- Playable 2D Swing game loop with frame-timed updates and rendering
- State-driven gameplay flow (menu/dialog/playing/paused)
- Configurable input mapping (WASD/IJKL/arrows, keyboard + mouse menu control)
- Basic collision and movement resolution via AABB physics
- Rich object model (player, monster, walls, boundaries, scene blocks, menu/dialog UI objects)
- Visual map editor with grid snap, create/select/drag/delete, undo/redo commands
- SQLite map persistence with object-specific JSON metadata serialization
- Automated tests for physics, input, entity management, object model, and state machine behavior

## Getting Started

### Prerequisites
- JDK 25+ (project toolchain is configured for Java 25; Java 26 also worked in this environment)
- A desktop environment for running Swing UI (game/editor)
- No separate database server required (SQLite file is local)

### Installation
```bash
# 1) Enter project root
cd /path/to/primary_software

# 2) Ensure wrapper is executable (Linux/macOS)
chmod +x ./gradlew

# 3) Build and run tests
./gradlew clean build
```

### Usage
```bash
# Run the game mode
./gradlew run

# Run the map editor mode (App checks first arg "editor")
./gradlew run --args="editor"

# Run tests only
./gradlew test
```

## Development

### Available Scripts
This project uses Gradle tasks (not npm scripts). Common tasks:

- `./gradlew run` — Start the desktop application
- `./gradlew build` — Compile + run tests + assemble artifacts
- `./gradlew test` — Run JUnit test suite
- `./gradlew check` — Run verification tasks
- `./gradlew checkstyleMain checkstyleTest` — Run style checks
- `./gradlew spotbugsMain spotbugsTest` — Run static bug analysis
- `./gradlew clean` — Remove generated build outputs
- `./gradlew distZip distTar installDist` — Build/install distribution bundles
- `./gradlew javadoc` — Generate API documentation

### Development Workflow
1. Implement changes in `src/main/java` (prefer matching existing package boundaries).
2. Add/adjust tests in `src/test/java` for behavior changes.
3. Run quick verification:
   - `./gradlew test`
   - `./gradlew checkstyleMain spotbugsMain`
4. Run full validation before commit:
   - `./gradlew clean build`
5. For UI Robot integration test (`RobotInputIntegrationTest`), enable explicitly in non-headless environments:
   - `./gradlew test -DuiTests=true`

## Configuration
- **Build & dependencies**: `build.gradle.kts`
  - `application.mainClass = "org.example.App"`
  - Java toolchain configured to version 25
  - Checkstyle + SpotBugs plugin setup
- **Gradle project identity**: `settings.gradle.kts`
- **Logging**: `src/main/resources/logback.xml` (console appender, INFO root level)
- **Code quality rules**:
  - `config/checkstyle/checkstyle.xml`
  - `config/spotbugs/spotbugs-exclude.xml`
- **Persistence location**:
  - `MapRepository` stores DB at `~/.hnsfgame/maps.db` by default
- **Runtime/system properties**:
  - `-DuiTests=true` enables Robot UI integration test path
- **Environment variables**:
  - No `.env`-style project configuration file detected

## Architecture
High-level flow:

1. **Entry Layer**: `org.example.App`
   - Starts game mode (`SwingGamePanel`) or editor mode (`EditorWindow`) based on args.

2. **Core Domain Layer**: `lib.game.GameWorld`
   - Owns world dimensions/background, object registry (`EntityManager`), physics (`PhysicsEngine`), and state machine reference.

3. **Object Model Layer**: `lib.object.*`
   - `GameObject` interface + `BaseObject`/`ActorObject` abstractions.
   - Concrete gameplay and UI objects (`PlayerObject`, `MonsterObject`, `SceneObject`, `WallObject`, `BoundaryObject`, `MenuObject`, `DialogObject`).

4. **Interaction Layer**: `lib.input.*` + `lib.state.*`
   - Input managers normalize keyboard/mouse state.
   - `InputActionMapper` maps controls to semantic actions.
   - `DefaultGameStateMachine` gates updates and interprets input by current state.

5. **Rendering Layer**: `lib.render.SwingGamePanel`
   - Swing timer-driven frame loop calls input processing, world update, and render.

6. **Persistence Layer**: `lib.persistence.*`
   - `MapDataMapper` + `GameObjectFactory` convert between runtime objects and DTOs.
   - `MapRepository` persists maps and objects into SQLite tables (`maps`, `map_objects`).

7. **Editor Layer**: `lib.editor.*`
   - Editor UI panel/window and controller for object placement, property editing, and undo/redo operations.

## Contributing
No dedicated `CONTRIBUTING.md` is present. Recommended contribution baseline:

- Follow existing package/module boundaries and naming conventions.
- Keep UI/gameplay behavior covered by tests where practical.
- Run `./gradlew test checkstyleMain spotbugsMain` before opening changes.
- Keep localization-sensitive strings (English/Chinese menu text support) compatible with existing logic.
- Avoid changing persistence schemas casually without migration planning.

## License
No `LICENSE` file and no explicit license metadata were found in the current project root. Treat license status as **unspecified** until project owners provide one.
