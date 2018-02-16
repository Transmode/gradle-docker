/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.junit.Assert.assertThat


class CreateDockerfileTest {
    @Test
    public void compareDockerfileBuiltOldstyle() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'docker'

        def task = project.task('docker', type: DockerTask)

        // don't actually execute docker, just build Dockerfile
        task.dockerBinary = '/bin/true'

        // example pulled from "http://docs.docker.io/en/latest/use/builder/#dockerfile-examples"
        task.baseImage 'ubuntu'
        task.maintainer 'Guillaume J. Charmes "guillaume@dotcloud.com"'
        task.label (maintainer:'Guillaume J. Charmes guillaume@dotcloud.com', version:'1.2.3')

        task.runCommand 'echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list'
        task.runCommand 'apt-get update'

        task.runCommand 'apt-get install -y inotify-tools nginx apache2 openssh-server'

        task.addInstruction "COPY", "tmp/file1"

        task.volume "/home/docker", "/tmp"

        task.defaultCommand(["/bin/bash"])

        def expected = []
        this.getClass().getClassLoader().getResource("nginx.Dockerfile").eachLine { expected << it.toString() }
        def actual = task.buildDockerfile().instructions.each { it.toString() }

        assertThat actual[0].toString(), equalToIgnoringCase('from ubuntu')
        assertThat actual, containsInAnyOrder(*expected)
    }

    @Test
    public void compareDockerfileBuiltWithDSL() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'docker'

        def task = project.task('docker', type: DockerTask)

        // don't actually execute docker, just build Dockerfile
        task.dockerBinary = "/bin/true"

        task.maintainer 'Guillaume J. Charmes "guillaume@dotcloud.com"'
        task.dockerfile {
            from 'ubuntu'
            label (maintainer:"Guillaume J. Charmes guillaume@dotcloud.com", version:"1.2.3")
            run 'echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list'
            run 'apt-get update'
            run 'apt-get install -y inotify-tools nginx apache2 openssh-server'
            copy "tmp/file1"
            volume "/home/docker", "/tmp"
            cmd(['/bin/bash'])
        }

        def expected = []
        this.getClass().getClassLoader().getResource("nginx.Dockerfile").eachLine { expected << it.toString() }
        def actual = task.buildDockerfile().instructions.each { it.toString() }

        assertThat actual[0], equalToIgnoringCase('from ubuntu')
        assertThat actual, containsInAnyOrder(*expected)
    }
}
