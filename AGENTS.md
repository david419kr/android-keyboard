# AGENTS.md

## Project Snapshot

This repository is FUTO Keyboard, an Android keyboard app forked from AOSP LatinIME. The app goal is an offline, privacy-preserving keyboard, so do not add network permissions, analytics, telemetry, or server-backed behavior unless the task explicitly asks for it and the flavor policy is checked.

The root `build.gradle` is the Android application project. `:voiceinput-shared` is the only included Gradle subproject. Main app code lives in `java/src`, shared LatinIME utilities live in `common/src`, Android instrumentation tests live in `tests/src`, and JNI/native code lives in `native/jni`.

## Required Setup

This repo depends on submodules. Run this before building or investigating missing assets/AARs:

```powershell
git submodule update --init --recursive
```

Build-affecting submodules include:

- `libs`: local AARs such as Mozc/Rime/VAD artifacts.
- `voiceinput-shared/src/main/ml`: bundled voice input model assets.
- `java/assets/layouts`: YAML keyboard layouts and mappings.
- `translations`: generated string resources used by Gradle.
- `java/assets/themes` and `java/res-large`: theme and large resource assets.

The CI image uses Gradle 8.14.3, JDK 21, Android SDK 35, build tools 35.0.0, and NDK `28.2.13676358`. The project uses Android Gradle Plugin `8.10.1`, Kotlin `2.1.0`, Java/Kotlin target 1.8, and Compose.

## Build And Verification

Use the Windows wrapper in this workspace:

```powershell
.\gradlew.bat assembleUnstableDebug
.\gradlew.bat assembleStableRelease
.\gradlew.bat bundlePlaystoreRelease
```

On this Windows machine, use these environment variables before Gradle builds:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\Android\Sdk"
$env:PYTHONUTF8="1"
.\gradlew.bat assembleUnstableDebug
```

`PYTHONUTF8=1` is needed because `updateLocales` reads UTF-8 locale JSON files; without it, Windows Python may default to `cp949` and fail with `UnicodeDecodeError`. The Android SDK path above was verified locally. Gradle installed or used SDK Platform 35, Build Tools 35.0.0, NDK `28.2.13676358`, and CMake 3.22.1 during the successful `assembleUnstableDebug` run.

If `voiceinput-shared/src/main/ml` hangs during submodule cloning, the required model can be placed manually:

```powershell
New-Item -ItemType Directory -Force voiceinput-shared\src\main\ml
Invoke-WebRequest `
  -Uri "https://gitlab.futo.org/keyboard/voice-input-models/-/raw/main/tiny_en_acft_q8_0.bin.not.tflite" `
  -OutFile "voiceinput-shared\src\main\ml\tiny_en_acft_q8_0.bin.not.tflite"
