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

**Full build** of this repo needs **JDK 17**, the **Android SDK**, and **CMake 3.31.1** (and NDK as used by AGP) so the `:shell` native code can compile. Install CMake from Android Studio (SDK Manager → SDK Tools → CMake 3.31.1) or:

```shell
sdkmanager "cmake;3.31.1"
```

If you see `[CXX1300] CMake '3.31.1' was not found`, install that CMake package (and ensure `ANDROID_HOME` points at your SDK).

**Without** CMake/NDK you can still build the Java CLI and the Gradle plugin (tests only):

```shell
./gradlew :dpt:build :dpt-gradle-plugin:build -Pdpt.plugin.minimalRuntime
```

That embeds only `dpt.jar` inside the plugin (no `shell-files`); use a normal full build before publishing or running real protection.

**CI** (`.github/workflows`) runs **`./gradlew build`** with CMake/ninja and **never** passes `minimalRuntime`, so `:dpt-gradle-plugin` tests and the packaged plugin JAR both use the **full** `dpt-runtime.zip` (`dpt.jar` + `shell-files`), same as a real release.

```shell
git clone --recursive https://github.com/luoyesiqiu/dpt-shell
cd dpt-shell
./gradlew assemble
cd executable
java -jar dpt.jar -f /path/to/android-package-file
```

### Gradle plugin (JitPack)

Published from [codewithtamim/dpt-shell](https://github.com/codewithtamim/dpt-shell) via [JitPack](https://jitpack.io/#codewithtamim/dpt-shell). Prefer a **release tag** as the plugin version (for example the project’s `versionName` tag like `2.10.0` or `v2.10.0`, depending on how you tag).

#### How JitPack builds it (full runtime, not `minimalRuntime`)

[JitPack runs `jitpack.yml`](jitpack.yml), which installs the Android SDK pieces needed for **`:shell:assembleRelease`**, then runs **`./gradlew :dpt-gradle-plugin:build`** with **no** `-Pdpt.plugin.minimalRuntime`. That is the same wiring as a local full build: `:dpt:jar` produces `executable/dpt.jar`, the shell module fills `executable/shell-files/`, and the plugin task **`zipDptRuntime`** packs both into `dpt-runtime.zip` inside the published plugin JAR. Consumers still run **`java -jar dpt.jar …`** under the hood (same as the command line); JitPack is building those artifacts for you, not replacing them with a different code path.

**Why not “only call `dpt` Java APIs” on JitPack?** The protector needs **`shell-files`** (the shell `classes.dex` and native `.so` libraries) on disk at runtime. Whether you invoke `Dpt.main` in-process or via CLI, those binaries must come from **building `:shell`** (or from shipping a prebuilt bundle). Calling APIs alone does not remove that dependency; embedding the result of `:shell:assembleRelease` keeps one self-contained Maven coordinate.

**`-Pdpt.plugin.minimalRuntime`** is only a **local developer escape hatch** when your machine cannot compile native code (no CMake/NDK). Do **not** use it for JitPack or release builds; the artifact would be missing `shell-files` and could not protect APKs for real.

**Maven coordinates:** `com.github.codewithtamim:dpt-gradle-plugin:<version>`  
**Plugin id:** `com.github.codewithtamim.dpt`

**`settings.gradle.kts`**

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

**App module `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("com.github.codewithtamim.dpt") version "<tag>"
}

dpt {
    enabled.set(true)
    applyToRelease.set(true)   // default: run after packageRelease (and other non-debug variants)
    applyToDebug.set(false)    // set true to protect debug APKs too

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

**Groovy**

```gradle
plugins {
    id 'com.android.application'
    id 'com.github.codewithtamim.dpt' version '<tag>'
}

dpt {
    enabled = true
    applyToRelease = true
    protectConfig = file("${rootProject.projectDir}/dpt-protect.json")
}
```

**Legacy `buildscript`**

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.codewithtamim:dpt-gradle-plugin:<tag>'
    }
}
apply plugin: 'com.android.application'
apply plugin: 'com.github.codewithtamim.dpt'
```

#### Behavior

- When `enabled` is true, each Android variant that passes the `applyToRelease` / `applyToDebug` filters registers a task `dptProtect<VariantName>` (for example `dptProtectRelease`). The corresponding **`package*`** task is **`finalizedBy`** that task, so the APK on disk matches what `dpt` reads (after packaging/signing steps for that variant).
- **`dptVersion`** — runs the bundled `dpt.jar --version` (helpful to confirm what JitPack or your build embedded).

#### Option reference (CLI parity)

Values set in the `dpt { }` block are defaults. **Project properties** `-Pdpt.<name>=...` override the DSL for that build (booleans: `true` / `false`; paths: absolute or project-relative strings). File options in Gradle use `file(...)`; on the command line use a filesystem path string.

| `dpt` CLI | Kotlin DSL | `-P` property | Default |
|-----------|------------|---------------|---------|
| *(plugin only)* | `enabled` | `dpt.enabled` | `false` |
| *(plugin only)* | `applyToRelease` | `dpt.applyToRelease` | `true` |
| *(plugin only)* | `applyToDebug` | `dpt.applyToDebug` | `false` |
| `-c` / `--protect-config` | `protectConfig` | `dpt.protectConfig` | unset |
| `--debug` | `debuggable` | `dpt.debuggable` | `false` |
| `--disable-acf` | `disableAppComponentFactory` | `dpt.disableAppComponentFactory` | `false` |
| `--dump-code` | `dumpCode` | `dpt.dumpCode` | `false` |
| `-e` / `--exclude-abi` | `excludeAbi` | `dpt.excludeAbi` | `""` |
| `-f` / `--package-file` | *(automatic)* | — | taken from the variant output APK |
| `-K` / `--keep-classes` | `keepClasses` | `dpt.keepClasses` | `false` |
| `--noisy-log` | `noisyLog` | `dpt.noisyLog` | `false` |
| `-o` / `--output` | `outputDirectory` | `dpt.output` | `build/outputs/dpt/<variantName>` under the module |
| `-r` / `--rules-file` | `rulesFile` | `dpt.rulesFile` | unset |
| `-S` / `--smaller` | `smaller` | `dpt.smaller` | `false` |
| `-vs` / `--verify-sign` | `verifySign` | `dpt.verifySign` | `false` |
| `-x` / `--no-sign` | `noSign` | `dpt.noSign` | `false` |
| `-v` / `--version` | use task `./gradlew dptVersion` | — | — |

**Example (command line overrides)**

```shell
./gradlew :app:assembleRelease \
  -Pdpt.enabled=true \
  -Pdpt.verifySign=true \
  -Pdpt.protectConfig=/absolute/path/protect.json \
  -Pdpt.output=/absolute/path/out
```

When **building this repo locally**, the plugin uses the same packaging as JitPack unless you pass **`-Pdpt.plugin.minimalRuntime`** (embeds only `dpt.jar`; for machines without CMake/NDK, not suitable for real protection).

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
