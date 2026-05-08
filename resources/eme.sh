#!/bin/bash -ex


##This is run from the /bin/eme location that is linked

##JAVA_HOME is not set throw an error if JAVA_HOME is not set
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME is not set. Please set JAVA_HOME to your JDK installation." >&2
    exit 1
fi  

JAVA="$JAVA_HOME/bin/java"

if [ -z "$USERID" ]; then
    USERID="entermedia"
    GROUPID="entermedia"
fi  

if [[ ! $(id -u entermedia 2>/dev/null) ]]; then
    groupadd -g $GROUPID entermedia
    useradd -ms /bin/bash entermedia -g entermedia -u $USERID
fi

# Resolve EMELIB: prefer sibling eme-lib, then env var, then system default

SCRIPT_DIR="$(pwd)"

if [ -z "$EMELIB" ]; then
    if [ -d "$SCRIPT_DIR/../eme-lib" ]; then
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
  
   
    ;;

  start)    

    TARGET="$2"
    TARGET="$(sudo mkdir -p "$TARGET" && cd "$TARGET" && pwd)"
 #   echo "Initializing new eme-server at: $TARGET"

    echo "**** Initializing eme-server instance at: $TARGET"


    sudo chown ${USERID}:${GROUPID} $TARGET

    if [ ! -d "$TARGET/tomcat" ]; then
        # Copy tomcat conf and webapp templates from eme-lib deploy
        # Create directory structure
        sudo -u entermedia mkdir -p "$TARGET/tomcat/conf" "$TARGET/tomcat/logs" "$TARGET/tomcat/webapps" "$TARGET/tomcat/work"
        sudo -u entermedia cp -rn "$EMELIB/tomcat/conf/." "$TARGET/tomcat/conf/" 2>/dev/null || true
        sudo chown -R ${USERID}:${GROUPID} "$TARGET/tomcat" 
    fi


    if [ ! -f "$TARGET/webapp/WEB-INF/base/_site.xconf" ]; then
        sudo -u entermedia mkdir -p "$TARGET/webapp/WEB-INF/base"
        sudo -u entermedia ln -s "$EMELIB/webapp/WEB-INF/base/_site.xconf" "$TARGET/webapp/WEB-INF/base/_site.xconf"
        
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/web.xml" ]; then
        sudo -u entermedia  cp -rp "$EMELIB/webapp/WEB-INF/web.xml" "$TARGET/webapp/WEB-INF/web.xml"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/node.xml" ]; then
         sudo -u entermedia cp -rp "$EMELIB/webapp/WEB-INF/node.xml" "$TARGET/webapp/WEB-INF/node.xml"
    fi


    if [ ! -d "$TARGET/webapp/WEB-INF/bin" ]; then
       sudo -u entermedia ln -s "$EMELIB/webapp/WEB-INF/bin" "$TARGET/webapp/WEB-INF/bin"
    fi

 #   sudo chown ${USERID}:${GROUPID} "$TARGET/webapp/"

    if [ ! -d "$TARGET/data/system" ]; then
        sudo -u entermedia mkdir -p "$TARGET/data/system"
        sudo -u entermedia cp -rp "$EMELIB/skills/system/webapp/data/defaultdata/" "$TARGET/data/system/"
    fi

    if [ ! -d "$TARGET/webapp/WEB-INF/data" ]; then
        sudo -u entermedia ln -s "$TARGET/data" "$TARGET/webapp/WEB-INF/data" 
        sudo chown -R ${USERID}:${GROUPID} "$TARGET/webapp/WEB-INF/data"
    fi

    ## symbolically link each of the $EMELI/skills/*/webapp folders to $TARGET/webapp/WEB-INF/base/*
    for pair in "$TARGET/skills:$TARGET/webapp" "$EMELIB/skills:$TARGET/webapp/WEB-INF/base"; do
        src="${pair%%:*}"
        dst="${pair##*:}"
        for skill in "$src"/*/; do
            if [ -d "$skill/webapp" ] && [ ! -L "$dst/$(basename "$skill")" ]; then
                sudo -u entermedia ln -s "${skill}webapp" "$dst/$(basename "$skill")"
            fi
        done
    done

    sudo chown ${USERID}:${GROUPID} "$TARGET/webapp/WEB-INF/base"
    
    echo "**** Starting eme-server"
    export EMSERVER="${2:-$SCRIPT_DIR}"
    export EMSERVER="$(cd "$EMSERVER" && pwd)"
    ARGS_TEMPLATE="$EMELIB/resources/tomcat.args"

    if [ ! -f "$ARGS_TEMPLATE" ]; then
        echo "ERROR: $ARGS_TEMPLATE not found. Run: eme.sh init <server-path>" >&2
        exit 1
    fi

    # Java @argfile does not expand shell variables, so expand them here
    EXPANDED_ARGS=$(sudo -u entermedia mktemp $TARGET/tomcat/work/tomcat-args.XXXXXX)
    sudo chmod 600 "$EXPANDED_ARGS"
    trap "sudo -u entermedia rm -f $EXPANDED_ARGS" EXIT
    sudo -u entermedia sed -e "s|\$EMELIB|$EMELIB|g" -e "s|\$EMSERVER|$EMSERVER|g" "$ARGS_TEMPLATE" > "$EXPANDED_ARGS"

    echo "$JAVA $(cat "$EXPANDED_ARGS") org.apache.catalina.startup.Bootstrap start"

    #Run Tomcat as entermedia user
    sudo -u entermedia  "$JAVA" "@$EXPANDED_ARGS" org.apache.catalina.startup.Bootstrap start
    ;;

  *)
    echo "Usage: eme.sh [init <new-server-path> | start [server-path]]" >&2
    exit 1
    ;;
esac
