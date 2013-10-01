#!/usr/bin/env bash
shopt -s expand_aliases
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

# loop
while inotifywait -qq -r -e modify "$DEV_MODULES_DIR"; do
    gradle -q compileGroovy jar
    if [ "$?" != "0" ]; then 
        notify-send -t 3000 "Failed compilation.\n\n$RESULT"
        echo "$RESULT" 2>&1
    else
        . scripts/deploy.sh
        notify-send -t 3000 "Success, deployed."
    fi
done
