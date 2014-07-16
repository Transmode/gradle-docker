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
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.nullValue
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class DockerTaskTest {

    private Project createProject() {
        Project project = ProjectBuilder.builder().build()
        // apply java plugin
        project.apply plugin: 'java'
        // add docker extension
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        // add docker task
        project.task('dockerTask', type: DockerTask)
        return project
    }

    @Test
    public void addTaskToProject() {
        def task = ProjectBuilder.builder().build().task('dockerTask', type: DockerTask)
        assertTrue(task instanceof DockerTask)
    }

    @Test
    public void defineExposePort() {
        def project = createProject()
        project.dockerTask.exposePort(99)
        assertTrue("EXPOSE ${99}" in project.dockerTask.buildDockerFile())
    }

    @Test
    public void nonJavaDefaultBaseImage() {
        def project = ProjectBuilder.builder().build()
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        def task = project.task('dockerTask', type: DockerTask)
        assertThat(task.baseImage, equalTo(DockerTask.DEFAULT_IMAGE))
    }

    @Test
    public void overrideBaseImageInExtension() {
        def project = createProject()
        project[DockerPlugin.EXTENSION_NAME].baseImage = "extensionBase"
        assertThat(project.dockerTask.baseImage, equalTo("extensionBase"))
    }

    @Test
    public void overrideBaseImageInTask() {
        def project = createProject()
        project.dockerTask.baseImage = "taskBase"
        assertThat(project.dockerTask.baseImage, equalTo("taskBase"))
    }

    @Test
    public void determineBaseImageFromTargetCompatibilityIfNotOverriden() {
        def project = createProject()
        def testVersion = JavaVersion.VERSION_1_6
        project.targetCompatibility = testVersion
        assertThat(project[DockerPlugin.EXTENSION_NAME].baseImage, nullValue())
        assertThat(project.dockerTask.baseImage,
                equalTo(JavaBaseImage.imageFor(testVersion).imageName))
    }


}
