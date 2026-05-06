FROM entermediadb/emubuntu:latest
LABEL maintainer="EnterMedia <help@entermediadb.org>"

#Environment
ENV CLIENT_NAME=entermedia
ENV INSTANCE_PORT=8080
ENV USERID=9009
ENV GROUPID=9009
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-18-openjdk-amd64


ARG CACHE_BREAKER=1
RUN  mkdir -p /usr/share/ && wget --output-document=/usr/share/eme-lib.tar.gz --no-cache -q https://dev.entermediadb.org/jenkins/view/EM12/job/eme-lib/lastSuccessfulBuild/artifact/deploy/eme-lib.tar.gz && \
#RUN mkdir -p /usr/share/ && wget --output-document=/usr/share/eme-lib.tar.gz --no-cache -q http://192.229.4.120:8080/eme-lib.tar.gz && \
    cd /usr/share/ && tar -xzf eme-lib.tar.gz && rm -rf eme-lib.tar.gz 

RUN cp /usr/share/eme-lib/resources/docker/delegates.xml /usr/local/share/ImageMagick-7/delegates.xml && \
    cp /usr/share/eme-lib/resources/docker/sysctl.conf /etc/sysctl.conf && \
    cp /usr/share/eme-lib/resources/docker/limits.conf /etc/security/limits.conf && \
    cp /usr/share/eme-lib/resources/docker/nproc.conf /etc/security/limits.d/20-nproc.conf

RUN rm -rf /tmp/* && \
    ln -s /usr/share/eme-lib/resources/eme.sh /usr/bin/eme && \
    chmod +x /usr/bin/eme


RUN mkdir -p /usr/share/eme-instance && chown ${USERID}:${GROUPID} /usr/share/eme-instance &&  /usr/bin/eme init

USER ${USERID}


RUN mkdir -p /usr/share/eme-instance && cd /usr/share/eme-instance &&  /usr/bin/eme init

WORKDIR /usr/share/eme-instance

CMD ["/usr/bin/eme", "start"]
