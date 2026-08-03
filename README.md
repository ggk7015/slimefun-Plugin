# Slimefun-slim — 非官方精簡版 Slimefun / Unofficial Slimmed Slimefun

> 以數據為依據的非官方精簡建置,基於官方 **Slimefun v4.9-UNOFFICIAL**。
> A data-driven unofficial slimmed build based on the official **Slimefun v4.9-UNOFFICIAL**.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

---

## 概覽 / Overview

針對 **Paper / Purpur**(Minecraft **1.21.10** 與 **26.1.2**)的 Slimefun 精簡建置:
透過 Maven Shade `minimizeJar`、移除未使用資源、以最小化 stub 取代 PaperLib 等手法,將最終 jar 由官方建置壓縮至 **1.703 MB(−18.5%)**,並提供經實測的 JVM 旗標使伺服器 RAM 使用量低於 **1.5 GB**。

A slimmed Slimefun build for **Paper / Purpur** (Minecraft **1.21.10** and **26.1.2**): by using Maven Shade `minimizeJar`, removing unused resources and replacing PaperLib with a minimal stub, the final jar is reduced from the official build to **1.703 MB (−18.5%)**, with measured JVM flags keeping server RAM usage below **1.5 GB**.

---

## 基準 / Baseline

| 項目 Item | 內容 Value |
|---|---|
| 上游 Upstream | [Slimefun/Slimefun4](https://github.com/Slimefun/Slimefun4) |
| 分支 Branch | `experimental` |
| 基線 commit Baseline commit | `5374034c8713248909e60e6855c6f745fd7ca676` |
| 基線版本 Base version | **v4.9-UNOFFICIAL** (`com.github.slimefun:Slimefun:4.9-UNOFFICIAL`) |
| 授權 License | GNU GPL v3.0(衍生作品) |
| 測試伺服器 Test server | Purpur 26.1.2(JVM 25.0.3)|

> 本倉庫保留完整上游 git 歷史;所有改動為基線 commit 之上的衍生修改。
> This repository preserves the full upstream git history; all changes are derivative modifications on top of the baseline commit.

---

## 成果數據 / Measured Results

### Jar 大小 / Jar size

| 階段 Stage | 大小 Size | 變動 Delta |
|---|---|---|
| 官方建置 Official build | ~2.09 MB | — |
| + `minimizeJar`(Shade) | 1.764 MB | −15.6% |
| + PaperLib 換成 stub | 1.732 MB | −1.8% |
| + 移除 zh / zh-CN / zh-TW 語言 | **1.703 MB**(1744.1 KB)| **−18.5%** |

最終產物 MD5:`6BD56055856FE22789AF4957B3ED2B65`

### Class / 資源統計 / Class & resource stats

| 指標 Metric | 官方 Official | 最終 Final |
|---|---|---|
| class 條目 class entries | 948 | **773** |
| 資源條目 resource entries | 109 | **64** |
| paperlib classes | 30 | **1**(stub)|
| 語言 languages | en / zh / zh-CN / zh-TW | **en**(移除 63,616 B)|

### RAM 實測 / Memory benchmark

環境:Windows 11, JDK 25.0.3, 同一測試伺服器(空載, Purpur 26.1.2)

| JVM 旗標 JVM flags | WorkingSet |
|---|---|
| `-Xms6G -Xmx6G -XX:+UseZGC`(舊) | 1836 MB |
| `-Xms256M -Xmx1G -XX:+UseG1GC ...`(建議) | **718 MB** |

`jcmd` 測量(建議旗標):heap committed **415 MB** / used **292 MB**;最壞情況估算 ≈ 1.36 GB(< 1.5 GB)。

### 啟動驗證 / Boot verification

每次啟動皆驗證:
```
[Slimefun] Successfully loaded 555 Items and 258 Researches
[dough: protection] Loading Protection Modules...
Done (13–17s, 空載)
```
無 `NoClassDefFoundError`、無版本誤判、無資源載入錯誤。

---

## 改動清單 / Modifications

1. **`pom.xml`**:啟用 Shade `<minimizeJar>true</minimizeJar>`,排除 `META-INF/**`;保留 dough relocate、移除 PaperLib 相依。
2. **PaperLib → stub**:`src/main/java/.../libraries/paperlib/PaperLib.java` 僅保留 `isPaper()`(回傳 `true`,因目標僅 Paper/Purpur)。移除 30 個 paperlib class;33 個源碼檔案改為純 Bukkit API(`getState()`、`teleportAsync()` 等)。
3. **自製版本解析**:`libraries/dough/versions/MinecraftVersion.java` 支援雙位數 major(26.x)與 `26.1.2.build.2592-stable` 格式;`api/MinecraftVersion` 新增 `MINECRAFT_26`。
4. **版本誤判修正**:`Slimefun#parseMinecraftVersion` 修正 1.21.10 被誤判為「1.1.x」而停用插件的 bug。
5. **語言資源**:移除 zh / zh-CN / zh-TW,僅保留 en(+ `translators.json`)。
6. **其他**:`BlockDataService`、`Slimefun.java` 註解同步清理。

---

## 需求 / Requirements

- Java **21+**(於 JDK 25 驗證)
- Paper / Purpur,MC **1.21.10** 或 **26.1.2**

---

## 建置 / Build

```bat
set MAVEN_OPTS=-Xmx2g
mvn -Dmaven.test.skip=true clean package
```

輸出:`target/Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar`

---

## 部署 / Deploy

1. 停止伺服器。
2. 將 jar 複製為 `plugins/Slimefun.jar`。
3. 建議 JVM 旗標(實測 RAM < 1.5 GB):

```bat
java -Xms256M -Xmx1G -XX:+UseG1GC -XX:MaxMetaspaceSize=192M -XX:MaxDirectMemorySize=256M -XX:ReservedCodeCacheSize=64M -jar purpur.jar nogui
```

---

## 授權 / License

本專案為 **GNU GPL v3.0**。基於 [Slimefun](https://github.com/Slimefun/Slimefun4)(GPL-3.0)衍生;原始版權歸 **TheBusyBiscuit 與 Slimefun contributors** 所有。

This project is licensed under the **GNU GPL v3.0**. It is a derivative of [Slimefun](https://github.com/Slimefun/Slimefun4) (GPL-3.0); original copyright belongs to **TheBusyBiscuit and the Slimefun contributors**.

---

## 免責聲明 / Disclaimer

非官方建置,與 Slimefun 官方無關,不提供官方支援;使用風險自負。改動與上游不同,相關問題請勿回報至上游專案。

Unofficial build, not affiliated with the Slimefun project. No official support; use at your own risk. Modifications differ from upstream — do NOT report issues to the upstream project.
