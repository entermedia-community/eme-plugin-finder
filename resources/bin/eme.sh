#!/bin/bash -e

#set -x 
##This is run from the /bin/eme location that is linked

CMD="${1:-start}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ "$CMD" = "version" ]; then
    echo "eme-lib version: 0.1.0"
    exit 0
fi

TARGET="$2"

if [ -z "$TARGET" ]; then
    echo "No target path specified"
    exit 1
fi

mkdir -p "$TARGET"
cd  "$TARGET"
# Resolve EMELIB: prefer sibling eme-lib, then env var, then system default

#Make a function that I can pass in the number of levels to go up for the relative path and it return ../.. etc
function get_relative_emelib {
    local levels=$1
    local relative_path=""      
    for ((i=0; i<levels; i++)); do
        relative_path="../$relative_path"
    done
    relative_path="${relative_path}eme-lib"
    echo "$relative_path"
}

EMELIB="$(get_relative_emelib 1)"

if [ -d "$EMELIB" ]; then
    export EMELIB
else
    echo "ERROR: Cannot find eme-lib. $EMELIB" >&2
    exit 1
fi


##JAVA_HOME is not set throw an error if JAVA_HOME is not set
if [ -z "$JAVA_HOME" ]; then
    #checi if there is a jre path
    if [ -d "/usr/lib/jvm/jre" ]; then
        JAVA_HOME="/usr/lib/jvm/jre"
    elif [ -d "/usr/lib/jvm/default-java" ]; then
        JAVA_HOME="/usr/lib/jvm/default-java"
    else
        echo "JAVA_HOME is not set and /usr/lib/jvm/jre does not exist. Please set JAVA_HOME to a valid JRE path."
        echo "Please run iwth your local java version: sudo update-alternatives --install /usr/lib/jvm/jre jre /usr/lib/jvm/java-X.XX.X-openjdk-amd64 20000"
        exit 1
    fi
