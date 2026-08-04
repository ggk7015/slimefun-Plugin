@echo off
REM ============================================================
REM   Slimefun JVM tuning template (start.bat)
REM ============================================================
REM   Direction 3: JVM tuning is deliberately kept OUT of the plugin.
REM   Copy this file next to your server jar and adjust the values.
REM
REM   These flags were used during the load benchmark and are a sane
REM   starting point for a small Slimefun server. Tune -Xmx to your
REM   available RAM (rule of thumb: -Xmx ~= 50-75%% of your RAM cap).
REM ============================================================

REM Path to your server jar
set SERVER_JAR=paper.jar

java -Xms128M -Xmx512M ^
     -XX:+UseG1GC ^
     -XX:MaxGCPauseMillis=100 ^
     -XX:G1NewSizePercent=30 ^
     -XX:G1MaxNewSizePercent=40 ^
     -XX:G1HeapRegionSize=4M ^
     -XX:G1ReservePercent=20 ^
     -XX:+ParallelRefProcEnabled ^
     -XX:+DisableExplicitGC ^
     -XX:+UnlockExperimentalVMOptions ^
     -XX:+UseStringDeduplication ^
     -Dfile.encoding=UTF-8 ^
     -jar %SERVER_JAR% nogui
