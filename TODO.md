# TODO

## Plugin
* Split plugin into a base- and a convention-plugin ("stacked plugin"):
  - docker-base adds task types DockerBuild, DockerRun, DockerPublish
  - docker imports docker-base and creates convention tasks such as distDocker
    (if application plugin used), replaces run with a DockerRun tasks, etc.
* Rename plugin to be accepted by gradle plugin page (e.g. se.transmode.docker)
  see: http://plugins.gradle.org/submit

## Convention plugin
* Check for applicationDistribution copySpec to determine if the project is an application.

## Base plugin
* addFiles should accept copySpec

## Docker
* Use the Java API client for Docker (https://github.com/docker-java/docker-java)
  - Currently docker-java does not support connecting to the docker host through UNIX sockets
    (see https://github.com/docker-java/docker-java#support-for-unix-sockets)
