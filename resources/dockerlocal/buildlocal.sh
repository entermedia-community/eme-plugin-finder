#!/bin/bash -x

DOCKERIMAGE=eme-lib-local  
DOCKERNAME=emubuntutest
BRANCH=latest
DOCKERNETWORK=entermedia

sudo docker stop $DOCKERNAME
sudo docker rm $DOCKERNAME	


	
sudo docker image rm $DOCKERIMAGE
#sudo docker system prune
set -x

cp '../../deploy/eme-lib.tar.gz' eme-lib.tar.gz
sudo docker build -t $DOCKERIMAGE .


# Pull latest images


IP_ADDR="172.18.0.$NODENUMBER"

ENDPOINT=../../../eme-server-test

rm -rf "$ENDPOINT/*"

USERID=$(id -u)
GROUPID=$(id -g)


# Create custom scripts
SCRIPTROOT=${ENDPOINT}/bin

echo "sudo docker start $INSTANCE" > ${SCRIPTROOT}/start.sh
echo "sudo docker stop -t 60 $INSTANCE" > ${SCRIPTROOT}/stop.sh
echo "sudo docker stop -t 60 $INSTANCE && sudo docker start $INSTANCE" > ${SCRIPTROOT}/restart.sh
echo "sudo docker logs -f --tail 500 $INSTANCE"  > ${SCRIPTROOT}/logs.sh
echo "sudo docker exec -it $INSTANCE bash"  > ${SCRIPTROOT}/bash.sh
echo "sudo bash $SCRIPTROOT/eme-docker-init.sh $SITE $NODENUMBER" > ${SCRIPTROOT}/rebuild.sh
echo 'sudo docker exec -it -u 0 '$INSTANCE' /usr/share/eme-lib/resources/docker/eme-docker-update.sh $1 $2' > ${SCRIPTROOT}/update.sh


sudo docker run -t -d \
	--restart unless-stopped \
	--name emubuntutest \
	-e USERID=$USERID \
	-e GROUPID=$GROUPID \
	--log-opt max-size=10m --log-opt max-file=10 \
	--cap-add=SYS_PTRACE \
	-e TZ="America/New_York" \
	-v ${ENDPOINT}/:/usr/share/eme-instance \
	$DOCKERIMAGE \
	/usr/bin/bash

#	/usr/bin/eme start /usr/share/eme-instance

#sudo docker exec -it emubuntutest bash
sudo docker exec -it emubuntutest /usr/bin/eme dockerstart /usr/share/eme-instance