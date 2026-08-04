# UPDATING — tracking a new upstream version

This project pins the upstream Slimefun commit it builds against. Two ways to
move the pin:

## Option A — one-off local build against the latest upstream

```bash
./build.sh --update          # checks out origin/experimental, builds, copies to dist/
./build.bat --update         # Windows
```

This does **not** change the pinned commit for future builds — it is a one-off.

## Option B — permanently move the pin (recommended)

1. Update the submodule and read the new commit:
   ```bash
   git submodule update --init upstream
   git -C upstream fetch origin experimental
   git -C upstream checkout --force origin/experimental
   NEW=$(git -C upstream rev-parse --short HEAD)
   ```
2. Point the submodule at it and bump the pin in `pom.xml`:
   ```bash
   git add upstream
   # edit pom.xml: <upstream.version>NEW</upstream.version>
   ```
3. Check whether the patches still apply:
   ```bash
   git -C upstream checkout --force $NEW
   for p in patches/*.patch; do git -C upstream apply --check --3way "$p" || echo "NEEDS FIX: $p"; done
   ```
4. Regenerate any patch that fails (see `docs/WEAVING.md` → "To add a patch").
5. Verify both profiles:
   ```bash
   ./build.sh
   ./build.sh -Pslim
   ```
6. Smoke-test on a Paper 26 server, then commit the new submodule pointer +
   `pom.xml` + regenerated patches.

## What to check after an update

- **New MC version** — if upstream still lacks it, `0001-add-minecraft-26.patch`
  may need the newest constant (or upstream may have added it already, in which
  case the patch becomes a no-op / fails and can be dropped).
- **API renames** — the `Attribute.GENERIC_MAX_HEALTH -> MAX_HEALTH` style
  changes appear when Mojang renames Bukkit API constants; patch them per file.
- **dough / PaperLib** — if upstream bumps these, re-check the overlay stubs
  still match the relocated package names.

## CI

`.github/workflows/build.yml` runs weekly. It builds the **latest** upstream
HEAD (not the pin) so you get an early warning when upstream breaks a patch. Red
CI = a patch or overlay needs updating; fix it with the steps above and commit.
