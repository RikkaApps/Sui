# Sui

Modern super user interface (SUI) implementation on Android. <del>The name, Sui, also comes from [a character](https://www.pixiv.net/artworks/71435059).</del>

## Introduction

Sui provides Java APIs, [Shizuku API](https://github.com/RikkaApps/Shizuku-API), for root apps. It mainly provides the ability to use Android APIs directly (almost in Java as the identity of the root, and start app's own AIDL-style Java service under root. This will make root app development much more comfortable.

Another advantage is that Sui does not add binaries to `PATH` and does not install a manager app. This means we no longer need to spend a huge amount of time to fight with apps that detect them.

To be clear, the full implementation of "root" is far more than "su" itself, there is a lot of hard work to be done before. Sui is not a full root solution, it requires Magisk to run.

<details>
  <summary>Why "su" is unfriendly for app development</summary>

The "su", a "shell" runs as root, is too far from the Android world.

To explain this, we need to talk about how system API works. For example, we can use `PackageManager#getInstalledApplications` to get the app list. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us. Android uses `Binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side so that the system server can check if the app has the permission to do the operation.

Back to "su", there are commands provided by the Android system. In the same example, to get the app list with "su", we have to use `pm list`. This is too painful.

1. Text-based, this means there is no structured data like `PackageInfo` in Java. You have to parse the output text.
2. It is much slower because run a command means at least one new process is started. And `PackageManager#getInstalledApplications` is used inside `pm list`.
3. The possibility is limited to how the command can do. The command only covers a little amount of Android APIs.

Although it is possible to use Java APIs as root with `app_process` (there are libraries like libsu and librootjava), transfer binder between app process and root process is painful. If you want the root process to run as a daemon. When the app process restarts, it has no cheap way to get the binder of the root process.

In fact, for Magisk and other root solutions, makes the "su" to work is not that easy as some people think (let "su" itself work and the communication between the "su" and the manager app have a lot of unhappy work behind).

</details>

## User guide

Note, the behavior of existing apps that only supports "su" will NOT change.

### Install

You can download and install Sui from Magisk directly. Or, download the zip from [release](https://github.com/RikkaApps/Sui/releases/) and use "Install from storage" in Magisk.

### Management UI

- (Android 8.0+, Sui 12.1+) Long press system settings from the home app, you will find the shortcut of Sui
- (Android 8.0+, Sui 12+) Enter "Developer options" in system settings, the system will ask you to add the shortcut of Sui
- Enter `*#*#784784#*#*` in the default dialer app

Note, the shortcut way requires your home app supports shortcut APIs that adds from Android 7.0 and 8.0. Unless you are using a old home app, you can the shortcut with no problem.

### Interactive shell

Sui provides interactive shell.

Since Sui does not add files to `PATH`, the files need to be copied manually. See `/data/adb/sui/post-install.example.sh` to learn how to do this automatically.

After the files are correctly copied, use `rish` as 'sh'.

## Application development guide

https://github.com/RikkaApps/Shizuku-API

## Build

Clone with `git clone --recurse-submodules`.

Gradle tasks:

`Flavor` could be `Riru` and `Zygisk`, and `BuildType` could be `Debug` and `Release`.

* `:module:assemble<Flavor><BuildType>`

   Generate Magisk module zip to `out`.

* `:module:push<Flavor><BuildType>`

   Push the zip with adb to `/data/local/tmp`.

* `:module:flash<Flavor><BuildType>`

   Install the zip with `adb shell su -c magisk --install-module`.

* `:module:flashAndReboot<Flavor><BuildType>`

   Install the zip and reboot the device.

## Internals

Sui requires [Magisk](https://github.com/topjohnwu/Magisk) (and [Riru](https://github.com/RikkaApps) for non-Zygisk version). Magisk allows us to run processes as uid 0 and a "do anything" SELinux context. Riru or Zygisk allows us to inject into system server process and app processes.

In short, there are four parts:

- Root process

  This is a root process started by Magisk. This process starts a Java server that implements Shizuku API and private APIs used by other parts.

- SystemServer inject

  - Hooks `Binder#execTransact` and finally allow us to handle an unused binder call
  - Implements "get binder", "set binder" logic in that binder call, so that root process can send its binder to the system server, and the apps can acquire root process's binder

- SystemUI inject

  - Acquire the fd of our apk from the root server, create a `Resource` instance from it
  - Show confirmation window with our `Resource` and `ClassLoader` when recevied callback from the root server

- Settings inject

  - Acquire the fd of our apk from the root server, create a `Resource` instance from it
  - Publish shortcut which targets an existing `Acitivty` but with a special intent extra
  - Replace `ActivityThread#mInstrumentation` to intervene the `Acitivty` instantiate process, if the intent has the speical extra, create our `Activity` which uses our `Resource` and `ClassLoader`
