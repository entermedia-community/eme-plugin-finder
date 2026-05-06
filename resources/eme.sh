#!/bin/bash
set -e

##This is run from the /bin/eme location that is linked

JAVA=/usr/lib/jvm/jdk-22.0.1/bin/java

# Resolve EMELIB: prefer sibling eme-lib, then env var, then system default
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(pwd)"

if [ -z "$EMELIB" ]; then
    if [ -d "$SCRIPT_DIR/../skills" ] && [ -d "$SCRIPT_DIR/../resources" ]; then
        export EMELIB="$(cd "$SCRIPT_DIR/.." && pwd)"
    elif [ -d "$SCRIPT_DIR/../eme-lib" ]; then
        export EMELIB="$(cd "$SCRIPT_DIR/../eme-lib" && pwd)"
    elif [ -d "/usr/share/eme-lib" ]; then
        export EMELIB="/usr/share/eme-lib"
    else
        echo "ERROR: Cannot find eme-lib. Set EMELIB env var or place eme-lib next to this script." >&2
        exit 1
    fi
fi

CMD="${1:-start}"

case "$CMD" in
  init)
    TARGET="$SCRIPT_DIR"
    TARGET="$(mkdir -p "$TARGET" && cd "$TARGET" && pwd)"
 #   echo "Initializing new eme-server at: $TARGET"

    # Create directory structure
    mkdir -p "$TARGET/tomcat/conf" "$TARGET/tomcat/logs" "$TARGET/tomcat/webapps" "$TARGET/tomcat/work"

    # Copy tomcat conf and webapp templates from eme-lib deploy
    cp -rn "$EMELIB/deploy/tomcat/conf/." "$TARGET/tomcat/conf/" 2>/dev/null || true

    mkdir -p "$TARGET/webapp/WEB-INF/base"
    ln -s "$EMELIB/webapp/WEB-INF/base/_site.xconf" "$TARGET/webapp/WEB-INF/base/_site.xconf"

    ## symbolically link each of the $EMELI/skills/*/webapp folders to $TARGET/webapp/WEB-INF/base/*
    for skill in "$EMELIB/skills/"*/; do
        ##See if the skill has a webapp folder
        if [ ! -d "$skill/webapp" ]; then
            continue
        fi  

        ##Dont link if its already linked        
        if [ -L "$TARGET/webapp/WEB-INF/base/$(basename "$skill")" ]; then
            continue
        fi
        ln -s "${skill}webapp" "$TARGET/webapp/WEB-INF/base/$(basename "$skill")"
    done 

    echo "Done. Run: $TARGET/eme.sh start"
    ;;

  start)
    export EMSERVER="${2:-$SCRIPT_DIR}"
    export EMSERVER="$(cd "$EMSERVER" && pwd)"
    ARGS_TEMPLATE="$EMELIB/resources/tomcat.args"

    if [ ! -f "$ARGS_TEMPLATE" ]; then
        echo "ERROR: $ARGS_TEMPLATE not found. Run: eme.sh init <server-path>" >&2
        exit 1
    fi

    # Java @argfile does not expand shell variables, so expand them here
    EXPANDED_ARGS=$(mktemp /tmp/tomcat-args.XXXXXX)
    trap "rm -f $EXPANDED_ARGS" EXIT
    envsubst '$EMELIB $EMSERVER' < "$ARGS_TEMPLATE" > "$EXPANDED_ARGS"

    echo "EMELIB=$EMELIB"
    echo "EMSERVER=$EMSERVER"
    exec "$JAVA" "@$EXPANDED_ARGS" org.apache.catalina.startup.Bootstrap start
    ;;

  *)
    echo "Usage: eme.sh [init <new-server-path> | start [server-path]]" >&2
    exit 1
    ;;
esac
