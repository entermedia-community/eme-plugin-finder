#!/bin/bash

DOCKERIMAGE=eme-lib-local  
DOCKERNAME=emubuntutest
BRANCH=latest
DOCKERNETWORK=entermedia

sudo docker stop $DOCKERNAME
sudo docker rm $DOCKERNAME	
	
sudo docker image rm $DOCKERIMAGE
#sudo docker system prune
set -x

cp '../../../deploy/eme-lib.tar.gz' eme-lib.tar.gz
sudo docker build -t $DOCKERIMAGE .


# Pull latest images


IP_ADDR="172.18.0.$NODENUMBER"

ENDPOINT=../../../eme-server-test

USERID=$(id -u)
GROUPID=$(id -g)


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