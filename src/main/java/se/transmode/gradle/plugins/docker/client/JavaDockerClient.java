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
package se.transmode.gradle.plugins.docker.client;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.github.dockerjava.client.NotFoundException;
import com.github.dockerjava.client.command.CreateContainerCmd;
import com.github.dockerjava.client.command.StartContainerCmd;
import com.github.dockerjava.client.model.Bind;
import com.github.dockerjava.client.model.ContainerCreateResponse;
import com.github.dockerjava.client.model.Link;
import com.github.dockerjava.client.model.Volume;
import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.ClientResponse;

public class JavaDockerClient extends com.github.dockerjava.client.DockerClient implements DockerClient {

    private static Logger log = Logging.getLogger(JavaDockerClient.class);

    JavaDockerClient() {
        super();
    }

    JavaDockerClient(String url) {
        super(url);
    }

    @Override
    public String buildImage(File buildDir, String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        ClientResponse response = buildImageCmd(buildDir).withTag(tag).exec();
        return checkResponse(response);
    }

    @Override
    public String pushImage(String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        ClientResponse response = pushImageCmd(tag).exec();
        return checkResponse(response);
    }

    private static String checkResponse(ClientResponse response) {
        String msg = response.getEntity(String.class);
        if (response.getStatusInfo() != ClientResponse.Status.OK) {
            throw new GradleException(
                    "Docker API error: Failed to build Image:\n"+msg);
        }
        return msg;
    }

    public static JavaDockerClient create(String url, String user, String password, String email) {
        JavaDockerClient client;
        if (StringUtils.isEmpty(url)) {
            log.info("Connecting to localhost");
            // TODO -- use no-arg constructor once we switch to java-docker 0.9.1
            client = new JavaDockerClient("http://localhost:2375");
        } else {
            log.info("Connecting to {}", url);
            client = new JavaDockerClient(url);
        }

        if (StringUtils.isNotEmpty(user)) {
            client.setCredentials(user, password, email);
        }

        return client;
    }

    @Override
    public String run(String tag, String containerName, boolean detached, boolean autoRemove,
            Map<String, String> env, Map<String, String> ports, Map<String, String> volumes, 
            List<String> volumesFrom, List<String> links) {
        
        Preconditions.checkArgument(!StringUtils.isEmpty(tag),  
                "Image tag cannot be empty or null.");
        Preconditions.checkArgument(env != null,  "Environment map cannot be null.");
        Preconditions.checkArgument(ports != null,  "Exported port map cannot be null.");
        Preconditions.checkArgument(volumes != null,  "Volume map cannot be null.");
        Preconditions.checkArgument(volumesFrom != null,  "Volumes from list cannot be null.");
        Preconditions.checkArgument(links != null,  "Link list cannot be null.");
        Preconditions.checkArgument(!detached || !autoRemove, 
                "Cannot set both detached and autoRemove options to true.");
        
        // Start by creating the container
        CreateContainerCmd createCmd = createContainerCmd(tag).withName(containerName);
        String[] envList = new String[env.size()];
        int index = 0;
        for (Entry<String, String> entry : env.entrySet()) {
            String envSetting = String.format("%s=%s", entry.getKey(), entry.getValue());
            envList[index++] = envSetting;
        }
        createCmd.withEnv(envList);
        ContainerCreateResponse createResponse;
        try {
            createResponse = createContainerCmd(tag).exec();
        } catch (NotFoundException nfe) {
            // TODO have option to pull image
            throw nfe;
        }
        String containerId = createResponse.getId();
        
        // Configure start command
        StartContainerCmd startCmd = startContainerCmd(containerId);
        Bind[] binds = new Bind[volumes.size()];
        index = 0;
        for (Entry<String, String> entry : volumes.entrySet()) {
            Volume vol = new Volume(entry.getValue());
            Bind bind = new Bind(entry.getKey(), vol);
            binds[index++] = bind;
        }
        startCmd.withBinds(binds);
        startCmd.withVolumesFrom(StringUtils.join(volumesFrom, ","));
        Link[] linkArr = new Link[links.size()];
        index = 0;
        for (String linkStr : links) {
            String[] values = linkStr.split(":");
            Link link = new Link(values[0], values.length == 2 ? values[1] : values[0]);
            linkArr[index++] = link;
        }
        startCmd.withLinks(linkArr);
        
        // Start the container
        try {
            startCmd.exec();
        } catch (Exception e) {
            // Want to get rid of container we created
            removeContainerCmd(containerId).exec();
        }
       
        // Should we wait around and/or remove the container on exit
        if (autoRemove) {
            return removeOnExit(containerId);
        } else if (detached) {
            return containerId;
        } else {
            return waitForExit(containerId);
        }
    }
    
    private String removeOnExit(String containerId) {
        String exitStatus = waitForExit(containerId);
        removeContainerCmd(containerId).exec();
        return exitStatus;
    }

    private String waitForExit(String containerId) {
        // TODO -- show container output if/when we get that option from docker-java
        return "Exit status: " + waitContainerCmd(containerId).exec();
    }
}
