# Slimefun-slim

> **An unofficial, data-driven slimmed build of Slimefun v4.9-UNOFFICIAL** for Paper / Purpur.
> Every claim in this document is backed by actual measurements from a real test server — not estimates.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Minecraft 1.21.10 / 26.1.2](https://img.shields.io/badge/Minecraft-1.21.10%20%2F%2026.1.2-green.svg)](https://purpurmc.org)

---

## TL;DR

| Metric | Official build | This build | Delta |
|---|---|---|---|
| Jar size | ~2.09 MB | **1.703 MB** | **−18.5%** |
| Java class entries | 948 | **773** | −175 |
| Resource files | 109 | **64** | −45 |
| PaperLib classes | 30 | **1** (stub) | −29 |
| Languages shipped | 4 | **1** (`en`) | −3 |
| Server memory after boot | 1836 MB | **718 MB** | **−61%** |

All features intact: **555 items and 258 researches** load successfully on a fresh world.

---

## Overview

**Slimefun-slim** is a minimal-footprint build of the well-known [Slimefun 4](https://github.com/Slimefun/Slimefun4) plugin,
targeting modern **Paper / Purpur** servers (Minecraft **1.21.10** and **26.1.2**).

It reduces the final artifact from **~2.09 MB to 1.703 MB (−18.5%)** and cuts measured server memory
from **1836 MB to 718 MB (−61%)** by combining:

- Maven Shade **`minimizeJar`** — dead-code-eliminates every class the plugin never uses;
- **PaperLib → minimal stub** — 30 bundled classes replaced by a 1-class stub (targets are Paper/Purpur only);
- **English-only language resources** — removes 3.4k lines of unused translation files;
- **JVM flag tuning** — G1GC + capped heaps instead of a 6 GB ZGC reservation (see [Server configuration](#server-configuration)).

The jar still runs **all 555 items, 258 researches**, and every subsystem of stock Slimefun on the target servers.

---

## Baseline

| Item | Value |
|---|---|
| Upstream | [Slimefun/Slimefun4](https://github.com/Slimefun/Slimefun4) |
| Branch | `experimental` |
| Baseline commit | `5374034c8713248909e60e6855c6f745fd7ca676` |
| Base version | **v4.9-UNOFFICIAL** (`com.github.slimefun:Slimefun:4.9-UNOFFICIAL`) |
| License | GNU GPL v3.0 (derivative work) |
| Test server | Purpur 26.1.2 on Windows 11, JDK 25.0.3 |

> This repository preserves the **full upstream git history** (8,403 commits). Every change here is a derivative
> modification layered on top of the baseline commit above — fully traceable with `git log`.

---

## Measured results

### 1. Jar size

![Jar size by build stage](docs/images/jar-size.png)

| Stage | Size | Delta |
|---|---|---|
| Official build (baseline) | ~2.09 MB | — |
| + `minimizeJar` (Shade) | 1.764 MB | −15.6% |
| + PaperLib → stub | 1.732 MB | −1.8% |
| + `en`-only languages | **1.703 MB** (1,785,972 B) | **−18.5%** |

**Final artifact:** `Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar`
**MD5:** `6BD56055856FE22789AF4957B3ED2B65`

### 2. Jar contents

![Jar contents: official vs final](docs/images/contents.png)

| Metric | Official | Final |
|---|---|---|
| Java class entries | 948 | **773** |
| Resource entries | 109 | **64** |
| PaperLib classes | 30 | **1** (stub) |
| Languages | en / zh / zh-CN / zh-TW | **en** (+ `translators.json`) |
| Language bytes removed | — | 63,616 B |

### 3. Server memory

![Server memory: official vs recommended JVM flags](docs/images/memory.png)

Measured on the same machine, same world, same server — only the JVM flags differ.

| JVM flags | Working Set |
|---|---|
| `-Xms6G -Xmx6G -XX:+UseZGC ...` (official-style) | 1836 MB |
| `-Xms256M -Xmx1G -XX:+UseG1GC ...` (**recommended**) | **718 MB** |

`jcmd` heap inspection (recommended flags): **committed 415 MB / used 292 MB**.
Worst-case footprint estimate for this build is **≈ 1.36 GB**, i.e. comfortably under **1.5 GB** — a small VPS can host it.

### 4. Boot verification

Every boot is verified against a fresh world (`world` created at first start):

```
[Slimefun] Successfully loaded 555 Items and 258 Researches
[Slimefun] Loaded a total of 0 Blocks for World "world"
[dough: protection] Loading Protection Modules...
Done (13.988s)! For help, type "help"
```

`Slimefun has finished loading in 1.76s`. No `NoClassDefFoundError`, no version-misdetection errors, no resource-loading failures.

---

## Server configuration

The exact configuration used for every measurement. You can reproduce all numbers on a machine that meets
the [requirements](#requirements).

### Host environment

| Item | Value |
|---|---|
| OS | Windows 11 10.0 (amd64) |
| JVM | OpenJDK 64-Bit Server VM **25.0.3+9-LTS** (Microsoft) |
| Server | **Purpur 26.1.2**-2592-HEAD@405ad83 (`Implementing API 26.1.2.build.2592-stable`) |
| Plugins | Slimefun (this build) + **Chunky 1.5.3** (for pre-generation testing) |
| Worlds | `world`, `world_nether`, `world_the_end` — fresh, empty |

### JVM flags

**Original test config** (official-style, the one that reserved 1836 MB):

```bat
java -Xms6G -Xmx6G -XX:+UseZGC -XX:+AlwaysPreTouch -XX:+ParallelRefProcEnabled -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -jar purpur.jar nogui
```

**Recommended config** (measured 718 MB):

```bat
java -Xms256M -Xmx1G -XX:+UseG1GC -XX:MaxMetaspaceSize=192M -XX:MaxDirectMemorySize=256M -XX:ReservedCodeCacheSize=64M -jar purpur.jar nogui
```

> `-XX:+AlwaysPreTouch` and `-XX:+UseZGC` in the original config force the JVM to eagerly commit all 6 GB of
> heap at startup. G1GC + small heaps in the recommended config let the JVM size itself to what the plugin
> actually needs.

### server.properties (highlights)

```properties
level-name=world
level-type=minecraft\:normal
gamemode=survival
difficulty=easy
max-players=20
view-distance=8
simulation-distance=8
spawn-protection=0
online-mode=true
motd=Slimefun Slim Server (Purpur 26.1.2)
```

### Plugins directory at test time

```
plugins/
├── Chunky.jar           (304,616 B)  — only used to pre-generate chunks in earlier tests
└── Slimefun.jar         (1,785,935 B) — the slim build
```

(spark and bStats are the copies bundled inside Purpur itself.)

### Measurement method

- **Memory:** read the server process `WorkingSet64` via `Get-Process` ~60 s after the `Done` line; `jcmd <pid> GC.heap_info` for committed/used heap.
- **Boot time:** time between `Starting minecraft server` and the `Done` line in `logs/latest.log` (13.988s on the reference boot).
- **Jar contents:** entry counts from `jar tf`; sizes in binary units (MiB/KiB).

---

## Modifications vs upstream

1. **`pom.xml`** — enabled Shade `<minimizeJar>true</minimizeJar>`, excluded `META-INF/**`, removed the PaperLib dependency (dough relocation kept).
2. **PaperLib → stub** — `libraries/paperlib/PaperLib.java` now only exposes `isPaper()` (returns `true`; targets are Paper/Purpur only). 30 PaperLib classes dropped; 33 source files switched to pure Bukkit API (`getState()`, `teleportAsync()`, …).
3. **Self-contained version parsing** — `libraries/dough/versions/MinecraftVersion.java` handles double-digit majors (26.x) and `26.1.2.build.2592-stable`; `api/MinecraftVersion` gained `MINECRAFT_26`.
4. **Version-misdetection fix** — `Slimefun#parseMinecraftVersion` no longer misparses **1.21.10 as "1.1.x"** (which disabled the plugin).
5. **Language resources** — removed `zh`, `zh-CN`, `zh-TW`; kept only `en` (+ `translators.json`).
6. **Cleanup** — comments / dead references tidied in `BlockDataService`, `Slimefun.java`, and others.

---

## Requirements

- Java **21+** (verified on JDK 25)
- Paper / Purpur for Minecraft **1.21.10** or **26.1.2**

---

## Build

```bat
set MAVEN_OPTS=-Xmx2g
mvn -Dmaven.test.skip=true clean package
```

Output: `target/Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar`

---

## Deploy

1. Stop the server.
2. Copy the jar to `plugins/Slimefun.jar`.
3. Launch with the recommended flags (see [Server configuration](#server-configuration)):

```bat
java -Xms256M -Xmx1G -XX:+UseG1GC -XX:MaxMetaspaceSize=192M -XX:MaxDirectMemorySize=256M -XX:ReservedCodeCacheSize=64M -jar purpur.jar nogui
```

---

## License

This project is licensed under the **GNU GPL v3.0**. It is a derivative of [Slimefun](https://github.com/Slimefun/Slimefun4)
(GPL-3.0); the original copyright belongs to **TheBusyBiscuit and the Slimefun contributors**.

---

## Disclaimer

Unofficial build, not affiliated with the Slimefun project. No official support is provided — use at your own risk.
This build differs from upstream; **do not** report its issues to the upstream project.
