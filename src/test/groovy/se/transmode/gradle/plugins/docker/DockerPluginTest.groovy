/**
 * Copyright 2013 Transmode AB
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

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.*

class DockerPluginTest {

    private Project createProjectAndApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'docker'
        return project
    }

    @Test
    public void pluginAddsExtensionToProject() {
        Project project = createProjectAndApplyPlugin()

        assertTrue(project.docker instanceof DockerPluginExtension)
    }

    @Test
    public void taskTypeIsAvailableInPluginNamespace() {
        Project project = createProjectAndApplyPlugin()

        assertTrue(project.Docker == DockerTask.class)
    }

    @Test
    public void pluginInjectsTaskMaintainerFromExtension() {
        Project project = createProjectAndApplyPlugin()
        def testMaintainer = "PluginTest Maintainer"
        project.docker.maintainer = testMaintainer

        def task = project.task('docker', type: DockerTask)

        assertThat task.maintainer, is(equalTo(testMaintainer))
    }
}

