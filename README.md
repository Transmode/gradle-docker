# Gradle Docker plugin

[![Join the chat at https://gitter.im/Transmode/gradle-docker](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Transmode/gradle-docker?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/Transmode/gradle-docker.svg?branch=master)](https://travis-ci.org/Transmode/gradle-docker)
[![Coverage](https://codecov.io/gh/Transmode/gradle-docker/branch/master/graph/badge.svg)](https://codecov.io/gh/Transmode/gradle-docker) [ ![Download](https://api.bintray.com/packages/transmode/gradle-plugins/gradle-docker/images/download.png) ](https://bintray.com/transmode/gradle-plugins/gradle-docker/_latestVersion)

This plugin for [Gradle](http://www.gradle.org/) adds the capability to build and publish [Docker](http://docker.io/) images from the build script. It is available through [jCenter](https://bintray.com/transmode/gradle-plugins/gradle-docker/view) and  [MavenCentral](http://search.maven.org/#browse%7C566382288).

See the [change log](CHANGELOG.md) for information about the latest changes.

## Extending the application plugin
The gradle-docker plugin adds a task `distDocker` if the project already has the [application plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html) applied:

```gradle
apply plugin: 'application'
apply plugin: 'docker'
```

Executing the `distDocker` task builds a docker image containing all application files (libs, scripts, etc.) created by the `distTar` task from the application plugin. If you already use the application plugin to package your project then the docker plugin will add simple docker image building to your project.

By default `distDocker` uses a base image with a Java runtime according to the project's `targetCompatibility` property. The docker image entry point is set to the start script created by the application plugin. Checkout the [application example](examples/application/) project.

**Note**: The creation of the convention task `distDocker` is currently only supported for JVM based application projects. If you are not using a JVM based application, use the task type `Docker` directly to create a task to build Docker images of your application.


## The `Docker`task
The docker plugin introduces the task type `Docker`. A task of this type can be used to build and publish Docker images. See the [Dockerfile documentation](http://docs.docker.com/reference/builder/) for information about how Docker images are built.

In the following example we build a Docker image in our Gradle build script for the popular reverse proxy nginx. The image will be tagged with the name `foo/nginx`. The example is taken from the official Dockerfile [examples](http://docs.docker.com/reference/builder/#dockerfile-examples):

```gradle
apply plugin: 'docker'

buildscript {
    repositories { jcenter() }
    dependencies {
        classpath 'se.transmode.gradle:gradle-docker:1.2'
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
```

## Building your Dockerfile
In the example above the instructions on how to build the nginx Docker image are configured **inline** using methods of the Docker Gradle task. During task execution the plugin first creates a [Dockerfile](https://docs.docker.com/reference/builder/) which it then passes to Docker to build the image.

The available instructions are:

| Dockerfile instruction | Gradle task method |
| -----------------------|--------------------|
| `ADD`                  | `addFile(Closure copySpec)`
|                        | `addFile(String source, String dest)`
|                        | `addFile(File source, String dest)`
| `CMD`                  | `defaultCommand(List cmd)`
| `ENTRYPOINT`           | `entryPoint(List entryPoint)`
| `ENV`                  | `setEnvironment(String key, String val)`
| `EXPOSE`               | `exposePort(Integer port)`
|                        | `exposePort(String port)`
| `RUN`                  | `runCommand(String cmd)`
| `USER`                 | `switchUser(String userNameOrUid)`
| `VOLUME`               | `volume(String... paths)`
| `WORKDIR`              | `workingDir(String dir)`

Instead of defining the build instructions inline in the task it is also possible to supply an **external Dockerfile**. If the task property `dockerfile` is set to the path of an existing Dockerfile the plugin will use this file instead to build the image.

You can even combine these two methods: Supplying an external Dockerfile and extending it by defining instructions in the task. The build instructions from the external Dockerfile are read first and the instructions defined in the task appended. If an external Dockerfile is supplied, the `baseImage` property is ignored.

## Configuring the plugin
The plugin exposes configuration options on 2 levels: globally through a plugin extension and on a per task basis. The plugin tries to always set sensible defaults for all properties.

### Global configuration through plugin extension properties
Configuration properties in the plugin extension `docker` are applied to all Docker tasks. Available properties are:

 - `dockerBinary` - The path to the docker binary.
 - `baseImage` - The base docker image used when building images (i.e. the name after `FROM` in the Dockerfile).
 - `maintainer` - The name and email address of the image maintainer.
 - `registry` - The hostname and port of the Docker image registry unless the Docker Hub Registry is used.
 - `useApi` - Use the Docker Remote API instead of a locally installed `docker` binary. See [below](https://github.com/Transmode/gradle-docker/blob/master/README.md#docker-remote-api)

Example to set the base docker image and maintainer name for all tasks:

```gradle
docker {
    maintainer = 'John Doe <john.doe@acme.org>'
    baseImage = 'johndoe/nextgenjdk:9.0'
}
```

### Task configuration through task properties
All properties that are exposed through the plugin extension can be overridden in each task.
The image tag is constructed according to:

```gradle
tag = "${project.group}/${applicationName}:${tagVersion}"
```

Where:

 - `project.group` - This is a standard Gradle project property. If not defined, the `{project.group}/` is omitted.
 - `applicationName` - The name of the application being "dockerized".
 - `tagVersion` - Optional version name added to the image tag name. Defaults to `project.version` or "latest" if `project.version` is unspecified.

The following example task will tag the docker image as `org.acme/bar:13.0`:

```gradle
...
group = 'org.acme'
...
task fooDocker(type: Docker) {
    applicationName = 'foobar'
    tagVersion = '13.0'
}
```

### A note about base images ###
If no base image is configured through the extension or task property a suitable image is chosen based on the project's `targetCompatibility`. A project targeting Java 7 will for instance get a default base image with a Java 7 runtime.

## Docker Remote API
By default the plug-in will use the `docker` command line tool to execute any docker commands (such as `build` and `push`).  However, it can be configured to use the [Docker Remote API](https://docs.docker.com/reference/api/docker_remote_api/) instead via the `useApi` extension property:

```gradle
docker {
    useApi true
}
```

Use of the remote API requires that the Docker server be configured to listen over HTTP and that it have support for version 1.11 of the API (connecting over Unix Domain sockets is not supported yet).  The following configuration options are available:

* `hostUrl` - set the URL used to contact the Docker server.  Defaults to `http://localhost:2375`
* `apiUsername` - set the username used to authenticate the user with the Docker server.  Defaults to `nil` which means no authentication is performed.
* `apiPassword` - set the password used to authenticate the user with the Docker server.
* `apiEmail` - set the user's email used to authenticate the user with the Docker server.

For example:

```gradle
docker {
    useApi true
    hostUrl 'http://myserver:4243'
    apiUsername 'user'
    apiPassword 'password'
    apiEmail 'me@mycompany.com'
}
```


## Requirements
* Gradle 2.x
* Docker 0.11+

#### Note to Gradle 1.x users
The plugin is built with Gradle 2.x and thus needs version 2.0 or higher to work due to a newer version of Groovy included in Gradle 2.x (2.3 vs. 1.8.6). To use the plugin with Gradle 1.x you have to add Groovy's upward compatibility patch by adding the following line to your build file:

```gradle
buildscript {
    // ...
    dependencies {
         classpath 'se.transmode.gradle:gradle-docker:1.2'
         classpath 'org.codehaus.groovy:groovy-backports-compat23:2.3.5'
    }
}
```

#### Note to native docker client users
If you are not using Docker's remote API (`useApi = false`, i.e. the default behaviour) you need to have Docker installed locally in order to build images. However if the `dryRun` task property is set to `true`  all calls to Docker are disabled. In that case only the Dockerfile and its context directory will be created.
