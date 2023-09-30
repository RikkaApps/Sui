# Changelog

### v13.5.1 (2023-09-19)

- Works on Android 14
- Works on ColorOS (OPPO & OnePlus) Android 14

### v13.0.1 (2023-02-01)

- Fix authentication error of `transactRemote` with `IBinder.FLAG_ONEWAY`
- Fix `rish` does not work on Android 8.x

### v12.7.1 (2022-10-06)

- Don't set `java.library.path` in rish (#38)
- Fix `peekUserService` not work after app process restarts
- Fix UserServices are not killed after permission is revoked
- Fix UserServices are not killed after the app is uninstalled
- Wrap hidden methods of `Instrumentation` (#41)

### v12.6.3 (2022-06-09)

* Works on Android 13 Beta 3

### v12.6.2 (2022-04-10)

* Temporary solution for [#35](https://github.com/RikkaApps/Sui/issues/35)
* Add `updateJson`, module now can be upgraded through Magisk app

### v12.6.1 (2022-02-23)

* Works on Android 13 DP1 (Apps use Sui/Shizuku may need changes also)
* Create `/data/adb/sui` on start (Swithing from Riru version to zygisk version will delete this folder, causing config unable to save)

### v12.4.0 (2022-01-09)

- Adapt recent Magisk (Zygisk) changes
- Remove the shortcut (shows when you long-press Settings) on uninstall (The shortcut that has already added to the launcher still needs manual removal)

### v12.3.0

- Zygisk support

### v12.2.1 (2021-09-20)

- Fix "adb install" under "adb root"
- Fix "adb root" support is not enabled for Android 11

### v12.2.0 (2021-09-11)

- Add adb root support (disabled by default, see description at [GitHub release](https://github.com/RikkaApps/Sui/releases))
- Fix using UserService will crash sui service on some devices

### v12.1.4 (2021-08-28)

- Make sure original extras from newActivity is not deserialized (For example, this will fix the crash of MIUI Settings "All specs" page)

### v12.1.3 (2021-08-26)

- Fix not working on Sony devices (Not sure if only China version ROMs have this problem)

### v12.1.2 (2021-08-26)

- Bug fix

### v12.1.1 (2021-08-25)

- Don't compile with R8 full mode

### v12.1.0 (2021-08-25)

- [Shizuku API 12](https://github.com/RikkaApps/Shizuku-API/releases/tag/12)
- Add interactive shell support (`post-install.sh` needs changes, see new `post-install.example.sh` for more)
- (Android 8.0+) Long press system settings from the home app, you will find the shortcut of Sui
- (Android 8.0+) Enter "Developer options" in system settings, the system will ask you to add the shortcut of Sui
- Use "the standard Android way" to create UI, the file size increases but a lot more things can achieve

### v11.5.0 (2021-03-02)

- Provide an experimental tool that can run commands, this tool can be used in terminal apps and adb shell (see README for more)
- Filter out packages without components
- Filter out nonexistent packages added by `MATCH_UNINSTALLED_PACKAGES` flag

> What's the meaning of the command-line tool? There is already "su" from Magisk.
>
> This does helped me to investigate [a bug of Magisk](https://github.com/topjohnwu/Magisk/issues/3976) that happens rarely. At that time, Magisk's su is not available.

### v11.4.5 (2021-02-21)

- Fix random authorization dialog or management ui not showing

### v11.4.4 (2021-02-19)

- Fix developer options crash on Android 12
- Works on devices that have dropped 32-bit support (Android 12 emulator or devices in the future)

### v11.4.3 (2021-02-14)

- Fix installation on x86

### v11.4.2 (2021-02-11)

- Reduce the file size

### v11.4.1 (2021-02-07)

- Fix an undefined behavior
- Skip packages with no code

### v11.4 (2021-01-26)

- Avoid a possible race condition

### v11.3 (2021-01-19)

- Open management UI trough a notification, this notification will show when you are in "Developer options"

### v11.2 (2021-01-18)

- Management UI works more like an `Activity` rather than a window

### v11.1 (2021-01-16)

- Fix permission for multi-process applications

### v11.0 (2021-01-16)

- First public version
