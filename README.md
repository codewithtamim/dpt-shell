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

```shell
git clone --recursive https://github.com/luoyesiqiu/dpt-shell
cd dpt-shell
./gradlew assemble
cd executable
java -jar dpt.jar -f /path/to/android-package-file
```

### Gradle plugin (JitPack)

Use [JitPack](https://jitpack.io/#codewithtamim/dpt-shell) with the **app** module. In the root `settings.gradle.kts`, put JitPack in **pluginManagement** so `plugins { id("com.github.codewithtamim.dpt-shell") }` can resolve (adding JitPack only under `dependencyResolutionManagement` or a module `repositories` block is not enough for plugin resolution):

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

In the app module, apply the Android plugin and this plugin (replace `<tag>` with a release tag or commit):

```kotlin
plugins {
    id("com.android.application")
    id("com.github.codewithtamim.dpt-shell") version "<tag>"
}

dpt {
    enabled.set(true)
    applyToRelease.set(true)
    applyToDebug.set(false)
    applyToBundle.set(true)

    protect {
        shellPackageName.set("<random>") // omit to default to <random> when any protect field is set
        // appSignSha256.set("…") and dexSign.set("…") optional
        signature {
            keystore.set(file("${rootProject.projectDir}/release.jks"))
            alias.set("key0")
            storePassword.set("***")
            keyPassword.set("***")
        }
    }
    // Optional: protectConfig.set(file(".../dpt-protect.json")) or -Pdpt.protectConfig=... overrides protect { }.

    rulesFile.set(file("${rootProject.projectDir}/keep.rules"))
    outputDirectory.set(layout.buildDirectory.dir("outputs/dpt"))
    // Default: also copy protected *_signed.apk / *_signed.aab under <rootProject>/build/dpt/<variant>/ (handy after Android Studio → Generate Signed APK / App Bundle).
    // collectProtectedToRoot.set(false)
    // collectOutputDirectory.set(rootProject.layout.projectDirectory.dir("build/dpt"))
    excludeAbi.set("armeabi-v7a,arm64-v8a,x86,x86_64")

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

APK builds run `dptProtect<Variant>` as a **finalizer of `assemble<Variant>`** (not `package*`), so signing finishes first and **Build → Generate Signed App Bundle or APK** in Android Studio still runs protection. Protected artifacts are written under `dpt.output` / `build/outputs/dpt/<variant>` (see `dpt { outputDirectory ... }`); the original APK in `build/outputs/apk/` is not replaced. By default, the plugin also **copies** each final `*_signed.apk` / `*_signed.aab` into `<rootProject>/build/dpt/<variant>/` so outputs are easy to find next to the repo root (`dpt { collectProtectedToRoot … }`, `collectOutputDirectory`; disable with `-Pdpt.collectToRoot=false`, custom parent dir with `-Pdpt.collectOutput=/path`). App Bundle builds use `dptProtectBundle<Variant>` after `bundle*` (AGP 8+). `./gradlew dptVersion` prints the bundled `dpt.jar` version. Override options with `-Pdpt.<name>=...`.

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