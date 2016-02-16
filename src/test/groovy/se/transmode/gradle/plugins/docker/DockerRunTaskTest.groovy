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
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Answer;

import se.transmode.gradle.plugins.docker.client.DockerClient;
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.*;

class DockerRunTaskTest {
    
    public static class TestDockerRunTask extends DockerRunTask {
        
        DockerClient mockClient;
        
        TestDockerRunTask() {
            mockClient = mock(DockerClient.class)
        }

        @Override
        protected DockerClient getClient() {
            return mockClient;
        }
        
    }
    
    private static ArgumentMatcher<Collection<?>> isEmptyCollection = 
        new ArgumentMatcher<Collection<?>>() {
            
            @Override
            public boolean matches(Object collection) {
                return (collection != null) && ((Collection) collection).size() == 0;
            }
    
    }
        
    private static ArgumentMatcher<Collection<?>> isSingletonCollection = 
        new ArgumentMatcher<Collection<?>>() {
            
            @Override
            public boolean matches(Object collection) {
                return (collection != null) && ((Collection) collection).size() == 1;
            }
    
    }
        
    private static ArgumentMatcher<Map<?, ?>> isEmptyMap = 
        new ArgumentMatcher<Map<?, ?>>() {
            
            @Override
            public boolean matches(Object map) {
                return (map != null) && ((Map) map).size() == 0;
            }
    
    }
        
    private static ArgumentMatcher<Map<?, ?>> isSingletonMap = 
        new ArgumentMatcher<Map<?, ?>>() {
            
            @Override
            public boolean matches(Object map) {
                return (map != null) && ((Map) map).size() == 1;
            }
    
    }

    private static final String CONTAINER_NAME = 'mycontainer'
        
    private Project createProject() {
        Project project = ProjectBuilder.builder().build()
        // apply java plugin
        project.apply plugin: 'java'
        // add docker extension
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        // add docker run task configured to return a mock client
        project.task('dockerRunTask', type: TestDockerRunTask)
        return project
    }
    
    @Test
    public void addTaskToProject() {
        def task = ProjectBuilder.builder().build().task('dockerTask', type: DockerRunTask)
        assertTrue(task instanceof DockerRunTask)
    }
    
    @Test
    public void runDefault() {
        def project = createProject()
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runNamedContainer() {
        def project = createProject()
        project.dockerRunTask.containerName = CONTAINER_NAME
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(CONTAINER_NAME),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }

    @Test
    public void runDetached() {
        def project = createProject()
        project.dockerRunTask.detached = true
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(true), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runAutoRemove() {
        def project = createProject()
        project.dockerRunTask.autoRemove = true
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(true), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runWithEnv() {
        def project = createProject()
        project.dockerRunTask.env("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isSingletonMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runWithPorts() {
        def project = createProject()
        project.dockerRunTask.publish("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isSingletonMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runWithVolumes() {
        def project = createProject()
        project.dockerRunTask.volume("foo", "bar")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isSingletonMap), argThat(isEmptyCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runWithVolumesFrom() {
        def project = createProject()
        project.dockerRunTask.volumesFrom("foo")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isSingletonCollection), argThat(isEmptyCollection))
    }
    
    @Test
    public void runWithLinks() {
        def project = createProject()
        project.dockerRunTask.link("foo")
        project.dockerRunTask.run()
        verify(project.dockerRunTask.mockClient).run(anyString(), eq(null),
            eq(false), eq(false), argThat(isEmptyMap), argThat(isEmptyMap), 
            argThat(isEmptyMap), argThat(isEmptyCollection), argThat(isSingletonCollection))
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void runIllegalTaskState() {
        // Create project without the client mocked
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java'
        project.extensions.create(DockerPlugin.EXTENSION_NAME, DockerPluginExtension)
        project.task('dockerRunTask', type: DockerRunTask)
        
        // Configure the task to be both detached and auto-remove
        project.dockerRunTask.detached = true
        project.dockerRunTask.autoRemove = true
        project.dockerRunTask.run()
    }
}
