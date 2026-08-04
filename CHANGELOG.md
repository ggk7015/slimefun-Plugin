# Changelog

## weaving (current)

Rebuilt as **build-time weaving** — upstream is a pinned submodule, all fixes
are applied at build time. No source fork.

### Added
- `upstream/` git submodule pinning `Slimefun/Slimefun4` @ `5374034` (`experimental`).
- `patches/0001-add-minecraft-26.patch` — `MinecraftVersion.MINECRAFT_26` enum constant (MC 26 support).
- `patches/0002-attribute-max-health.patch` — `Attribute.MAX_HEALTH` rename (Bandage, MedicalSupply, Splint, VampireBlade).
- `overlay/` — MC 26 compatible replacements for PaperLib (stub) and dough
  skins/versions, woven in via maven-shade relocations + filters.
- `pom.xml` — upstream sources compiled from the submodule; `default`
  (activeByDefault, full parity) and `slim` (`-Pslim`, en/zh + tuned config) profiles.
- `build.bat` / `build.sh` — submodule checkout (pinned or `--update`),
  idempotent patch apply, `mvn clean package`, jars copied to `dist/`.
- `.github/workflows/build.yml` — weekly upstream tracking, default + slim
  builds, artifact upload, optional GitHub release.
- `start.sh` / `start.bat` — JVM (G1GC) tuning templates from the load benchmark.
- `LICENSE` — GPLv3 (copied from upstream; upstream code redistributed unmodified).

### Changed
- Default build is now **1:1 upstream**: all languages, upstream `config.yml`.
  (The old fork's all-languages build also shipped upstream defaults; the slim
  build keeps the tuned config.)
- Slim build now bundles only `en` / `zh` / `zh-CN` / `zh-TW` translations.

### Removed
- Direct edits of upstream sources (they live as patches now).
- `docs/benchmarks` + `docs/images/benchmark` are kept **locally only**
  (git-ignored) — benchmark data stays out of the repository.

### Notes
- Upstream unit tests cannot compile/run against Paper API 26 (stale MockBukkit
  1.21 mocks); skipped by default (`-Dmaven.test.skip=true`).

## v1.0.0 (tag, historical `main`)

The original manual fork that added MC 26 support by editing upstream sources
directly. Preserved as-is on `main` / tag `v1.0.0` and superseded by this
`weaving` branch.
