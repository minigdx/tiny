
# 🧸 Tiny Game Engine

> 🎮 Tiny is a lightweight, cross-platform game engine powered by Lua — perfect for small games, game jams, and rapid prototyping.

![Breakout created with Tiny Game Engine](./images/breakout.gif)

[![GitHub release](https://img.shields.io/github/v/release/minigdx/tiny)](https://github.com/minigdx/tiny/releases)
[![License: MIT](https://img.shields.io/github/license/minigdx/tiny)](https://github.com/minigdx/tiny/blob/main/LICENSE)

---

## ✨ Features

- 🖥️ & 🌐 **Multiplatform** – Runs on desktop and web
- ✍️ **Lua scripting** – Simple, flexible, and fast
- 🔄 **Hot reload** – Instantly see your code changes
- 🪶 **Lightweight** – No bloat, just the essentials for small games

---

## 🚀 Getting Started

Create your first game with just a few lines of Lua:

```lua
function _draw()
    -- draw a rectangle at {x: 10, y: 10} with the size {width: 100, height: 50} 
    shape.rectf(10, 10, 100, 50)
end
```

➡️ Read the [Getting Started Guide](https://minigdx.github.io/tiny/) to set up the engine and start creating!

---

## 📦 Download

Get the latest version from the [Releases Page](https://github.com/minigdx/tiny/releases).

---

## 📚 Documentation

Full documentation is available on the [Tiny website](https://minigdx.github.io/tiny/), including:

- Engine setup
- Lua API reference
- Examples and tutorials

🗣️ A behind-the-scenes presentation was given at [DroidKaigi 2024 – Tokyo](https://2024.droidkaigi.jp/en/timetable/683368/):
- 📽️ [Watch the session on YouTube](https://www.youtube.com/watch?v=4_i_Xp96IMM)
- 📑 [View the slides](https://speakerdeck.com/dwursteisen/crafting-cross-platform-adventures-building-a-game-engine-with-kotlin-multiplatform)

[![Crafting Cross-Platform Adventures](tiny-doc/src/docs/asciidoc/sample/droidkaigi-tiny-export.gif)](https://speakerdeck.com/dwursteisen/crafting-cross-platform-adventures-building-a-game-engine-with-kotlin-multiplatform)

---

## 🎮 Games Made With Tiny

Want to create games like these? Dive into the docs and start building:

[![Camping](./tiny-doc/src/docs/asciidoc/sample/camping.gif)](https://dwursteisen.itch.io/trijam-camping)
[![Type It](./tiny-doc/src/docs/asciidoc/sample/level-up.gif)](https://dwursteisen.itch.io/trijam-220-type-it)
[![Memory Pong](./tiny-doc/src/docs/asciidoc/sample/memory.gif)](https://dwursteisen.itch.io/memory-pong-trijam-251)
[![One Light For Three Seconds](./tiny-doc/src/docs/asciidoc/sample/only_three_seconds.gif)](https://dwursteisen.itch.io/one-light-for-three-seconds)
[![Connect Me](./tiny-doc/src/docs/asciidoc/sample/connect_me.gif)](https://dwursteisen.itch.io/connect-me)
[![Meiro De Maigo 2](./tiny-doc/src/docs/asciidoc/sample/meiro_de_maigo2.gif)](https://dwursteisen.itch.io/meiro-de)
[![Freezming](./tiny-doc/src/docs/asciidoc/sample/freezming.gif)](https://dwursteisen.itch.io/freezming)
[![Gravity Balls](./tiny-doc/src/docs/asciidoc/sample/gravity-balls.gif)](https://dwursteisen.itch.io/gravity-balls)

---

## 📄 License

🧸 Tiny is open-source software licensed under the [MIT License](https://github.com/minigdx/tiny/blob/main/LICENSE).

Use it freely in personal, jam, or commercial projects.

---

## 🛠️ Tech Overview

| Core           | Scripting | Platforms       | License |
|----------------|-----------|------------------|---------|
| Kotlin Multiplatform | Lua       | Desktop, Web      | MIT     |
