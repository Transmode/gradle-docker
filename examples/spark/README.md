# Gradle Docker Plugin Example

This example is intended as an illustration on how to build runnable docker images with the gradle-docker plugin in conjunction with Gradle [Java](https://docs.gradle.org/current/userguide/java_plugin.html) and [Application](https://docs.gradle.org/current/userguide/application_plugin.html) plugins.

## Example Application
The source code in this example consists of a simple main method that starts a Jetty web server using [Spark](http://sparkjava.com/), serving static HTML.

## Requirements
- JDK 1.8
- Docker 0.6+

## Instructions
### 1. Create Docker Image
    ./gradlew distDocker

### 2. Run Image
Provide a port mapping. You will be able to access the website from your host via port 8080. Spark's default port is 4567.

    docker run -p 8080:4567 se.transmode/docker-spark:0.1

### 3. Connect to Website From Host
If you are on Windows or OS X, get the name of your Docker machine:

    docker-machine ls

Let's say your machine is called `my-default`, get the IP address of your machine:

    docker-machine ip my-default

Assuming the IP of the docker machine is `192.168.99.100`, point your host's browser to

    192.168.99.100:8080

## Troubleshooting
Restart your Docker machine with `docker-machine restart my-default` and then reinitialize Docker with `eval "$(docker-machine env my-default)"` if you get the following error:

    An error occurred trying to connect: Post http://%2F%2F.%2Fpipe%2Fdocker_engine/v1.23/build?buildargs=%7B%7D&cgroupparent=&cpuperiod=0&cpuquota=0&cpusetcpus=&cpusetmems=&cpushares=0&dockerfile=Dockerfile&labels=%7B%7D&memory=0&memswap=0&rm=1&shmsize=0&t=example%2Fappname%3A1.0&ulimits=null: open //./pipe/docker_engine: The system cannot find the file specified.

