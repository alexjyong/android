[![Open in DevPod!](https://devpod.sh/assets/open-in-devpod.svg)](https://devpod.sh/open#https://github.com/alexjyong/android)
<div align="center">
    <h1>Refork</h1>
    <p>Forked and modified version of the <a href="https://revolt.chat">Revolt</a> Android app.</p>
    <br/><br/>
    <div>
        <img width="537" height="455" alt="image" src="https://github.com/user-attachments/assets/ea3e5f70-295d-4015-b4c5-0b981f68f143" />
        <br/>
    </div>
    <br/><br/><br/>
</div>

## Description

NOTE: This is a forked version of the Android app for the [Revolt](https://revolt.chat) chat platform.
**I am not affilated with the Revolt Team, nor is this an official Revolt product.**

I made this for some QOL changes that aren't present in the current version at the time of writing such as notification support, jump to reply, voice messages, and more!

**This app also works on de-Googled phones as well!**

You can download the latest APK [here](https://github.com/alexjyong/android/releases/latest).

For support, discussion, updates and other things, visit our support server on [Revolt](https://rvlt.gg/C7qQMwsZ).

## Features Added
Tap a card to expand.
<details>
<summary><strong>Notification Support!!!🎉🎉</strong></summary>

![Notification support preview](https://github.com/user-attachments/assets/8123962e-e2d3-4690-87e3-44e09724a29c)
</details>

<details>
<summary><strong>Role Mention (with notifications!)</strong></summary>
    
![Role Mention - with notifications](https://github.com/user-attachments/assets/bb779690-2ac4-4293-b662-102c92a20923)
</details>

<details>
<summary><strong>Voice messages</strong></summary>

![Voice messages preview](https://github.com/user-attachments/assets/8b794d8e-46cc-407e-84aa-99b4128d5922)
</details>

<details>
<summary><strong>Emojis next to channel names</strong></summary>

![Channel Emojis](https://github.com/user-attachments/assets/6c37f059-47ef-4028-aac9-ef233f7a85e4)
</details>

<details>
<summary><strong>Jump to replied message</strong></summary>

![Jump to Reply](https://github.com/user-attachments/assets/1dc0fb6b-521d-40e2-a833-1ada19bd207b)
</details>

<details>
<summary><strong>Server context menu on long press</strong></summary>

![Server Context Menu](https://github.com/user-attachments/assets/034eea4f-0d21-4ddc-8b44-b7c70dde8965)
</details>

<details>
<summary><strong>Recently used emojis</strong></summary>

![Recently Used Emojis](https://github.com/user-attachments/assets/6f1e126a-8e73-49ee-a5d1-c4841907e14a)
</details>

<details>
<summary><strong>Auto-open server channel list</strong></summary>

![Server channel list open by default](https://github.com/user-attachments/assets/6a4d7c96-924e-4344-9e19-b90589fa6908)
</details>

And more!


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

In-depth setup instructions are pretty much the same as the stock Revolt app and can be found
at [Setting up your Development Environment](https://revoltchat.github.io/android/contributing/setup/)
