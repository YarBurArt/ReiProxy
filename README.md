# ReiProxy

A compact HTTP/HTTPS traffic analysis tool under development, now rewritten in Kotlin and targeted at Android phones and tablets. Unlike desktop-focused solutions, this app uses a touch-friendly mobile layout to make the most of limited screen space. The Kotlin version lives on the `master` branch. The old Python version is still on the `main` branch for reference.

## Problem Statement

This is not intended for bug‑bounty ordinary mobile apps, at least not yet, because secure certificate management at the OS level is harder than trusting certificates inside a browser. **It’s meant for situations where the only available devices are Android‑based.** Workarounds include using the [Caido](https://github.com/caido/caido) web interface, which isn’t enough adapted to limited resources and small screens; running a Linux environment via [AnLinux](https://github.com/EXALAB/AnLinux-App) or [Andronix](https://github.com/AndronixApp/AndronixOrigin) that uses proot + an Xorg server over TigerVNC to launch WM/DE and inside run Burp Suite (but this brings interface-adaptation issues, the need to keep session running, and possible lag); or trying [mitmweb](https://github.com/mitmproxy/mitmproxy/tree/main), which just lacks some features. This application is a prototype that aims to address those problems. In the future I plan to optimize the app using the NDK.

Not a replacement but a companion tool, and a partial source of inspiration is [PCAPdroid](https://github.com/emanuele-f/PCAPdroid), which focuses on a slightly different objective and use case.


## Installation

### Kotlin dev version

```bash
git clone https://github.com/YarBurArt/ReiProxy.git
```
```bash
cd ReiProxy/app
```
```bash
git checkout master
```

Open the project in Android Studio and wait for Gradle sync to finish.
Run the app on a device or emulator from Android Studio.

Or just build debug apk with

```bash
./gradlew assembleDebug
```

### Python dev version

```bash
git clone https://github.com/YarBurArt/ReiProxy.git
```
```bash
cd ReiProxy
```
```bash
git checkout main
```

```bash
uv sync
```

```bash
uv run python main.py
```

Build app:

```bash id="fks3mc"
uv run --group build buildozer -v android debug
```

---

## How it works

I was too lazy to write docs. That's left from the template is outdated now.

Further details are documented within the code itself.

