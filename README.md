# Gradle Docker plugin

This plugin for [Gradle](http://www.gradle.org/) adds the capability to build und publish [Docker](http://docker.io/) images from the build script.

[ ![Download](https://api.bintray.com/packages/transmode/gradle-plugins/gradle-docker/images/download.png) ](https://bintray.com/transmode/gradle-plugins/gradle-docker/_latestVersion)

## Extending the application plugin
The gradle-docker plugin adds a task `distDocker` if the project already has the [application plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html) applied:

    apply plugin: 'application'
    apply plugin: 'docker'

Executing the `distDocker` task builds a docker image containing all application files (libs, scripts, etc.) created by the `distTar` task from the application plugin. If you already use the application plugin to package your project then the docker plugin will add simple docker image building to your project.

By default `distDocker` uses a base image with a Java runtime according to the project's `targetCompatibility` property. The docker image entry point is set to the start script created by the application plugin. Checkout the [example](example/) project.

*Note: Only JVM based projects are supported.*


## Stand-alone
The docker plugin introduces the task type `Docker`. A task of this type can be used to build Docker images. See the [Dockerfile documentation](http://docs.docker.io/en/latest/use/builder/) for information about how docker containers are built.

The following example builds a docker image for the popular reverse proxy nginx. The image will be tagged with the name `foo/nginx`. The example is taken from the official Dockerfile [examples](http://docs.docker.io/en/latest/use/builder/#dockerfile-examples):


    apply plugin: 'docker'

    buildscript {
        repositories { mavenCentral() }
        dependencies {
            classpath 'se.transmode.gradle:gradle-docker:1.1.1'
        }
    }
    
    group = "foo"
    
    docker {
        baseImage "ubuntu"
        maintainer 'Guillaume J. Charmes "guillaume@dotcloud.com"'
    }

    task nginxDocker(type: Docker) {
        applicationName = "nginx"
        runCommand 'echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list'
        runCommand "apt-get update"
        runCommand "apt-get install -y inotify-tools nginx apache2 openssh-server"
    }
    
    
## Requirements
* Docker 0.6+

You need to have docker installed in order to build docker images. However if the `dryRun` task property is set to `true`  all calls to docker are disabled. In that case only the Dockerfile and its context directory will be created.

