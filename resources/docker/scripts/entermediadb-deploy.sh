#!/bin/bash -x

# Deploy entermediadb 
# emubuntu scripts/entermediadb-deploy.sh
# TODO: change parameters to only rely on NODE ID instead of client name and instance port
# Variables CLIENT_NAME and INSTANCE_PORT should be coming from Docker ENV

EMELIB=/usr/share/eme-lib
EMSERVER=/opt/entermediadb
WEBAPP=$EMSERVER/webapp

#Finish install
if [[ ! $(id -u) -eq 0 ]]; then
	echo You must run this script as a superuser.
	exit 1
fi
if [[ ! $(id -u entermedia 2>/dev/null) ]]; then
	groupadd -g $GROUPID entermedia
	useradd -ms /bin/bash entermedia -g entermedia -u $USERID
fi

if [[ ! -f $WEBAPP/index.html ]]; then
	echo "Initial Deploy to $WEBAPP"

	mkdir -p $WEBAPP

	##TODO Use symbolic links using new eme tool. eme -initialize  .
	rsync -ar --chown=entermedia:entermedia $EMELIB/webapp/ $WEBAPP/
else
	#In case they got removed docker will re-create them but owned by root (Not -R to avoid slow restarts)
	chown entermedia:entermedia $WEBAPP/WEB-INF/data
	chown entermedia:entermedia $WEBAPP/WEB-INF/elastic
	chown entermedia:entermedia /tmp
	chown -R entermedia:entermedia $WEBAPP/WEB-INF/base
	chown -R entermedia:entermedia $WEBAPP/WEB-INF/lib
fi

if [[ ! -d $WEBAPP/WEB-INF/data ]]; then
	mkdir -p $WEBAPP/WEB-INF/data
	chown -R entermedia:entermedia $WEBAPP/WEB-INF/data
fi

if [[ ! -d $WEBAPP/WEB-INF/data/system ]]; then
	rsync -ar $EMELIB/webapp/WEB-INF/data/system $WEBAPP/WEB-INF/data/
	chown -R entermedia:entermedia $WEBAPP/WEB-INF/data/system
fi

#Always replace the base and lib folders on new container
if [[ ! -d $WEBAPP/WEB-INF/base ]]; then
	rsync -ar --delete --chown=entermedia:entermedia --exclude '/WEB-INF/data' --exclude '/WEB-INF/encrypt.properties' --exclude '/WEB-INF/pluginoverrides.xml' --exclude '/WEB-INF/classes' --exclude '/WEB-INF/elastic' $EMELIB/webapp/WEB-INF $WEBAPP/
fi

#Rotate Logs
if [[ ! -f /etc/logrotate.d/tomcat_$CLIENT_NAME ]]; then
	cp $EMELIB/resources/logrotate.conf /etc/logrotate.d/tomcat_$CLIENT_NAME
fi

#Always upgrade
rsync -ar --delete --chown=entermedia:entermedia $EMELIB/webapp/WEB-INF/bin $WEBAPP/WEB-INF/
rsync -a --chown=entermedia:entermedia $EMELIB/webapp/WEB-INF/web.xml $WEBAPP/WEB-INF/web.xml
rsync -a --chown=entermedia:entermedia $EMELIB/conf/im/ /usr/local/etc/ImageMagick-7

#Make links and copy stuff
if [[ ! -d $EMSERVER/tomcat/conf ]]; then
	mkdir -p "$EMSERVER/tomcat"/{logs,temp}
	cp -rp "$EMELIB/tomcat/conf" "$EMSERVER/tomcat"
	cp -rp "$EMELIB/tomcat/bin" "$EMSERVER/tomcat"
	echo "export CATALINA_BASE=\"$EMSERVER/tomcat\"" >>"$EMSERVER/tomcat/bin/setenv.sh"
	sed "s/%PORT%/8080/g;s/%NODE_ID%/${CLIENT_NAME}${INSTANCE_PORT}/g" <"$EMELIB/tomcat/conf/server.xml.cluster" >"$EMSERVER/tomcat/conf/server.xml"
	sed "s/%NODE_ID%/${CLIENT_NAME}${INSTANCE_PORT}/g" <"$EMELIB/tomcat/bin/catalina.sh.cluster" >"$EMSERVER/tomcat/bin/catalina.sh"
	sed "s/%CLUSTER_NAME%/${CLIENT_NAME}-cluster/g" <"$EMELIB/conf/node.xml.cluster" >"$EMSERVER/tomcat/conf/node.xml"
	chmod 755 "$EMSERVER/tomcat/bin/*.sh"
	chown -R entermedia:entermedia $EMSERVER/tomcat
else
	#Deletes all logs
	rsync -ar --delete --chown=entermedia:entermedia --exclude '/tomcat/conf/server.xml' --exclude '/tomcat/logs/*' --exclude '/tomcat/conf/node.xml' $EMELIB/tomcat $EMSERVER/
	mkdir -p "$EMSERVER/tomcat"/{logs,temp}
	chown -R entermedia:entermedia $EMSERVER/tomcat
fi

#Remove old node.xml and link new one
rm $WEBAPP/WEB-INF/node.xml
ln -s $EMSERVER/tomcat/conf/node.xml $WEBAPP/WEB-INF/node.xml


if [ ! -f /media/services/startup.sh ]; then
	wget -O /media/services/startup.sh https://raw.githubusercontent.com/entermedia-community/entermediadb-docker/master/scripts/startup.sh
	chmod +x /media/services/startup.sh
fi

# Execute arbitrary scripts if provided
if [[ -d /media/services ]]; then
	echo ""
	echo "Executing script /media/services/$script"
	for script in $(ls /media/services/*.sh); do
		bash $script
	done
fi

# Execute arbitrary scripts if provided
rm -rf /tmp/unpacked
sudo -u entermedia /usr/bin/entermediadb-extensions-deploy.sh

#Run command
echo Starting EnterMedia ...
pid=0

#SIGTERM-handler
term_handler() {
	pid=$(pgrep -f "$CATALINA_BASE/conf/logging.properties")
	if [[ ! -z $pid ]]; then
		if [ $pid -ne 0 ]; then
			echo "Deployment shutdown start"
			sudo -u entermedia sh -c "$EMSERVER/tomcat/bin/catalina.sh stop"
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
sudo -u entermedia sh -c "$EMSERVER/tomcat/bin/catalina.sh run" &
catalinapid=0
while [ $catalinapid -eq "0" ]; do
	catalinapid=$(pgrep -f "$EMSERVER/tomcat/bin/catalina.sh run")
	sleep 1
done

wait $catalinapid
