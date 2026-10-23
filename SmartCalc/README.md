# SmartCalc — Stage 1

Contents: Gradle skeleton, pure-Kotlin calculation engine (`:engine`), minimal Compose screen (`:app`).

## Build
1. Open the `SmartCalc` folder in Android Studio (Ladybug or newer, JDK 17). Let it sync; it creates the Gradle wrapper.
2. Engine tests: `./gradlew :engine:test` (or run `CalculatorTest` from the IDE).
3. Run on a device/emulator: Run > `app`. APK: Build > Build APK(s), or `./gradlew :app:assembleDebug`
   (output: `app/build/outputs/apk/debug/`).

If sync complains about versions, accept Android Studio's suggested AGP/Kotlin updates (keep the Kotlin
and `kotlin.plugin.compose` versions identical) and send me the error text if anything remains.

## Engine behavior (decisions to review)
- `%` is a postfix operator meaning /100: `200*10%` = 20, `5+10%` = 5.1 (phone calculators often give 5.5).
- `-2^2` = -4; `2^3^2` = 512 (right-associative).
- Implicit multiplication: `2(3+4)`, `2x`, `2pi`. Two bare numbers (`2 3`) are a syntax error.
- DEG is the default angle mode; `sin(180)` returns exactly 0 (noise below 1e-14 is snapped).
- Results are rounded to 12 significant digits for display; values stay full `Double` internally.
- Accepts Persian/Arabic digits and `× ÷ − π √`. Input limit: 1000 chars; nesting limit ~200 parentheses.
- Error messages are English strings for now; localization comes with the UI stage.

## Status
- Written but NOT yet compiled or run by me (no Kotlin compiler/network in my environment). Expect possible small compile errors on first build.
- Not started: graphs, history/Room, appearance/themes, binary animation, glow animation, camera scan, tablet layout, settings/privacy.

## Build without Android Studio (GitHub Actions)
Push this folder to a GitHub repository. The workflow in `.github/workflows/build.yml` runs the engine
tests and builds the debug APK. Download it from the run's "Artifacts" section (`SmartCalc-debug-apk`).
