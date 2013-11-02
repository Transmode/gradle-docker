# Gradle Docker Plugin Example

The example in this folder is intended as an illustration on how to build runnable docker images with the gradle-docker plugin.

## Example Application
The source code in this example consists of a simple main method that starts a jetty webserver to serves static html.

## Requirements
- JDK 1.7
- Docker 0.6+

## Instructions

    cd example/
    ./gradlew distDocker

