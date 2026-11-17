# VORION AI

VORION AI is an independent, extensible intelligent-assistant project.

## Initial milestone — 0.1.0

This repository starts with an Android-native foundation and a reusable holographic presence component. The first milestone focuses on the presentation layer and explicit assistant states; it does not yet provide wake-word detection, system-wide device control, a backend AI model, or background listening.

## Android app

- Kotlin
- Jetpack Compose
- Material 3
- Minimum Android version: API 26

Open the repository in Android Studio and sync the Gradle project.

## Hologram states

`IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, `EXECUTING`, `SUCCESS`, `WARNING`, and `ERROR`.

The UI component accepts an `audioLevel` value so a future audio pipeline can drive the visual response. No microphone access is performed by the component itself.

## Planned architecture

1. Android client and holographic UI.
2. Assistant orchestration and tool-permission layer.
3. Secure backend/API integration.
4. Optional memory, voice, vision, and device integrations.

Sensitive actions must require explicit user confirmation. Platform permissions and Android restrictions apply.

## Status

Foundation scaffold only. Build verification and device testing are still required before calling this a release.