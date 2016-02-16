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

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction;

import se.transmode.gradle.plugins.docker.client.DockerClient

class DockerRunTask extends DockerTaskBase {
    private static Logger logger = Logging.getLogger(DockerRunTask)
    
    String containerName
    
    boolean detached = false

    boolean autoRemove = false
    
    Map<String, String> env
    
    Map<String, String> ports
    
    Map<String, String> volumes
    
    List<String> volumesFrom
    
    List<String> links
    
    DockerRunTask() {
        env = [:]
        ports = [:]
        volumes = [:]
        volumesFrom = []
        links = []
    }
    
    @TaskAction
    public void run() {
        DockerClient client = getClient()
        client.run(getImageTag(), getContainerName(), getDetached(), getAutoRemove(), getEnv(), 
            getPorts(), getVolumes(), getVolumesFrom(), getLinks())
    }
    
    void env(String key, String value) {
        env.put(key, value)
    }
    
    void publish(String host, String container) {
        ports.put(host, container)
    }
    
    void volume(String host, String container) {
        volumes.put(host, container)
    }
    
    void volumesFrom(String containerName) {
        volumesFrom.add(containerName)
    }
    
    void link(String containerName) {
        links.add(containerName)
    }
}
