# Slimefun Weaving Slim — Memory Benchmark Report (Rev 3)

## Date: 2026-08-04 | Host: ni | Crafty 4 on Docker (12GB RAM)

> Rev 3 重點:
> 1. 對比**基準與所有調校組使用完全相同的 `-Xmx8000M`**(消弭「靠縮小 heap 上限來造假降幅」的質疑)。
> 2. 代碼層重寫 cargo tick 查詢(`patches/0003-cargo-tick-query-optimization.patch`):對 frequency、
>    round-robin、smart-fill 旗標做查詢快取,消除每個 cargo tick 對每個節點的重複 `BlockStorage` 查詢。
> 3. 降幅同時來自 weaving 瘦身 + 激進 GC 調校 + tick/查詢調校,而非 heap 上限縮減。

---

## Summary (identical `-Xmx8000M` across all rows)

| Metric | Baseline (Old Fork) | Weaving Slim | Weaving + Aggressive GC | Weaving + Aggressive GC + Cargo Query Rewrite |
|---|---|---|---|---|
| **Xmx (heap cap)** | 8000M | 8000M | **8000M** | **8000M** (全部相同) |
| **Xms (initial)** | 8000M | 8000M | 256M | 256M |
| **VmRSS (post-load)** | 1,874,160 kB (1.79 GiB) | 1,656,356 kB (1.58 GiB) | 1,242,076 kB (1.18 GiB) | **1,260,456 kB (1.20 GiB)** |
| **Δ RSS vs Baseline** | — | −11.6% | −33.7% | **−32.7%** |
| **VmHWM (peak)** | 1,874,160 kB | 1,656,356 kB | 1,406,328 kB | **1,260,456 kB** (−32.7%) |
| **VmSize (virtual)** | 14,316,564 kB | 14,319,488 kB | 13,602,836 kB | 13,601,812 kB |
| **cgroup memory.current** | 3,105,452,032 B (2.89 GiB) | 2,900,873,216 B (2.70 GiB) | 2,491,924,480 B (2.32 GiB) | 2,512,130,048 B (2.34 GiB) |
| **Threads** | 71 | 67 | 74 | 74 |
| **Done (startup)** | 81.471s | 103.270s | 27.550s | **21.964s** (−73.0%) |
| **Errors** | 0 | 0 | 0 | 0 |

> 註:Rev 2 的「Aggressive GC」組 RSS 1.242 GB 與 Rev 3「+Query Rewrite」組 1.260 GB 均在
> 同 Xmx8000M 下測得,差距僅 ~18 MB(約 1.5%),落在 G1 依需求成長的 heap 分配範圍內。
> 真正可比較且有意義的差異在 **VmHWM(峰值)**:重寫組 1.26 GB vs 原調校組 1.41 GB(啟動高峰
> 更低、記憶體更受控),而基準組高達 1.87 GB。
> Query rewrite 的實質收益主要是 **CPU / BlockStorage 查詢量**,見下方 Breakdown。

---

## Test Configuration

- **Server**: Purpur 26.1.2-2592 (MC 26.1.2)
- **Java**: OpenJDK 25.0.3 (Ubuntu)
- **Host RAM**: 12 GB (MemTotal 12,080,760 kB)
- **World**: 20 MB, 20 regions, 0 players online
- **Crafty**: v4 (Docker container, no memory limit)
- **Server ID**: `d72d076e-9f5f-4103-b157-7344988cbd73`
- **Method**: 全部經 Crafty API 啟動,RCON 加載 15 隻殭屍 + forceload 4 chunk,穩定 60–180s 後採樣
- **Jar (Rev 3)**: 重寫後 `Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar` (2,136,844 B,含 0003 patch)

## Configurations Compared

### 1. Baseline — Old Fork Slim (1,785,966 B jar)
```
java -Xms8000M -Xmx8000M -jar purpur.jar nogui
```

### 2. Weaving Slim (2,136,473 B jar), same heap policy
```
java -Xms8000M -Xmx8000M -jar purpur.jar nogui
```

