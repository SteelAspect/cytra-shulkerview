#!/usr/bin/env bash
# Runs a command (e.g. ./gradlew runClientGameTest -PgameTests=MinimapGameTest) on an invisible virtual display,
# silent, so automated test clients never show up on the screen.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/share/PrismLauncher/java/java-runtime-delta}"
export ALSOFT_DRIVERS=null
mkdir -p build
printf '%q ' "$@" > build/.headless-cmd
rm -f build/.headless-exit
dbus-run-session kwin_wayland --virtual --xwayland --width 1280 --height 720 --no-lockscreen \
    --exit-with-session="bash -c 'cd $(pwd) && $(cat build/.headless-cmd); echo \$? > build/.headless-exit'" >/dev/null 2>&1 || true
exit "$(cat build/.headless-exit 2>/dev/null || echo 1)"
