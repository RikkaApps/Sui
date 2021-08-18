# Sui

Modern super user interface implementation on Android. It's is "fileless", which means no binary files like "su" is required. The API is built on Android's binder mechanism, this is the same as most Android APIs.

> The name, Sui, also comes from a character. (<https://www.pixiv.net/artworks/71435059>)

## Background

To all root implementations on Android, the "su", a "shell" runs as root, is the only interface exposed to the user and the developer. This is too far from the Android world.

First, we need to talk about how system API works. For example, we can use `PackageManager#getInstalledApplications` to get the app list. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us. Android uses `Binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side so that the system server can check if the app has the permission to do the operation.

Back to "su", there are commands provided by the Android system. In the same example, to get the app list with "su", we have to use `pm list`. This is too painful.

1. Text-based, this means there is no structured data like `PackageInfo` in Java. You have to parse the output text.
2. It is much slower because run a command means at least one new process is started. And `PackageManager#getInstalledApplications` is used inside `pm list`.
3. The possibility is limited to how the command can do. The command only covers a little amount of Android APIs.

In fact, for Magisk and other root projects, makes the "su" to work is not that easy as some people think (let "su" itself work and the communication between the "su" and the manager app have a lot of unhappy work behind).

The result is, everyone, suffers from "su". The goal of this project is to replace the "su" executable and call for the root community to switch to the correct way.

Note, the full implementation of the root is far more than a "su" executable, there is a lot of hard work to be done before, so this project requires [Magisk](https://github.com/topjohnwu/Magisk/) to run. The current Sui is a [Riru](https://github.com/RikkaApps/Riru) module, Riru modules are also Magisk modules.

## Introduction

With the help of Magisk and Riru, we can start a daemon process that runs under root and hijack an unused binder call in the system server, make apps that can acquire the binder from the root daemon. This is pretty similar to binding an Android service using [AIDL](https://developer.android.com/guide/components/aidl), but the remote service is running under root.

Sui shares the same API design with [Shizuku](https://github.com/RikkaApps/Shizuku). The main function is "binder call as root", use Android APIs directly in Java as the identity of the root, and "user service", start app's own AIDL-style Java service under root. This will make root app development much more comfortable.

Also, requesting "root permission" can have the same experience as Android's runtime permission, the app will get a Java callback instead of counting timeout by self. Even more, the confirmation UI runs under the systemui process, the is much more reliable and no "manager app" is required.

In conclusion, we can get a superuser interface similar to standard Android APIs, and no "su" binary or "root manager" app is required in the whole process.

## Build

Clone with `git clone --recurse-submodules`.

Gradle tasks:

* `:riru:assembleDebug/Release`

   Generate Magisk module zip to `out`.

* `:riru:pushDebug/Release`

   Push the zip with adb to `/data/local/tmp`.

* `:riru:flashDebug/Release`

   Flash the zip with `adb shell su -c magisk --install-module`.

* `:riru:flashAndRebootDebug/Release`

   Flash the zip and reboot the device.

## Guide for users

* Install the zip from Magisk manager
* Open Management UI:
  - (Android 8.0+) Through shortcut, enter "Developer options" to add the shortcut
  - Enter `*#*#784784#*#*` in the default dialer app
* Play with apps that support Sui

The behavior of existing apps using "su" will NOT change. You can ask the developer of root apps to use Sui, or wait for the day that Sui becomes part of Magisk.

For existing apps that already support Shizuku, add support for Sui (including changes for Shizuku API v11) should only take less than half an hour.

### Interactive shell

Sui provides interactive shell.

Since Sui does not add files to `PATH`, the files need to be copied manually. See `/data/adb/sui/post-install.example.sh` to learn how to do this automatically.

After the files are correctly copied, use `rish` as 'sh'.

> What's the meaning of this tool? There is already "su" from Magisk.
>
> This does helped me to investigate [a bug of Magisk](https://github.com/topjohnwu/Magisk/issues/3976) that happens rarely. At that time, Magisk's su is not available.

## Guide for application developers

https://github.com/RikkaApps/Shizuku-API
