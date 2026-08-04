#!/usr/bin/env bash
# slimefun-slim build pipeline
#
# Direction 4: git submodule + patch mechanism.
# The upstream Slimefun source is compiled UNCHANGED from the "upstream"
# submodule. All modifications are applied at build time:
#   1. checkout the pinned (or latest) upstream commit
#   2. apply the patches from patches/ (currently: MC 26 support)
#   3. compile upstream source + overlay/ stubs (maven-shade-plugin weaves
#      PaperLib & dough replacements in)
#
# Usage:
#   ./build.sh              reproducible build of the pinned upstream commit
#   ./build.sh --update     track the latest upstream "experimental" HEAD
#   ./build.sh --skip-checkout   build whatever upstream is currently checked out
#   ./build.sh -Pslim       also pass any maven arguments (e.g. -Pslim, -Dmaven.test.skip=true)
#
# Output jars are copied to ./dist/
set -euo pipefail

cd "$(dirname "$0")"

UPDATE=0
SKIP_CHECKOUT=0
ARGS=()

for arg in "$@"; do
    case "$arg" in
        --update) UPDATE=1 ;;
        --skip-checkout) SKIP_CHECKOUT=1 ;;
        *) ARGS+=("$arg") ;;
    esac
done

UPSTREAM_VERSION="$(sed -n 's:.*<upstream.version>\([0-9a-f]*\)</upstream.version>.*:\1:p' pom.xml)"
: "${UPSTREAM_VERSION:?could not read <upstream.version> from pom.xml}"

# 1. Make sure the upstream submodule is present
git submodule update --init upstream

# 2. Checkout the source we build against
if [[ "$SKIP_CHECKOUT" == "1" ]]; then
    echo "[build] using the upstream working tree as-is"
elif [[ "$UPDATE" == "1" ]]; then
    echo "[build] tracking latest upstream 'experimental'..."
    git -C upstream fetch origin experimental
    git -C upstream checkout --force origin/experimental
    echo "[build] upstream now at $(git -C upstream rev-parse --short HEAD)"
else
    echo "[build] building pinned upstream ${UPSTREAM_VERSION}..."
    git -C upstream checkout --force "$UPSTREAM_VERSION"
fi

# 3. Apply our patches (idempotent: reverse first, then apply)
for patch in patches/*.patch; do
    echo "[build] applying ${patch}"
    git -C upstream apply -R "$patch" 2>/dev/null || true
    git -C upstream apply --check "$patch"
    git -C upstream apply "$patch"
done

# 4. Build
# Upstream unit tests cannot run against paper-api 26 (stale mocks /
# MockBukkit 1.21 vs MC 26 API), so they are skipped by default.
# Pass -Dmaven.test.skip=false in ARGS to override.
mvn -B clean package -Dmaven.test.skip=true "${ARGS[@]:-}"

# 5. Collect the jars
mkdir -p dist
cp target/"Slimefun v"*.jar dist/ 2>/dev/null || true
echo
echo "[build] done. Artifacts:"
ls -la dist/
