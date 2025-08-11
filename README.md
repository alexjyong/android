# Revolt on Android


## Description

NOTE: This is a forked version of the Android app for the [Revolt](https://revolt.chat) chat platform.  I made for some QOL changes that aren't present in the current version at the time of writing.

Feel free to use this for whatever, but note that this is NOT the official Revolt android app. :)

## Features Added
<table>
  <tr>
    <td width="50%" valign="top">
      <strong>Emojis next to channel names</strong><br>
      <img src="https://github.com/user-attachments/assets/6c37f059-47ef-4028-aac9-ef233f7a85e4" width="300" alt="Channel Emojis">
    </td>
    <td width="50%" valign="top">
      <strong>Jump to replied message</strong><br>
      <img src="https://github.com/user-attachments/assets/1dc0fb6b-521d-40e2-a833-1ada19bd207b" width="220" alt="Jump to Reply">
    </td>
  </tr>
  <tr>
    <td colspan="2" align="center" valign="top">
      <strong>Server context menu on long press</strong><br>
      <img src="https://github.com/user-attachments/assets/29270bb0-1bf6-4a00-90f8-ff0168f62cf4" width="300" alt="Server Context Menu">
    </td>
  </tr>
</table>



The codebase includes the app itself, as well as an internal library for interacting with the Revolt
API.

| Module | Package       | Description          |
|--------|---------------|----------------------|
| `:app` | `chat.revolt` | The main app module. |

The API library is part of the `app` module, and is not intended to be used as a standalone library,
as it makes liberal use of Android-specific APIs for reactivity.

The app is written in Kotlin, and uses
the [Jetpack Compose](https://developer.android.com/jetpack/compose) UI toolkit, the current state
of the art for Android UI development.

## Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
    - For some Material components, the View-based
      [Material Components Android](https://github.com/material-components/material-components-android)
      (MDC-Android) library is used.
- [Ktor](https://ktor.io/)
- [Dagger](https://dagger.dev/) with [Hilt](https://dagger.dev/hilt/)

## Resources

### Revolt on Android

- [Revolt on Android Technical Documentation](https://revoltchat.github.io/android/)
- [Android-specific Contribution Guide](https://revoltchat.github.io/android/contributing/guidelines/)
  &mdash;**read carefully before contributing!**

### Revolt

- [Revolt Project Board](https://github.com/revoltchat/revolt/discussions) (Submit feature requests
  here)
- [Revolt Testers Server](https://app.revolt.chat/invite/Testers)
- [General Revolt Contribution Guide](https://developers.revolt.chat/contributing)

## Quick Start

Open the project in Android Studio. You can then run the app on an emulator or a physical device by
running the `app` module.

In-depth setup instructions can be found
at [Setting up your Development Environment](https://revoltchat.github.io/android/contributing/setup/)
