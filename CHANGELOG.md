# Changelog

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
