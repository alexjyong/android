<div align="center">
    <h1>Revolt for Android</h1>
    <p>Official <a href="https://revolt.chat">Revolt</a> Android app.</p>
    <br/><br/>
    <div>
        <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width="200">
        <br/>
    </div>
    <small>Google Play is a trademark of Google LLC.</small>
    <br/><br/><br/>
</div>

## Description

The codebase includes the app itself, as well as an internal library for interacting with the Revolt
API. The app is written in Kotlin, and wholly
uses [Jetpack Compose](https://developer.android.com/jetpack/compose).

## Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
    - For some Material components, the View-based
      [Material Components Android](https://github.com/material-components/material-components-android)
      (MDC-Android) library is used.
- [Ktor](https://ktor.io/)
- [Dagger](https://dagger.dev/) with [Hilt](https://dagger.dev/hilt/)

## Resources

### Revolt for Android

- [Roadmap](https://op.revolt.wtf/projects/revolt-for-android/work_packages)
- [Revolt for Android Technical Documentation](https://revoltchat.github.io/android/)
- [Android-specific Contribution Guide](https://revoltchat.github.io/android/contributing/guidelines/)
  &mdash;**read carefully before contributing!**

### Revolt

- [Revolt Project Board](https://github.com/revoltchat/revolt/discussions) (Submit feature requests
  here)
- [Revolt Development Server](https://app.revolt.chat/invite/API)
- [Revolt Server](https://app.revolt.chat/invite/Testers)
- [General Revolt Contribution Guide](https://developers.revolt.chat/contrib.html)

## Quick Start

Open the project in Android Studio. You can then run the app on an emulator or a physical device by
running the `app` module.

In-depth setup instructions can be found
at [Setting up your Development Environment](https://revoltchat.github.io/android/contributing/setup/)
