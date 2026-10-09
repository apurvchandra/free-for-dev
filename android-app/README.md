# Wordstep

Wordstep is a local-first Android vocabulary app for conversational English practice. Its learning flow works offline and does not require an account.

## Build

Use Android SDK Platform 36 and Java 17, then run:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The signed debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Debug signing is intended for private testing; keep the generated debug keystore if you want to install updates over an existing copy. Do not commit signing keys.

To install from a connected device with Android Platform Tools:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android may ask the device owner to allow installation from the file-sharing app.

## On-device prompts

Gemini Nano is optional. Wordstep checks ML Kit's runtime availability and keeps curated prompts available when the device cannot use Nano. The app does not record spoken practice or send learner data to a backend. A Nano model may need to be downloaded by Android before its first use.