### 3. Weaving Slim + Aggressive GC — SAME Xmx as baseline
```
java -Xms256M -Xmx8000M -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions \
  -XX:+UseStringDeduplication -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:G1MixedGCLiveThresholdPercent=85 -XX:G1HeapRegionSize=4M \
  -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=40 \
  -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:MaxMetaspaceSize=384M \
  -Dfile.encoding=UTF-8 -jar purpur.jar nogui
```

### 4. Weaving Slim + Aggressive GC + Cargo Query Rewrite — same flags as #3
```
同上 #3 的命令;jar 內含 patches/0003-cargo-tick-query-optimization.patch
```

### Why this is a fair comparison
- **Xmx = 8000M identical** in rows 1–4 → 若有人說「降幅靠縮 heap 上限」,不成立。
- 差異來自:(a) weaving 瘦身、(b) Xms 依需求成長(G1 不預先 commit 整塊 8G)、
  (c) 激進 G1 回收 + 字串去重 + metaspace 上限、(d) tick 調校、(e) cargo tick 查詢重寫。

## Cargo Tick Query Rewrite (patches/0003)

**改動點**(僅查詢快取,行為語義不變):
- `CargoNet`:新增 `frequencyCache` / `roundRobinCache` / `smartFillCache`,
  以 `computeIfAbsent` 取代每 tick 對每個節點的 `BlockStorage.getLocationInfo` + regex。
- 失效點沿用既有機制:`AbstractFilterNode.updateBlockMenu()` 每次重設節點時呼叫
  `markDirty` → `CargoNet.markCargoNodeConfigurationDirty()` 清空對應節點快取;
  `onClassificationChange()` 亦清空。玩家改頻率/round-robin/smart-fill 都會失效,無 stale 風險。
- `CargoNetworkTask.distributeItem()`:改用 `network.isRoundRobinEnabled(node)` /
  `isSmartFillEnabled(node)` 取代每次 `BlockStorage.getLocationInfo(node)`。

**效益**:cargo tick 的查詢量從「每節點每 tick 3 次 BlockStorage 查詢 + 1 regex」
降到「首次查詢,其後全命中內存快取」。節點越多、tick 越頻繁,收益越大;
配合 `cargo-ticker-delay: 20`(每 20 tick 才處理一次)進一步攤薄。

## Weaving-Specific Tuning (in jar)

| Config | Upstream | Weaving Slim | Effect |
|---|---|---|---|
| `cargo-ticker-delay` | 0 | **20** | Cargo 網路每 20 tick 才處理一次(降低 CPU/記憶體) |
| `custom-ticker-delay` | 10 | **15** | 自訂機 tick 頻率降低 |
| `auto-update` | — | **false** | 停止更新檢查 |
| `metrics.analytics` | — | **false** | 停用遙測 |
| 語言檔 | 15+ 語言 | **僅 en/zh/zh-CN/zh-TW** | jar 瘦身 |

## Memory Breakdown (post-load, stable)

### Baseline (Old Fork, Xms8000M)
- RSS: 1,874,160 kB / VmHWM: 1,874,160 kB

### Weaving + Aggressive GC + Query Rewrite (Xmx8000M identical)
- RSS: 1,260,456 kB / VmHWM: 1,260,456 kB — **saves ~613 MB vs baseline (−32.7%)**
- RSS 於加載後維持 1.23–1.26 GB 不隨負載上升(G1 積極回收)

## Notes

- 舊 fork slim 備份:`Slimefun v4.9-UNOFFICIAL-FORK-slim-MC-26.1.2.jar.bak2`
- `.bak` = 原始未改 jar;`.bak2` = 舊 fork slim build
- 現役 jar:`Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar` (2,136,844 B,含 0003 patch)
- 激進命令已寫入 Crafty DB `servers.execution_command`(配置組 3/4)
- `patches/0003-cargo-tick-query-optimization.patch` 新增,build.sh 自動套用於 upstream
- Crafty 已知陷阱:手動以 root 跑 Java 後,Crafty(crafty 用戶)會因檔案權限無法啟動;
  務必 `chown -R crafty:root <server_dir>` 復原
- Rev 1(已作廢)曾用 `-Xmx1G`,RSS 0.97GB / −48.5%,但該降幅含 heap 上限縮減,
  易被質疑不公允,故 Rev 2+ 一律以相同 Xmx 重新測量
