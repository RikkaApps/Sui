# Sui

Modern super user implementation for Android. API design from Shizuku.

**Work in progress (management UI + demo + README not finished).**

> The name, Sui, comes from a character. (<https://www.pixiv.net/artworks/71435059>)

## The advantage

* Use "binder", the "native" IPC method of Android, super fast and super convenient for root app developing
* No "su" exectuable and "manager app" <sup>**〔1〕**</sup> required, impossible to be detected<sup>**〔2〕**</sup>
* Share the same API design of [Shizuku](https://github.com/RikkaApps/Shizuku)

<sub>**〔1〕** The confirmation and management UI are run from the systemui process</sub>
<br><sub>**〔2〕** The current inject method, Riru, still leaves in memory, but could change in the future</sub>