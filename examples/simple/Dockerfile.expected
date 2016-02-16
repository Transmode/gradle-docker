FROM debian:jessie
RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
RUN apt-get update
RUN apt-get install -y nginx
ADD add_4.tar /
MAINTAINER John Doe <john@doe.com>
