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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.google.common.base.Preconditions;

public class JavaDockerClient implements DockerClient {

    private static Logger log = Logging.getLogger(JavaDockerClient.class);

    private com.github.dockerjava.api.DockerClient dockerClient;

    JavaDockerClient(com.github.dockerjava.api.DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public static JavaDockerClient create(String url, String user, String password, String email) {
        final DockerClientConfig.DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder();
        if (StringUtils.isEmpty(url)) {
            log.info("Connecting to localhost");
        } else {
            log.info("Connecting to {}", url);
            configBuilder.withUri(url);
        }
        if (StringUtils.isNotEmpty(user)) {
            configBuilder.withUsername(user).withPassword(password).withEmail(email);
        }
        return new JavaDockerClient(DockerClientBuilder.getInstance(configBuilder).build());
    }

    @Override
    public String buildImage(File buildDir, String tag, boolean pull) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(), "Image tag can not be empty.");
        final BuildImageResultCallback resultCallback = new BuildImageResultCallback();
        dockerClient.buildImageCmd(buildDir).withTag(tag).withPull(pull).exec(resultCallback);
        return resultCallback.awaitImageId();
    }

    @Override
    public String pushImage(String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(), "Image tag can not be empty.");
        final PushImageResultCallback pushImageResultCallback = dockerClient.pushImageCmd(tag).exec(new PushImageResultCallback());
        pushImageResultCallback.awaitSuccess();
        return "";
    }

    @Override
    public String run(String tag, String containerName, boolean detached, boolean autoRemove, Map<String, String> env, Map<String, String> ports, Map<String, String> volumes, List<String> volumesFrom, List<String> links) {
        return null;
    }

    /*@Override
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
    }*/
}
