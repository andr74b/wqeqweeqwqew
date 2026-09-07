#!/bin/sh
set -eu

# Resolve paths from this script so it also works when launched from another folder.
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$project_dir"

# A failed build or test must never start a stale artifact.
./mvnw --batch-mode --no-transfer-progress clean verify

if [ -n "${JAVA_HOME:-}" ]; then
    java_command="$JAVA_HOME/bin/java"
else
    java_command=java
fi

exec "$java_command" -jar target/blackjack.jar --vaadin.productionMode=true "$@"
