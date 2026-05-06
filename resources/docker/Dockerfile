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


ARG CACHE_BREAKER=1
RUN mkdir /tmp/untar && cd /tmp/untar/ && wget --no-cache -q http://dev.entermediadb.org/jenkins/view/EM12/job/eme-lib/lastSuccessfulBuild/artifact/deploy/eme-lib.tar.gz && \
    cd /tmp/untar/ && tar -xzf *.gz && \
    mv /tmp/untar/eme-lib /usr/share/ && \
    cp /usr/share/eme-lib/resources/docker/delegates.xml /usr/local/share/ImageMagick-7/delegates.xml && \
    cp /usr/share/eme-lib/resources/docker/sysctl.conf /etc/sysctl.conf && \
    cp /usr/share/eme-lib/resources/docker/limits.conf /etc/security/limits.conf && \
    cp /usr/share/eme-lib/resources/docker/nproc.conf /etc/security/limits.d/20-nproc.conf && \
    rm -rf /tmp/* && \
    ln -s /usr/share/eme-lib/resources/eme.sh /usr/bin/eme 

USER entermedia
RUN mkdir -p /usr/share/eme-instance && cd /usr/share/eme-instance && /usr/bin/eme init
WORKDIR /usr/share/eme-instance
CMD ["/usr/bin/eme", "start"]
