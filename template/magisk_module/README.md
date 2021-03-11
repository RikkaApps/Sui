# Sui

<https://github.com/RikkaApps/Sui>

## Changelog

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