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

import org.gradle.api.Task
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class DockerTaskTest {

    private Project createProject() {
        return ProjectBuilder.builder().build()
    }

    private Task createDockerTask() {
        return createProject().task('docker', type: DockerTask)
    }

    @Test
    public void addTaskToProject() {
        def task = createDockerTask()
        assertTrue(task instanceof DockerTask)
    }

    @Test
    public void defineExposePort() {
        def task = createDockerTask()
        def port = 99
        task.exposePort(port)
        def dockerFile = task.buildDockerFile()
        assertTrue("EXPOSE ${port}" in dockerFile)
    }


}
