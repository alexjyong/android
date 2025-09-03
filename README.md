# Revolt for Android (Forked Version!)

<div align="center">
    <h1>Revolt for Android (Forked Version!)</h1>
    <p>Forked Version of the <a href="https://revolt.chat">Revolt</a> Android app.</p>
    <br/><br/>
    <div>
        <img width="192" height="192" alt="image" src="https://github.com/user-attachments/assets/ec2986a6-115d-4f66-8b17-c76cff10ead7" />
        <br/>
    </div>
    <br/><br/><br/>
</div>




## Description

NOTE: This is a forked version of the Android app for the [Revolt](https://revolt.chat) chat platform.
I am not affilated with the Revolt Team, nor is this an official Revolt product.

I made for some QOL changes that aren't present in the current version at the time of writing.

Feel free to use this for whatever, but note that this is NOT the official Revolt android app. :)

You can download the latest APK [here](https://github.com/alexjyong/android/releases/latest).

## Features Added
<table>
  <tr>
    <td width="50%" valign="top" align="center">
      <strong>Emojis next to channel names</strong><br>
      <img src="https://github.com/user-attachments/assets/6c37f059-47ef-4028-aac9-ef233f7a85e4" width="300" alt="Channel Emojis">
    </td>
    <td width="50%" valign="top" align="center">
      <strong>Jump to replied message</strong><br>
      <img src="https://github.com/user-attachments/assets/1dc0fb6b-521d-40e2-a833-1ada19bd207b" width="300" alt="Jump to Reply">
    </td>
  </tr>
  <tr>
    <td colspan="2" align="center" valign="top">
      <strong>Server context menu on long press</strong><br>
      <img src="https://github.com/user-attachments/assets/1450ea4c-a283-4ac3-8a0a-8e9f1adb436b" width="300" alt="Server Context Menu">
    </td>
  </tr>
  <tr>
    <td colspan="2" align="center" valign="top">
      <strong>Recently used emojis</strong><br>
      <img src="https://github.com/user-attachments/assets/6f1e126a-8e73-49ee-a5d1-c4841907e14a" width="600" alt="Recently Used Emojis">
    </td>
  </tr>
</table>




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

## Quick Build

If you don't want to download the apks in the releases section and rather build yourself, follow these steps:

Fire up a Github Codespaces instance at this link [here](https://github.com/codespaces/new?hide_repo_select=true&ref=combined-pr&repo=1020437871&skip_quickstart=true&machine=premiumLinux&devcontainer_path=.devcontainer%2Fdevcontainer.json&geo=UsEast)

The URL should have it selected for you automatically, but be sure to use this branch for your instance!

Note that this url will have an 8-core instance selected by default. Feel free to use a smaller instance, but I've ran into build errors with that.
At the time of writing, Github offers a number of free hours for personal accounts, but note that this bigger instance will use more of your free hours than a smaller one. **For just building the apk and downloading it to whatever device, this should be fine though**. Be sure to delete the instance when you are done. **It won't cost you $$ if you don't have payment set up with Github or have budget limits.** [See the billing page for more details.](https://docs.github.com/en/billing/concepts/product-billing/github-codespaces#pricing-for-paid-usage)

After the instance fires up run

```sh
./gradlew assembledebug --no-daemon
``` 
To generate a debug version of the application. 

If you wanted a signed copy that isn't in debug mode, set up a release-key.keystore file, update revoltbuild.properties to have your passwords and run:

```sh
./gradlew assembleRelease -x app:uploadSentryProguardMappingsRelease
```

It will be located in `app/build/outputs/apk/debug/` under the name `app-debug.apk`

Download it to your system by right clicking on the file like so:

<img width="455" height="596" alt="image" src="https://github.com/user-attachments/assets/2fdffb6b-0fdc-4131-97b4-3360ae8871d8" />

Send it to your phone, and install and run it!

Alternatively, you can send it to your phone right from Codespaces using magic wormhole (installed on this codespace instance by default)

`wormhole send app/build/outputs/apk/debug/app-debug.apk`

It will give you a code that you can punch into your phone. 

Either use the [Wormhole William app from the Google Playstore](https://play.google.com/store/apps/details?id=io.sanford.wormhole_william&hl=en_US)

Or get the apk [directly from here](https://github.com/psanford/wormhole-william-mobile/releases/tag/v1.0.13)

Or install [Termux](https://termux.dev/en/), then install `wormhole-rs` on Termux with `pkg install magic-wormhole-rs` and fetch the apk with
`wormhole-rs receive YOUR_CODE_HERE`

## Quick Build

If you don't want to download the apks in the releases section and rather build yourself, follow these steps:

Fire up a Github Codespaces instance at this link [here](https://github.com/codespaces/new?hide_repo_select=true&ref=combined-pr&repo=1020437871&skip_quickstart=true&machine=premiumLinux&devcontainer_path=.devcontainer%2Fdevcontainer.json&geo=UsEast)

The URL should have it selected for you automatically, but be sure to use this branch for your instance!

Note that this url will have an 8-core instance selected by default. Feel free to use a smaller instance, but I've ran into build errors with that.
At the time of writing, Github offers a number of free hours for personal accounts, but note that this bigger instance will use more of your free hours than a smaller one. **For just building the apk and downloading it to whatever device, this should be fine though**. Be sure to delete the instance when you are done. **It won't cost you $$ if you don't have payment set up with Github or have budget limits.** [See the billing page for more details.](https://docs.github.com/en/billing/concepts/product-billing/github-codespaces#pricing-for-paid-usage)

After the instance fires up run

```sh
./gradlew assembledebug --no-daemon
``` 
To generate a debug version of the application. 

If you wanted a signed copy that isn't in debug mode, set up a release-key.keystore file, update revoltbuild.properties to have your passwords and run:

```sh
./gradlew assembleRelease -x app:uploadSentryProguardMappingsRelease
```

It will be located in `app/build/outputs/apk/debug/` under the name `app-debug.apk`

Download it to your system by right clicking on the file like so:

<img width="455" height="596" alt="image" src="https://github.com/user-attachments/assets/2fdffb6b-0fdc-4131-97b4-3360ae8871d8" />

Send it to your phone, and install and run it!

Alternatively, you can send it to your phone right from Codespaces using magic wormhole (installed on this codespace instance by default)

`wormhole send app/build/outputs/apk/debug/app-debug.apk`

It will give you a code that you can punch into your phone. 

Either use the [Wormhole William app from the Google Playstore](https://play.google.com/store/apps/details?id=io.sanford.wormhole_william&hl=en_US)

Or get the apk [directly from here](https://github.com/psanford/wormhole-william-mobile/releases/tag/v1.0.13)

Or install [Termux](https://termux.dev/en/), then install `wormhole-rs` on Termux with `pkg install magic-wormhole-rs` and fetch the apk with
`wormhole-rs receive YOUR_CODE_HERE`

## Quick Start

Open the project in Android Studio. You can then run the app on an emulator or a physical device by
running the `app` module.

In-depth setup instructions can be found
at [Setting up your Development Environment](https://revoltchat.github.io/android/contributing/setup/)
