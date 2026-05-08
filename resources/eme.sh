#!/bin/bash -ex


##This is run from the /bin/eme location that is linked

##JAVA_HOME is not set throw an error if JAVA_HOME is not set
if [ -z "$JAVA_HOME" ]; then
    #checi if there is a jre path
    if [ -d "/usr/lib/jvm/jre" ]; then
        JAVA_HOME="/usr/lib/jvm/jre"
    else
        echo "JAVA_HOME is not set and /usr/lib/jvm/jre does not exist. Please set JAVA_HOME to a valid JRE path."
        echo "Please run iwth your local java version: sudo update-alternatives --install /usr/lib/jvm/jre jre /usr/lib/jvm/java-X.XX.X-openjdk-amd64 20000"
        exit 1
    fi
    
    JAVA_HOME="/usr/lib/jvm/jre"

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
  dockerstart)
    #Verify if $USERID is passed in
    if [ -z "$USERID" ]; then
       echo "USERID not set"
       exit 1 
    fi
    if [[ ! $(id -u entermedia 2>/dev/null) ]]; then
        groupadd -g $GROUPID entermedia
        useradd -ms /bin/bash entermedia -g entermedia -u $USERID
    fi

    JAVA_HOME="/usr/lib/jvm/java-18-openjdk-amd64"
    export JAVA_HOME

    sudo -u entermedia /usr/bin/eme start "$2"
    
   
    ;;

  start)    
    if [[ $(id -u) -eq 0 ]]; then
        echo "Don't run this script as root."
        exit 1
    fi
    
    #Set USERID and GROUPID to the current user if not running in Docker
    USERID="$(id -un)"
    if GROUPNAME=$(id -gn "$USERID" 2>/dev/null); then
        GROUPID="$GROUPNAME"
    else
        GROUPID="$USERID"
    fi

    TARGET="$2"
    TARGET="$(sudo mkdir -p "$TARGET" && cd "$TARGET" && pwd)"
 #   echo "Initializing new eme-server at: $TARGET"

    echo "**** Initializing eme-server instance at: $TARGET"


    
    #$USER is the user running the container
    sudo chown $USERID:$GROUPID $TARGET

    if [ ! -d "$TARGET/tomcat" ]; then
        # Copy tomcat conf and webapp templates from eme-lib deploy
        # Create directory structure
        mkdir -p "$TARGET/tomcat" "$TARGET/tomcat/conf" "$TARGET/tomcat/logs" "$TARGET/tomcat/webapps" "$TARGET/tomcat/work"
        cp -rn "$EMELIB/tomcat/conf/." "$TARGET/tomcat/conf/" 2>/dev/null || true
        cp -rpn "$EMELIB/tomcat/bin" "$TARGET/tomcat/" 2>/dev/null || true
        echo "export CATALINA_BASE=\"$TARGET/tomcat\"" >>"$TARGET/tomcat/bin/setenv.sh"
        sudo chown -R $USERID:$GROUPID "$TARGET/tomcat" 
        #chmod 755 "$TARGET/tomcat/bin/*.sh"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/base/_site.xconf" ]; then
         mkdir -p "$TARGET/webapp/WEB-INF/base"
         ln -s "$EMELIB/webapp/WEB-INF/base/_site.xconf" "$TARGET/webapp/WEB-INF/base/_site.xconf"
        sudo chown -R $USERID:$GROUPID "$TARGET/webapp"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/web.xml" ]; then
          cp -rp "$EMELIB/webapp/WEB-INF/web.xml" "$TARGET/webapp/WEB-INF/web.xml"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/node.xml" ]; then
          cp -rp "$EMELIB/webapp/WEB-INF/node.xml" "$TARGET/webapp/WEB-INF/node.xml"
    fi


    if [ ! -d "$TARGET/webapp/WEB-INF/bin" ]; then
        ln -s "$EMELIB/webapp/WEB-INF/bin" "$TARGET/webapp/WEB-INF/bin"
    fi

 #   sudo chown ${USERID}:${GROUPID} "$TARGET/webapp/"

    if [ ! -d "$TARGET/data/system" ]; then
         mkdir -p "$TARGET/data/system"
         cp -rp "$EMELIB/skills/system/webapp/data/defaultdata/" "$TARGET/data/system/"
    fi

    if [ ! -d "$TARGET/webapp/WEB-INF/data" ]; then
         ln -s "$TARGET/data" "$TARGET/webapp/WEB-INF/data" 
        sudo chown -R $USERID:$GROUPID "$TARGET/webapp/WEB-INF/data"
    fi

    ## symbolically link each of the $EMELI/skills/*/webapp folders to $TARGET/webapp/WEB-INF/base/*
    for pair in "$TARGET/skills:$TARGET/webapp" "$EMELIB/skills:$TARGET/webapp/WEB-INF/base"; do
        src="${pair%%:*}"
        dst="${pair##*:}"
        for skill in "$src"/*/; do
            if [ -d "$skill/webapp" ] && [ ! -L "$dst/$(basename "$skill")" ]; then
                 ln -s "${skill}webapp" "$dst/$(basename "$skill")"
            fi
        done
    done

    sudo chown $USERID:$GROUPID "$TARGET/webapp/WEB-INF/base"
    
    echo "**** Starting eme-server"
    export EMSERVER="${2:-$SCRIPT_DIR}"
    export EMSERVER="$(cd "$EMSERVER" && pwd)"
    ARGS_TEMPLATE="$EMELIB/resources/tomcat.args"

    if [ ! -f "$ARGS_TEMPLATE" ]; then
        echo "ERROR: $ARGS_TEMPLATE not found. Run: eme.sh init <server-path>" >&2
        exit 1
    fi

    # Java @argfile does not expand shell variables, so expand them here
    EXPANDED_ARGS=$( mktemp $TARGET/tomcat/work/tomcat-args.XXXXXX)
    sudo chmod 600 "$EXPANDED_ARGS"
    #trap " rm -f $EXPANDED_ARGS" EXIT
     sed -e "s|\$EMELIB|$EMELIB|g" -e "s|\$EMSERVER|$EMSERVER|g" "$ARGS_TEMPLATE" > "$EXPANDED_ARGS"

    JAVA="$JAVA_HOME/bin/java"

    echo "$JAVA $(cat "$EXPANDED_ARGS") org.apache.catalina.startup.Bootstrap start"

    #Run Tomcat as entermedia user
    #  "$JAVA" "@$EXPANDED_ARGS" org.apache.catalina.startup.Bootstrap start 

    
    CATALINA_BASE="$TARGET/tomcat"
    export CATALINA_BASE
    #SIGTERM-handler
    term_handler() {
        pid=$(pgrep -f "$CATALINA_BASE/conf/logging.properties")
        if [[ ! -z $pid ]]; then
            if [ $pid -ne 0 ]; then
                echo "Deployment shutdown start"
                 sh -c "$TARGET/tomcat/bin/catalina.sh stop"
                kill -SIGTERM "$catalinapid"
                while [ -e /proc/$pid ]; do
                    printf .
                    sleep 1
                done
            fi
        fi
        exit 143 # 128 + 15 -- SIGTERM
    }

    #SIGKILL
    # setup handlers
    # on callback, kill the last background process, which is `tail -f /dev/null` and execute the specified handler
    trap 'kill ${!}; term_handler' SIGTERM

    #Run application
    # sh -c "$EMTARGET/tomcat/bin/catalina.sh run" &

    "$JAVA" "@$EXPANDED_ARGS" org.apache.catalina.startup.Bootstrap start 
    
    catalinapid=0
    while [ $catalinapid -eq "0" ]; do
        catalinapid=$(pgrep -f "eme start")
        echo "Catalina PID: $catalinapid"
        sleep 1
    done

    wait $catalinapid
    echo "Tomcat process $catalinapid exited"
    ;;

  *)
    echo "Usage: eme.sh [init <new-server-path> | start [server-path]]" >&2
    exit 1
    ;;
esac
