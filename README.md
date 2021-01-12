# Sui

Modern super user interface implementation on Android.

**Work in progress (management UI + demo + README not finished).**

> The name, Sui, also comes from a character. (<https://www.pixiv.net/artworks/71435059>)

## Background

To all root implementations on Android, the "su", a "shell" runs as root, is the only interface exposed to the user and the developer. This is too far from the Android world.

First, we need to talk how system API works. For example, we can use `PackageManager#getInstalledApplications` to get the app list. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us. Android uses `Binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side, so that the system server can check if the app has the permission to do the operation.

Back to "su", there are commands provided by Android system. The same example, to get the app list with "su", we can use `pm list`. This is too painful.

1. Test based, this means there is no structured data like `PackageInfo` in Java and you have to parse the output text.
2. Is is much slower because run a command means at least one new process is started. And `PackageManager#getInstalledApplications` is used inside `pm list`.
3. The possibility is limited to how the command can do. The command only covers a little amount of Android APIs.

In fact, for Magisk and other root projects, makes the "su" to work is not that easy as some people think (let "su" itself work and the communication between the "su" and the manager app have a lot of unhappy work behind).

The result is, everyone suffers from "su". The goal of this project is to replace the "su" executable and call for the root community to switch to the correct way.

Note, the full implementation of root is far more than a "su" executable, there is a lot of hard work to be done before, so this project requires [Magisk](https://github.com/topjohnwu/Magisk/) to run. The current Sui is a [Riru](https://github.com/RikkaApps/Riru) module, Riru modules are also Magisk modules. In the future, Sui maybe shipped with official Magisk.

## Introduction

With the help with Magisk and Riru, we can start a daemon process runs under root and hijack an unused binder call in the system server, make apps can acquire the binder from the root daemon. This is pretty similar to binding an Android service using [AIDL](https://developer.android.com/guide/components/aidl), but the remote service is running under root.

Sui shares the same API design with [Shizuku](https://github.com/RikkaApps/Shizuku). The main function are "binder call as root", use Android APIs directly in Java as the identity of root, and "user service", start app's own AIDL-style Java service under root. This will make root app developing much more comfortable.

Also, requesting root permission can have the same experience like Android permission, the app will get Java callback instead of counting timeout by self. Even more, the confirmation UI runs under the systemui process, the is much more reliable and no "manager app" is required.

In conclusion, we can get a super user interface similar to standard Android APIs, and no "su" binary or "root manager" app is required in the whole process.

## Build

* Clone with `git clone --recurse-submodules`
* Run gradle task `:module:assembleRelease` task from Android Studio or the terminal, zip will be saved to `out`

## Install

Install the zip from Magisk manager.