fi  

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
        echo "entermedia ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/entermedia
        chmod 0440 /etc/sudoers.d/entermedia
    fi

    JAVA_HOME="/usr/lib/jvm/java-18-openjdk-amd64"
    export JAVA_HOME

    sudo -u entermedia /usr/bin/eme start "$2"
    ;;

  init | start)    
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
    echo "**** Starting server from: $TARGET"

    if [ ! -d "$TARGET" ]; then
        sudo mkdir -p "$TARGET"
    fi    
    #check ownership of target, if not owned by current user, change ownership to current user
    if [ "$(stat -c '%u:%g' "$TARGET")" != "$USERID:$GROUPID" ]; then
        echo "Changing ownership of $TARGET to $USERID:$GROUPID"
        sudo chown "$USERID:$GROUPID" "$TARGET"
    fi  

    TARGET="$(cd "$TARGET" && pwd)"

    #$USER is the user running the container

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


    if [ ! -L "$TARGET/webapp/_site.xconf" ]; then
        mkdir -p "$TARGET/webapp/WEB-INF/"
        ln -nsf "$(get_relative_emelib 2)/resources/webapp/_site.xconf" "$TARGET/webapp/_site.xconf"
        sudo chown -R $USERID:$GROUPID "$TARGET/webapp"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/web.xml" ]; then
          cp -rp "$EMELIB/resources/webapp/WEB-INF/web.xml" "$TARGET/webapp/WEB-INF/web.xml"
    fi

    if [ ! -f "$TARGET/webapp/WEB-INF/node.xml" ]; then
          cp -rp "$EMELIB/resources/webapp/WEB-INF/node.xml" "$TARGET/webapp/WEB-INF/node.xml"
    fi

    if [ ! -L "$TARGET/webapp/WEB-INF/bin" ]; then
        ln -nsf "$(get_relative_emelib 3)/resources/webapp/WEB-INF/bin" "$TARGET/webapp/WEB-INF/bin"
    fi

 #   sudo chown ${USERID}:${GROUPID} "$TARGET/webapp/"
    if [ ! -L "$TARGET/data" ]; then
        mkdir -p "./webapp/WEB-INF/data"
        ln -nsf "./webapp/WEB-INF/data" "./data" 
        sudo chown -R $USERID:$GROUPID "$TARGET/data"
    fi

    if [ ! -d "$TARGET/data/system" ]; then
         mkdir -p "$TARGET/webapp/WEB-INF/data/system/"
         cp -rp "$EMELIB/plugins/system/defaultdata/." "$TARGET/webapp/WEB-INF/data/system/"
         sudo chown -R $USERID:$GROUPID "$TARGET/webapp/WEB-INF/data/system/"
    fi

    # symbolically link users plugins to webapp first!
    for plugin in "$TARGET/plugins"/*/; do
        pluginname="$(basename "$plugin")"
        if [ -d "${plugin}html" ]; then
            if [ ! -L "./webapp/$pluginname" ]; then
                ln -nsf "./plugins/${pluginname}/html" "./webapp/$pluginname"
            fi
        fi
    done

    # symbolically link built-in plugins from emelib to webapp, but only if they don't already exist in the target plugins directory (i.e. user overwrote them)
    #only do this if the emelib is relative
    for plugin in "$(get_relative_emelib 1)/plugins"/*/; do
        pluginname="$(basename "$plugin")"

        if [ -d "${plugin}html" ]; then
            ##if its an invalid symbolic link then remove it and create a new one
            echo "Adding plugin: $TARGET/webapp/$pluginname"
            if [ -L "$TARGET/webapp/$pluginname" ]; then
                echo "Removing invalid symbolic link: $TARGET/webapp/$pluginname"
                rm "$TARGET/webapp/$pluginname"
            fi
            ln -nsf "$(get_relative_emelib 2)/plugins/${pluginname}/html"  "$TARGET/webapp/$pluginname"
        fi
    done

    export EMSERVER="${2:-$SCRIPT_DIR}"
    export EMSERVER="$(cd "$EMSERVER" && pwd)"
    export EMSERVER_NAME="$(basename "$EMSERVER")"

    # Write VSCode configs

    mkdir -p "$TARGET/.vscode"
    sed -e "s|\$JAVA_HOME|$JAVA_HOME|g" "$EMELIB/resources/editor-configs/settings.json" > "$TARGET/.vscode/settings.json"

    sed -e "s|\$EMSERVER_NAME|$EMSERVER_NAME|g" "$EMELIB/resources/editor-configs/launch.json" > "$TARGET/.vscode/launch.json"
    printf "// Do not edit this file unless you know what you are doing\n" | cat - "$TARGET/.vscode/launch.json" > "$TARGET/.vscode/temp" && mv "$TARGET/.vscode/temp" "$TARGET/.vscode/launch.json"

    cp "$EMELIB/resources/editor-configs/formatter.xml" "$TARGET/formatter.xml"

    sed -e "s|\$EMELIB|$EMELIB|g" -e "s|\$EMSERVER|$EMSERVER|g" "$EMELIB/resources/editor-configs/eme.code-workspace" > "$TARGET/$EMSERVER_NAME.code-workspace"

    if command -v code >/dev/null 2>&1; then
        code "$TARGET/$EMSERVER_NAME.code-workspace" && echo "Launching VS Code, press F5 to start your server" && exit 0
    else
        echo "VS Code 'code' command not found. Open $TARGET/$EMSERVER_NAME.code-workspace manually."
    fi



    ARGS_TEMPLATE="$EMELIB/resources/bin/tomcat.args"

    echo "**** Starting $EMSERVER_NAME using JAVA_HOME  = $JAVA_HOME"


    if [ ! -f "$ARGS_TEMPLATE" ]; then
        echo "ERROR: $ARGS_TEMPLATE not found. Run: eme.sh init <server-path>" >&2
        exit 1
    fi

    if( $CMD = "init" ); then
        echo "Initialization complete. Run: eme.sh start <server-path> to start the server."
        exit 0
    fi  

    # Java @argfile does not expand shell variables, so expand them here
    EXPANDED_ARGS=$( mktemp $TARGET/tomcat/work/tomcat-args.XXXXXX)
    sudo chmod 600 "$EXPANDED_ARGS"
    trap " rm -f $EXPANDED_ARGS" EXIT
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
    while [ $catalinapid -eq 0 ]; do
        catalinapid=$(pgrep -f "eme start")
        ##make sure its an integer not a string
        catalinapid=$(echo "$catalinapid" | awk '{print int($0)}')
        echo "Catalina PID: $catalinapid"
        sleep 1
    done

    wait $catalinapid
    echo "Tomcat process $catalinapid exited"
    ;;

  *)
        echo "Usage: eme.sh [version | dockerstart <server-path> | start [server-path]]" >&2
    exit 1
    ;;
esac
