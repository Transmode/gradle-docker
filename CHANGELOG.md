# Changelog

## Version 1.2 (2014-07-28)

#### Docker Remote API
It is now possible to use the Docker Remote API instead of the `docker` command line tool. See the [docs](README.md#docker-remote-api) for more information (PR #10). This is particularly useful for users who do not have Docker installed locally.

#### addFile
* Fixed `addFile` accepting a copySpec as an argument (issue #4).
* `addFile` now accepts the destination path as an optional second argument (default: `/`)

#### Base image
* Fixed setting of a custom base image both through the plugin extension or a task property (issue #11).
* Fixed default base image detection based on project's `targetCompatibility`.
* Added default base image for Java 8 (PR #9).

#### External Dockerfile
* Supply an external Dockerfile instead of defining it in the build script. See the [docs](README.md#building-your-dockerfile) (issue #13).
* Mix and match loading external Dockerfiles and extending in the build script.

#### Gradle 2.0
The plugin is now compatible with Gradle 2.0 (see the [docs](README.md#note-to-gradle-1.x-users) if you are using Gradle 1.x)

#### Image tagging
* Possible to set docker image tag version to something else than *:latest* (PR #5).
* Fixed setting of the image tag name and version (issue #15).

Many thanks to the contributors

* [@aglover](https://github.com/aglover)
* [@Teudimundo](https://github.com/Teudimundo)
* [@sfitts](https://github.com/sfitts)
* [@frvi](https://github.com/frvi)
* [@mattgruter](https://github.com/mattgruter)


## Version 1.1.1 (2014-06-13)
* Possible to build without specifying group.
* Failing gradle build if Docker execution fails.

Many thanks to the contributors:

* [@Teudimundo](https://github.com/Teudimundo)
* [@frvi](https://github.com/frvi)


## Version 1.1 (2014-04-11)
* Support for more Dockerfile commands:
  - WORKDIR
  - VOLUME
  - ENV
* Add any raw Dockerfile command.
* Set path to docker binary per task or globally.
* Switched to official dockerfile/java base image as default.
* Fixed path seperator bug for integration testing on Windows.

Many thanks to the contributors:

* [@sfitts](https://github.com/sfitts)
* [@kernel164](https://github.com/kernel164)
* [@nicarlsson](https://github.com/nicarlsson)
* [@mattgruter](https://github.com/mattgruter)


## Version 1.0 (2013-12-16)
Initial public release
* Task type "Docker" to:
  - create Dockerfiles
  - build a Docker image from above Dockerfile
  - push the Docker image to the privat or public index
* Convention task "distDocker" to build an image from the gradle applicationDistribution
* Plugin extension to apply configuration to all Docker tasks
