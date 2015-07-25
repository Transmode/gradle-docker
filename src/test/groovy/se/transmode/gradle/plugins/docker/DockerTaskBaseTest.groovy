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

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Matchers
import org.junit.Test

import se.transmode.gradle.plugins.docker.client.DockerClient
import se.transmode.gradle.plugins.docker.client.JavaDockerClient;
import se.transmode.gradle.plugins.docker.client.NativeDockerClient

class DockerTaskBaseTest {

    private static final String PROJECT_GROUP = 'mygroup'
    private static final String REGISTRY = 'myregistry'
    private static final String PROJECT_VERSION = 'myversion'
    private static final String TAG_VERSION = 'tagVersion'
    
    // Create dummy task sub-class so we can test the functionality provided by the super-class
    public static class DummyTask extends DockerTaskBase {
        
    }
    
    private Project createProject() {
        Project project = ProjectBuilder.builder().build()
        // apply java plugin
        project.apply plugin: 'java'
        // add docker extension
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        // add dummy task
        project.task('dummyTask', type: DummyTask)
        project.dummyTask.dockerBinary = 'true'
        return project
    }

    @Test
    public void getNativeClient() {
        def project = createProject()
        DockerClient client = project.dummyTask.getClient()
        assertThat(client, isA(NativeDockerClient.class))
    }

    @Test
    public void getJavaClient() {
        def project = createProject()
        project.dummyTask.useApi = true
        DockerClient client = project.dummyTask.getClient()
        assertThat(client, isA(JavaDockerClient.class))
    }
    
    @Test
    public void getImageTag() {
        def project = createProject()

        // Check the default value
        def imageTag = project.dummyTask.imageTag
        assertThat(imageTag, 
            equalToIgnoringCase("${project.name}:${DockerTaskBase.LATEST_VERSION}"))
        
        // A project group should be added to the name
        project.group = PROJECT_GROUP
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
            equalToIgnoringCase("${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST_VERSION}"))

        // If we set the registry that takes precedence
        project.group = null
        project.dummyTask.registry = REGISTRY
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
            equalToIgnoringCase("${REGISTRY}/${project.name}:${DockerTaskBase.LATEST_VERSION}"))

        // if we set registry and group
        project.group = PROJECT_GROUP
        project.dummyTask.registry = REGISTRY
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
                equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST_VERSION}"))

        // If the project has a version that should be used
        project.version = PROJECT_VERSION
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
            equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${PROJECT_VERSION}"))
        
        // If we set an override version that should be used

        project.dummyTask.tagVersion = TAG_VERSION
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
            equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${TAG_VERSION}"))
        
        // Explicitly setting version to latest should use that
        project.dummyTask.setTagVersionToLatest()
        imageTag = project.dummyTask.imageTag
        assertThat(imageTag,
            equalToIgnoringCase("${REGISTRY}/${PROJECT_GROUP}/${project.name}:${DockerTaskBase.LATEST_VERSION}"))
        
    }
}
