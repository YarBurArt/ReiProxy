# ReiProxy

A compact HTTP/HTTPS traffic analysis tool under development, written in Python with Kivy/KivyMD and targeted at Android phones and tablets. Unlike desktop-focused solutions, this app uses a Kivy multi-pane layout adapted for touch screens to make the most of limited space. Its core relies on mitmproxy’s async library for request handling, and the UI call the AndroidX Proxy-Override API for vpn mode. Features planned: built-in certificate authority management, basic hex viewing/editing of binary payloads (for now just webview of [CyberChef](https://gchq.github.io/CyberChef/)), and a gesture-friendly repeater for manual testing during mobile bug-bounty work.

## Problem Statement

This is not intended for bug‑bounty ordinary mobile apps, at least not yet, because secure certificate management at the OS level is harder than trusting certificates inside a browser. **It’s meant for situations where the only available devices are Android‑based.** Workarounds include using the [Caido](https://github.com/caido/caido) web interface, which isn’t enough adapted to limited resources and small screens; running a Linux environment via [AnLinux](https://github.com/EXALAB/AnLinux-App) or [Andronix](https://github.com/AndronixApp/AndronixOrigin) that uses proot + an Xorg server over TigerVNC to launch WM/DE and inside run Burp Suite (but this brings interface-adaptation issues, the need to keep session running, and possible lag); or trying [mitmweb](https://github.com/mitmproxy/mitmproxy/tree/main), which just lacks some features. This application is a prototype that aims to address those problems. In the future I plan to optimize the app using the NDK.

Not a replacement but a companion tool, and a partial source of inspiration is [PCAPdroid](https://github.com/emanuele-f/PCAPdroid), which focuses on a slightly different objective and use case.

## Installation Instructions & Usage

```bash
git clone https://github.com/YarBurArt/ReiProxy.git
```
```bash
cd ReiProxy
```

Install dependencies

```bash
uv sync
```

Run on desktop for testing

```bash
uv run python main.py
```

Build app

```bash
uv run --group build buildozer -v android debug
```

---

## How it works

> I was too lazy to write docs, this is what's left from the template.

### Navigation
The [`Root`](https://github.com/kulothunganug/kivy-lazy-loading-template/blob/main/libs/uix/root.py) is based on [`ScreenManager`](https://kivy.org/doc/stable/api-kivy.uix.screenmanager.html) and additionally provides a few navigation methods: `push(screen_name, side)`, `push_replacement(screen_name, side)` and `pop()`.

Also `load_screen(screen_name)` method can be used to load the screen and the kv file without setting it as the current screen.

To incorporate additional screens into your app, follow these steps:

1. Create `screen_file.py` in the `libs/uix/baseclass/` directory.
2. Create `screen_file.kv` in the `libs/uix/kv/` directory.
3. Add the screen details to `screens.json` as shown below:
```json
{
    ...,
    "screen_name": {
        "module": "libs.uix.baseclass.screen_file",
        "object": "ScreenObjectName",
        "kv": "libs/uix/kv/screen_file.kv"
    }
}
```
This template already contains three screens as example which uses all the navigation methods.

## Buildozer
To use this template for mobile devices, make sure to add **json** to your `buildozer.spec` file, such as
```
# (list) Source files to include (let empty to include all the files)
source.include_exts = py,png,jpg,kv,atlas,gif,json
```

### Further details are documented within the code itself.
