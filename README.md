# dpt-shell

[![](https://img.shields.io/github/license/luoyesiqiu/dpt-shell)](https://github.com/luoyesiqiu/dpt-shell/blob/main/LICENSE) [![](https://img.shields.io/github/downloads/luoyesiqiu/dpt-shell/total?color=blue)](https://github.com/luoyesiqiu/dpt-shell/releases/latest) [![](https://img.shields.io/github/issues-raw/luoyesiqiu/dpt-shell?color=red)](https://github.com/luoyesiqiu/dpt-shell/issues) ![](https://img.shields.io/badge/Android-5.0%2B-brightgreen)

English | [简体中文](./README.zh-CN.md) 

dpt-shell is an Android Dex protection shell that hollows out Dex method implementations and reconstructs them at runtime.

## Usage

### Quick uses

Go to [Releases](https://github.com/luoyesiqiu/dpt-shell/releases/latest) download `executable.zip` and unzip it, run the follow command lines in terminal: 

```shell
java -jar dpt.jar -f /path/to/android-package-file
```

### Manual builds

A full build of this repo needs JDK 17, the Android SDK, CMake 3.31.1, and the NDK version your AGP expects, so the `:shell` native code can compile. Install CMake from Android Studio (SDK Manager, SDK Tools, CMake 3.31.1) or run:

```shell
sdkmanager "cmake;3.31.1"
```

If Gradle reports `CMake '3.31.1' was not found`, install that package and check that `ANDROID_HOME` points at the right SDK.

Building `:dpt-gradle-plugin` requires a full native toolchain: it packs `dpt.jar` and `shell-files` from `:shell:assembleRelease` (or an existing `executable/shell-files` tree). Use the published JitPack artifact if you only need the plugin without compiling this repo.

```shell
git clone --recursive https://github.com/luoyesiqiu/dpt-shell
cd dpt-shell
./gradlew assemble
cd executable
java -jar dpt.jar -f /path/to/android-package-file
```

### Gradle plugin (JitPack)

The Android Gradle plugin is published from [codewithtamim/dpt-shell](https://github.com/codewithtamim/dpt-shell) via [JitPack](https://jitpack.io/#codewithtamim/dpt-shell). Prefer a stable Git tag as the version string when you depend on it.

JitPack uses [jitpack.yml](jitpack.yml): it downloads CMake 3.31.1 and ninja (the default image cannot use `apt`/`sdkmanager` reliably), writes `cmake.dir` into `local.properties`, initializes git submodules, then runs `./gradlew :dpt-gradle-plugin:build`. Android Gradle Plugin still pulls the SDK platform, build-tools, and NDK as needed. Enable recursive submodules for the repo on jitpack.io if builds complain about missing native sources. The published plugin matches a full local build (`dpt.jar` plus `shell-files`) and runs `java -jar dpt.jar` like the CLI.

JitPack uses Maven group `com.github.codewithtamim.dpt-shell` (GitHub user plus repo name). The published plugin id is chosen to match that: `com.github.codewithtamim.dpt-shell`. Gradle then publishes a marker `com.github.codewithtamim.dpt-shell:com.github.codewithtamim.dpt-shell.gradle.plugin` alongside the jar module `dpt-gradle-plugin`. See the [builds API](https://jitpack.io/api/builds/com.github.codewithtamim/dpt-shell/latest) for module names on a given tag.

Use `id("com.github.codewithtamim.dpt-shell")` in `plugins { }` — not the colon form from JitPack’s “implementation” snippet. Do not use `implementation` for this; it is a plugin.

settings.gradle.kts:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

**If Gradle reports the plugin was not found and lists only** Gradle Central Plugin Repository, Google, and Maven Central **(JitPack is not in that list)**, then JitPack is not registered for **plugin** resolution. Adding `maven("https://jitpack.io")` only under `repositories { }` in a module `build.gradle.kts` or only under `dependencyResolutionManagement` does **not** apply to `plugins { id(...) }`. It must appear inside **`pluginManagement { repositories { ... } }`** in the **root** `settings.gradle.kts`, as above.

App module build.gradle.kts:

```kotlin
plugins {
    id("com.android.application")
    id("com.github.codewithtamim.dpt-shell") version "<tag>"
}

dpt {
    enabled.set(true)
    applyToRelease.set(true)
    applyToDebug.set(false)

    protectConfig.set(file("${rootProject.projectDir}/dpt-protect.json"))
    rulesFile.set(file("${rootProject.projectDir}/keep.rules"))
    outputDirectory.set(layout.buildDirectory.dir("outputs/dpt"))
    excludeAbi.set("x86,x86_64")

    verifySign.set(false)
    noSign.set(false)
    debuggable.set(false)
    disableAppComponentFactory.set(false)
    dumpCode.set(false)
    keepClasses.set(false)
    noisyLog.set(false)
    smaller.set(false)
}
```

Groovy:

```gradle
plugins {
    id 'com.android.application'
    id 'com.github.codewithtamim.dpt-shell' version '<tag>'
}

dpt {
    enabled = true
    applyToRelease = true
    protectConfig = file("${rootProject.projectDir}/dpt-protect.json")
}
```

Legacy buildscript (JitPack group includes the repo name):

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.codewithtamim.dpt-shell:dpt-gradle-plugin:<tag>'
    }
}
apply plugin: 'com.android.application'
apply plugin: 'com.github.codewithtamim.dpt-shell'
```

When `enabled` is true, each variant that matches `applyToRelease` or `applyToDebug` gets a task named `dptProtect` plus the variant name (for example `dptProtectRelease`). The matching `package` task for that variant is finalized by it so `dpt` sees the same APK that packaging produced. Run `./gradlew dptVersion` to print the bundled `dpt.jar` version.

DSL values in `dpt { }` are defaults. You can override them per invocation with project properties `-Pdpt.<name>=...` (booleans as true/false, paths as strings). Gradle file properties use `file(...)`; `-P` uses a path string.

Plugin-only options: `enabled` / `dpt.enabled` (default false), `applyToRelease` / `dpt.applyToRelease` (default true), `applyToDebug` / `dpt.applyToDebug` (default false).

CLI-aligned options (Kotlin name, then `-P` key, then default): `protectConfig` / `dpt.protectConfig` (unset), `debuggable` / `dpt.debuggable` (false), `disableAppComponentFactory` / `dpt.disableAppComponentFactory` (false), `dumpCode` / `dpt.dumpCode` (false), `excludeAbi` / `dpt.excludeAbi` (empty), `keepClasses` / `dpt.keepClasses` (false), `noisyLog` / `dpt.noisyLog` (false), `outputDirectory` or `dpt.output` (defaults to `build/outputs/dpt/<variant>` under the module), `rulesFile` / `dpt.rulesFile` (unset), `smaller` / `dpt.smaller` (false), `verifySign` / `dpt.verifySign` (false), `noSign` / `dpt.noSign` (false). The input APK is always the variant output; there is no DSL field for `-f`. For `--version` use the `dptVersion` task.

Example:

```shell
./gradlew :app:assembleRelease \
  -Pdpt.enabled=true \
  -Pdpt.verifySign=true \
  -Pdpt.protectConfig=/absolute/path/protect.json \
  -Pdpt.output=/absolute/path/out
```

### Command line options

```text
usage: java -jar dpt.jar [option] -f <package_file>
 -c,--protect-config <arg>   Protect config file.
                             
    --debug                  Make package debuggable.
    --disable-acf            Disable app component factory(just use for
                             debug).
    --dump-code              Dump the code item of DEX and save it to
                             .json files.
 -e,--exclude-abi <arg>      Exclude specific ABIs (comma separated, e.g.
                             x86,x86_64).
                             Supported ABIs:
                             - arm       (armeabi-v7a)
                             - arm64     (arm64-v8a)
                             - x86
                             - x86_64
 -f,--package-file <arg>     Need to protect android package(*.apk, *.aab)
                             file.
 -K,--keep-classes           Keeping some classes in the package can
                             improve the app's startup speed to a certain
                             extent, but it is not supported by some
                             application packages.
    --noisy-log              Open noisy log.
 -o,--output <arg>           Output directory for protected package.
 -r,--rules-file <arg>       Rules file for class names that will not be
                             protected.
 -S,--smaller                Trade some of the app's performance for a
                             smaller app size.
 -v,--version                Show program's version number.
 -vs,--verify-sign           Enable runtime app signature verification.
                             The certificate SHA-256 is computed
                             automatically from the signing keystore.
 -x,--no-sign                Do not sign package.
```

## Notice

This project has not too many tests, be careful use in prod environment. Otherwise, all consequences are at your own risk.

## Dependency or use follows project code

- [dx](https://android.googlesource.com/platform/dalvik/+/refs/heads/master/dx/)
- [Dobby](https://github.com/jmpews/Dobby)
- ~~[libzip-android](https://github.com/julienr/libzip-android)~~
- [ManifestEditor](https://github.com/WindySha/ManifestEditor)
- ~~[Xpatch](https://github.com/WindySha/Xpatch)~~
- [bhook](https://github.com/bytedance/bhook)
- [zipalign-java](https://github.com/Iyxan23/zipalign-java)
- [minizip-ng](https://github.com/zlib-ng/minizip-ng)
- [JSON-java](https://github.com/stleary/JSON-java)
- [zip4j](https://github.com/srikanth-lingala/zip4j)
- [commons-cli](https://github.com/apache/commons-cli)
- [dexmaker](https://android.googlesource.com/platform/external/dexmaker)
- [Obfuscate](https://github.com/adamyaxley/Obfuscate)