```

The expected file size observed locally is `43,550,795` bytes. This is enough for Gradle resource/model binding even if the Git submodule checkout remains partial.

Useful task-specific commands:

```powershell
.\gradlew.bat updateLocales
.\gradlew.bat updateBundleResources
.\gradlew.bat lintUnstableDebug
.\gradlew.bat connectedUnstableDebugAndroidTest
```

Notes:

- `connected...AndroidTest` requires a connected device or emulator.
- `updateLocales` runs `tools/make-keyboard-text-py/src/generate.py` and is already a `preBuild` dependency.
- `updateBundleResources` is used by the Play Store release path.
- Native unit test scripts under `native/jni/run-tests.sh` and `native/dicttoolkit/run_tests.sh` assume an AOSP tree with `mmm`; they are not normal Gradle wrapper tests.
- If validation is skipped because submodules, SDK, NDK, or a device are unavailable, say so explicitly in the final response.

## Source Layout

- `java/AndroidManifest.xml`: main manifest. Declares `.LatinIME` as the `BIND_INPUT_METHOD` service, `SettingsActivity`, import/payment/text edit activities, and content providers.
- `java/stable/AndroidManifest.xml`: stable/unstable additions for update checking.
- `java/playstore/AndroidManifest.xml`: Play Store flavor overlay.
- `build.gradle`: app configuration, flavors, source sets, signing, CMake, dependencies, generated translation filtering.
- `voiceinput-shared/build.gradle`: voice input shared library configuration.
- `java/res`: core resources, drawables, XML, strings, keyboard UI assets.
- `java/assets`: keyboard layouts, themes, fonts, spacing/punctuation, and other app assets.
- `dictionaries`: compressed wordlist/combined dictionary resources; inspect tooling and call sites before assuming they are packaged directly.
- `tools`: contributor, dicttool, keyboard text generation, and bundle resource scripts.

## Dictionary Binary Format Notes

- `dict/japanese.dict` and bundled Japanese `builtin_mozc_data.dict` are Mozc DataSet binaries, not LatinIME `main.dict` files or line-editable word lists. The first 4-byte magic is `EF 4D 4F 5A` (`0xef4d4f5a`, `\xEFMOZ`), followed by the rest of the Mozc signature such as `C\r\n`; `ImportResourceActivity.kt` detects this as a Japanese dictionary, and `JapaneseIME.kt` passes the path or APK asset offset/length directly to `MozcJNI.load(...)`. The container has aligned Mozc chunks such as `pos_matcher`, `conn`, `dict`, `sugg`, `symbol_*`, `emoji_*`, `zero_query_*`, `usage_*`, plus protobuf `DataSetMetadata`, footer metadata size, SHA-1, and file size. The `dict` chunk is itself a Mozc `DictionaryFile` with value trie, token array, key trie, and frequent POS sections. Do not treat it as an AOSP/FUTO `0x9BC13AFE` binary dictionary or combined word list.
- `dict/korean_emoji.dict` is the AOSP/FUTO LatinIME binary dictionary format. Magic is `9B C1 3A FE` (`0x9bc13afe`), format version `202`, header size `0x68`, and header attributes include `dictionary=emoji:ko`, `description=Emoji for Korean words`, `locale=ko`, and `version=19`. Body data starts after the header and uses the v2 Patricia trie layout in `FormatSpec.java`: PtNode arrays, compact code point encoding, terminal probabilities, child offsets, and shortcut lists. The inspected file has 2,763 node arrays, 11,871 nodes, and 10,792 terminal shortcut records mapping Korean/English trigger text to emoji and emoji glyphs back to Korean descriptions.

## Japanese Moaki Custom Layout Notes

The custom Japanese Moaki responsive layout is intentionally split across several files. Check all of these before changing it:

- `java/assets/layouts/Japanese/japanese_moaki_responsive.yaml`: phone/folded Moaki layout. It uses `imeHint: moaki`, is registered in `java/assets/layouts/mapping.yaml`, and currently has lower-case `f`/`w` labels, `w` tap output `ｗ`, `w` down-flick output `う`, and `ー` on the first-row sixth key. Multi-character flick outputs such as `ちゃ`, `ふぁ`, and `うぃ` are layout-level strings but need IME handling below.
- `java/src/org/futo/inputmethod/v2keyboard/ResponsiveLayoutResolver.kt`: resolves `japanese_moaki_responsive` to existing `japanese_qwerty` on large screens/unfolded foldables. Do not create a separate large-screen Moaki QWERTY file unless the task explicitly asks for it.
- `java/src/org/futo/inputmethod/engine/general/JapaneseIME.kt`: handles `JAPANESE_MOAKI_IME_HINT`, treats Moaki as Japanese 12-key-like Mozc input, whitelists multi-character Moaki strings in `MoakiMozcTextOutputs`, handles small-kana/dakuten/handakuten modifier keys, old middle-dot yoon conversion logic, full-width top number row for Moaki, and full-width alphabet/number/space behavior in Japanese QWERTY alphabet subkeyboard.
- `java/src/org/futo/inputmethod/keyboard/PointerTracker.java`: handles Moaki return-to-center flick behavior. Look at `mMoakiReturnFlickDirection`, `getMoakiYoonText(Key key, Key baseKey)`, and the `w`-specific branch: normal `w` down-flick is `う`, but return-to-center from that flick is `ヴ`; `w` right/left/up/up-right return-to-center outputs are `ヴぁ`/`ヴぇ`/`ヴぉ`/`ヴぃ`.

## Main Runtime Architecture

The high-level input flow is:

1. `java/src/org/futo/inputmethod/latin/LatinIME.kt`
2. `LatinIMELegacy.java`
3. `IMEManager.kt`
4. active `IMEInterface` implementation
5. `InputLogic`, dictionaries, language model, and keyboard UI

Key classes:

- `LatinIME.kt`: top-level `InputMethodService`. It adds Compose lifecycle support, creates the Compose input view, embeds the legacy `InputView`, owns `IMEManager`, `UixManager`, theme state, keyboard sizing, and routes Android IME lifecycle callbacks.
- `LatinIMELegacy.java`: AOSP LatinIME-style service core. It owns `Settings`, `RichInputMethodManager`, `KeyboardSwitcher`, feedback managers, receivers, input-view setup, settings reloads, subtype changes, and legacy keyboard lifecycle.
- `IMEManager.kt`: selects the active IME by locale. `zh` routes to `ChineseIME`, `ja` to `JapaneseIME`, and everything else to `GeneralIME`. It also manages action-input transactions and delayed selection updates.
- `IMEInterface.kt` and `IMEHelper.kt`: the contract and helper bridge for all IME engines. New engine behavior should normally go through this interface rather than reaching around it.
- `GeneralIME.kt`: Latin/general engine. It combines `InputLogic`, `DictionaryFacilitator`, `SuggestionBlacklist`, and `LanguageModelFacilitator`.
- `ChineseIME.kt` and `JapaneseIME.kt`: specialized engines for those locales.
- `KeyboardSwitcher.java`: singleton that creates and reloads the legacy keyboard view, keyboard state, themes, and `KeyboardLayoutSetV2`.
- `v2keyboard/*`: YAML-backed layout system, keyboard sizing, key data, more-key handling, and layout parsing. Call `LayoutManager.init(context)` before querying layouts.
- `UixManager.kt`: Compose keyboard shell, action bar, suggestions UI, notices, action windows, quick clips, preedit UI, floating/split/one-handed keyboard windows, and Compose locals.
- `java/src/org/futo/inputmethod/latin/uix/settings/*`: Compose settings UI and hooks.
- `voiceinput-shared/src/main/java/org/futo/voiceinput/shared/*`: shared voice input UI/model runner code.
- `native/jni/*`: JNI bridge plus C++ dictionary, suggestion, GGML, Whisper, and transformer language model code.

## Settings And State

There are two settings systems:

- Legacy `SharedPreferences`: `java/src/org/futo/inputmethod/latin/settings/Settings.java`, `SettingsValues.java`, and settings constants such as `Settings.PREF_*`.
- Jetpack DataStore: `java/src/org/futo/inputmethod/latin/uix/Settings.kt` with typed `SettingsKey<T>`, plus Compose helpers in `uix/settings/Hooks.kt`.

Use the store that existing nearby code uses. Some Compose settings mirror DataStore values into legacy `SharedPreferences` with `SyncDataStoreToPreferences*`; preserve that bridge when changing behavior consumed by `SettingsValues`.

Direct Boot matters. `Context.dataStore` can return a locked fallback store before first unlock, and `PreferenceUtils.getDefaultSharedPreferences(context)` switches to device-protected storage when needed. Check `Context.isDirectBootUnlocked` or `forceUnlockDatastore(context)` before adding code that needs unlocked user data.

## Native Code

The Gradle app builds `native/jni/CMakeLists.txt`, which includes `NativeFileList.cmake`. When adding or removing C++ files, update `NativeFileList.cmake`; source discovery is intentionally manual to avoid compiling unwanted third-party files.

JNI entry points include:

- `org_futo_inputmethod_keyboard_ProximityInfo.cpp`
- `org_futo_inputmethod_latin_BinaryDictionary*.cpp`
- `org_futo_inputmethod_latin_DicTraverseSession.cpp`
- `org_futo_inputmethod_latin_xlm_*.cpp`
- `org_futo_voiceinput_WhisperGGML.cpp`

Native code uses C++17 and strict warning flags. Keep changes local and avoid broad third-party churn under `native/jni/src/third_party`.

## Flavor And Permission Rules

Flavors:

- `unstable`: application id suffix `.unstable`, update checking enabled, default dev flavor.
- `stable`: standalone release flavor.
- `playstore`: application id suffix `.playstore`, Play Store payment price/build flags, update checking disabled.

The stable CI release job fails if `android.permission.INTERNET` appears in stable or Play Store packaged manifests. Check manifest overlays and dependency manifest merges before adding permissions.

## Working Guidelines

- Start with `git status --short`; preserve unrelated user changes.
- Prefer `rg` / `rg --files` for search.
- Keep edits scoped to the layer being changed. IME lifecycle, settings reloads, keyboard sizing, and suggestion updates are tightly coupled.
- Avoid blocking the input path. Use existing `lifecycleScope`, `Dispatchers.IO`, or `Dispatchers.Default` patterns for disk/model work.
- For UI changes, follow existing Compose components in `uix/settings/Components.kt` and existing keyboard shell patterns in `UixManager.kt`.
- For keyboard layouts, edit YAML assets in `java/assets/layouts` or custom-layout code in `v2keyboard`/settings only after submodules are present.
- For action bar/actions, register or update actions through `uix/actions/Registry.kt` and nearby action classes.
- For model or prediction changes, inspect `LanguageModelFacilitator.kt`, `LanguageModel.kt`, `TrainingWorker.kt`, and JNI xlm files together.
- For dictionary/autocorrect changes, inspect `InputLogic.java`, `DictionaryFacilitator*`, `Suggest*`, `SuggestedWords*`, and native dictionary/suggest code as a unit.
- Do not reformat whole AOSP-derived Java files. Preserve local style and license headers.

## Testing Focus By Change Type

- Input logic, autocorrect, suggestions: run or update tests under `tests/src/org/futo/inputmethod/latin`, plus a variant build.
- Keyboard layouts/sizing: test `v2keyboard` paths, `KeyboardSwitcher`, and at least `assembleUnstableDebug`.
- Compose settings/UI: build the affected flavor and manually inspect on device/emulator when possible.
- Native dictionary/JNI changes: run a Gradle assemble for ABI build coverage; use AOSP native test scripts only in an AOSP environment.
- Manifest/flavor changes: inspect merged manifests for stable/playstore and verify no accidental `INTERNET` permission.
