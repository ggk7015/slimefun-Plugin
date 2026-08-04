#!/usr/bin/env bash
# ============================================================
#  Slimefun JVM tuning template (start.sh)
# ============================================================
#  Direction 3: JVM tuning is deliberately kept OUT of the plugin.
#  Copy this file next to your server jar and adjust the values.
#
#  These flags were used during the load benchmark and are a sane
#  starting point for a small Slimefun server. Tune -Xmx to your
#  available RAM (rule of thumb: -Xmx ~= 50-75% of your RAM cap).
# ============================================================

# Path to your server jar
SERVER_JAR="paper.jar"

# Heap, G1 garbage collector, Metaspace/string deduplication and
# generic JVM settings (all flags below are the recommended baseline).
exec java \
    -Xms128M \
    -Xmx512M \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=4M \
    -XX:G1ReservePercent=20 \
    -XX:+ParallelRefProcEnabled \
    -XX:+DisableExplicitGC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication \
    -Dfile.encoding=UTF-8 \
    -jar "$SERVER_JAR" nogui
