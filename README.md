# slimefun-slim

Build-time woven **Slimefun 4** for Minecraft 26.x (Paper 1.21+ compatible). This
project does **not** fork the upstream source. It follows the official
[Slimefun/Slimefun4](https://github.com/Slimefun/Slimefun4) `experimental` branch
verbatim and applies all differences at **build time**:

- upstream source is pinned as a git **submodule** and compiled **unchanged**,
- small source patches (currently: MC 26 support) are applied by the build script,
- PaperLib / dough compatibility replacements are **woven in** via maven-shade
  (see `overlay/`).

The result is a jar that behaves like upstream, but actually runs on modern
Paper 26.x servers where the vanilla Slimefun jar no longer works.

> ℹ️ This project is **not** affiliated with the Slimefun team. It is an
> unofficial build that ships the untouched GPLv3 upstream code plus build-time
> compatibility fixes. See [LICENSE](LICENSE).

---

## Why not a fork?

Forking means every upstream change has to be merged by hand and the history
diverges. With build-time weaving:

- **Upstream is never edited** — it stays a pristine submodule at a pinned commit.
- **Updating** = bump `<upstream.version>` (or run `./build.sh --update`) and
  re-run the build; if a patch no longer applies, the build tells you loudly.
- **Reproducible** — identical jar from the pinned commit on any machine.

## Why this exists

The old `main` branch of this repository was a manual fork of Slimefun that
added MC 26 support by editing upstream sources directly. That work is preserved
in `main` / tag `v1.0.0`. This `weaving` branch rebuilds the same functionality
without touching upstream code.

---

## Building

Requirements: JDK 16+ (21 recommended), Maven 3.8+, git, and network access for
dependencies.

```bash
# reproducible build of the pinned upstream commit
./build.bat              # Windows
./build.sh               # Linux / macOS / CI

# optional: track the latest upstream experimental HEAD instead of the pin
./build.sh --update

# also build the slim profile (en/zh translations + tuned config.yml)
./build.sh -Pslim
```

Output jars are written to `dist/`:

| Build    | Jar                                | Contents |
|----------|------------------------------------|----------|
| default  | `Slimefun v4.9-UNOFFICIAL-MC-26.1.2.jar` | 1:1 upstream: all languages, upstream `config.yml` |
| slim     | `Slimefun v4.9-UNOFFICIAL-slim-MC-26.1.2.jar` | `en`/`zh`/`zh-CN`/`zh-TW` languages, tuned `config.yml` (auto-update off, analytics off, larger ticker delays) |

> Upstream's unit tests cannot run against Paper API 26 (stale MockBukkit
> 1.21 mocks), so they are skipped by default. Pass `-Dmaven.test.skip=false` to
> attempt them.

Raw Maven equivalent: `mvn -Dmaven.test.skip=true clean package [-Pslim]`.

## Continuous tracking

`.github/workflows/build.yml` runs weekly (and on demand) and:

1. checks out the submodule,
2. follows the latest upstream `experimental` HEAD,
3. builds default + slim,
4. uploads the jars as CI artifacts (and optionally creates a GitHub release).

If an upstream change conflicts with a patch, the build goes **red** — that is
the signal to update the patch (see `docs/WEAVING.md`).

---

## Layout

```
upstream/   git submodule -> Slimefun/Slimefun4@<pinned commit>
overlay/    replacement sources woven into the jar (PaperLib + dough MC26 stubs)
patches/    source patches applied to the untouched upstream tree before compile
slim/       slim profile resources (tuned config.yml)
dist/       build output (git-ignored)
```

More: [docs/WEAVING.md](docs/WEAVING.md) (how the weaving works), 
[docs/UPDATING.md](docs/UPDATING.md) (how to track a new upstream version).

## Benchmarks

| Report | What it measures | Result |
|---|---|---|
| [docs/benchmarks/benchmark.md](docs/benchmarks/benchmark.md) | TPS / MSPT / CPU vs load (slim vs bare server) | no material tick degradation; +5.8 s boot, +52 MB boot peak |
| [docs/benchmarks/memory-benchmark.md](docs/benchmarks/memory-benchmark.md) | Memory (RSS / HWM / cgroup) at identical `-Xmx8000M` | **−32.7% RSS (−613 MB)** vs old fork; **−73% boot time** |

Chart preview (full set in `docs/benchmarks/memory-benchmark.md`):

![Working set](docs/images/benchmark/memory-rss-hwm.png)

## JVM tuning

`start.sh` / `start.bat` are templates with the G1GC flags used during load
benchmarking. Tuning is intentionally **not** baked into the plugin.

---

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE). The upstream Slimefun
code (GPLv3) is redistributed unmodified under the same license.
