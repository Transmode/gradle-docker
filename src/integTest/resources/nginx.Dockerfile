FROM ubuntu
MAINTAINER Guillaume J. Charmes "guillaume@dotcloud.com"
LABEL "maintainer"="Guillaume J. Charmes guillaume@dotcloud.com" "version"="1.2.3"
RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
RUN apt-get update
RUN apt-get install -y inotify-tools nginx apache2 openssh-server
COPY file1 /
VOLUME /home/docker /tmp
CMD ["/bin/bash"]
