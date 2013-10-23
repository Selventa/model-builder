#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/../
cd "${DIR}" || exit 1
. env.sh || exit 1

CLASSPATH="."
for lib in $(find . -path "./libs*" -name "*.jar" -not -name "groovy-all*.jar"); do
    CLASSPATH="$CLASSPATH:$lib"
done

exec "${DEV_TOOLS_GROOVY_DIR}/bin/groovysh" -classpath "$CLASSPATH"
