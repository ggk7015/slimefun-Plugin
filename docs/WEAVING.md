# WEAVING — how this build works

The goal: ship Slimefun that runs on Paper 26.x **without** maintaining a source
fork. Everything that makes our jar different from upstream happens during the
Maven build.

## Pipeline

```
upstream/ (submodule, pinned commit)
        │  build.bat / build.sh
        │  1. git checkout <pinned commit> (or origin/experimental with --update)
        │  2. apply patches/ (idempotent)
        │  3. mvn clean package
        │     ├─ compile: upstream/src/main/java  +  overlay/src/main/java
        │     │            (overlay added via build-helper-maven-plugin)
        │     ├─ resources: default profile (all languages) OR slim profile (en/zh)
        │     └─ shade:    relocate io.papermc.lib + dough + commons-lang,
        │                  drop stale dough skins/versions classes, keep overlay stubs
        ▼
dist/Slimefun v4.9-UNOFFICIAL[-slim]-MC-26.1.2.jar
```

### 1. Submodule

`upstream` is a git submodule pointing at the upstream repository, pinned to the
commit in `<upstream.version>` (see `pom.xml`). `git submodule update --init
upstream` fetches it. `--update` instead checks out the latest
`origin/experimental`.

### 2. Patches (source-level fixes that cannot be overlays)

Some upstream code simply **does not compile** against the Paper 26 API. Overlay
classes can't fix a compile error inside an upstream file, so those changes are
patches:

| Patch | What / why |
|-------|-----------|
| `0001-add-minecraft-26.patch` | Add the `MINECRAFT_26(26, 0, "26.x")` enum constant, so `MinecraftVersion` recognises MC 26 servers. |
| `0002-attribute-max-health.patch` | `Attribute.GENERIC_MAX_HEALTH` was renamed to `Attribute.MAX_HEALTH` in MC 26 (Bandage, MedicalSupply, Splint, VampireBlade). |
| `0003-cargo-tick-query-optimization.patch` | Cache cargo network config lookups (`CargoNet` frequency/round-robin/smart-fill + `CargoNetworkTask.distributeItem`), cutting per-node-per-tick `BlockStorage` queries. No behaviour change. |

To add a patch: edit the file inside `upstream/`, run
`git -C upstream diff > patches/000N-my-change.patch`, then
`git -C upstream checkout -- .`. The patch must apply cleanly to the pinned
commit; if it stops applying after an upstream update, the CI build goes red and
the patch needs regenerating.

### 3. Overlays (dependency / class replacements)

The upstream jar shades PaperLib and dough. Both are incompatible with MC 26:

- **PaperLib** — upstream calls `io.papermc.lib.PaperLib`, etc. The shade plugin
  relocates those references to
  `io.github.thebusybiscuit.slimefun4.libraries.paperlib`, and `overlay/` ships a
  stub in that exact package (guarded by Bukkit API calls, so no Paper dependency).
- **dough skins** — dough's `GameProfile`-based skin loading breaks on MC 26
  (final class, `IncompatibleClassChangeError`). The original `dough.skins`
  classes are **excluded** from the shaded jar and `overlay/` provides MC 26
  compatible replacements. Same for `dough.versions.MinecraftVersion` (can't
  parse the `26.1.2.build.NNN-stable` format).

The shade plugin relocates `io.github.bakedlibs.dough -> ...libraries.dough`,
`io.papermc.lib -> ...libraries.paperlib`, `org.apache.commons.lang ->
...libraries.commons.lang` (matching upstream's own shading).

### 4. Profiles

| Profile | Resources |
|---------|-----------|
| `default` (activeByDefault) | all languages, upstream `config.yml` — byte-for-byte upstream parity |
| `slim` (`-Pslim`) | `en`/`zh`/`zh-CN`/`zh-TW` languages + tuned `config.yml` |

Only one is ever active: activating `-Pslim` automatically deactivates the
`activeByDefault` profile (standard Maven behaviour).

## Why the tests are skipped

`upstream/src/test` compiles the stale `InventoryViewWrapper` mock and runs
against MockBukkit 1.21 — neither works with Paper API 26. Tests are therefore
skipped by default (`-Dmaven.test.skip=true`) in `build.bat` / `build.sh`.

## Golden rule

Never edit files under `upstream/` (or the build's behaviour silently diverges
and the submodule checkin won't match). If a change is needed:

- runtime / classpath fix  → `overlay/`
- source file won't compile → `patches/`
- resource / config tweak  → slim profile resources
