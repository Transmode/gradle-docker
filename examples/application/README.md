# Gradle Docker Plugin Example

This example is intended as an illustration on how to build runnable docker images with the gradle-docker plugin in conjunction with Gradle Java and Application plugins.

## Example Application
The source code in this example consists of a simple main method that starts a jetty webserver serving static html.

## Requirements
- JDK 1.7
- Docker 0.6+

## Instructions

    ./gradlew distDocker

