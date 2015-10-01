/**
 * Copyright 2014 Transmode AB
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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
    public String buildImage(File buildDir, String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(), "Image tag can not be empty.");
        final BuildImageResultCallback resultCallback = new BuildImageResultCallback();
        dockerClient.buildImageCmd(buildDir).withTag(tag).exec(resultCallback);
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
    public void saveImage(String tag, File toFile) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(), "Image tag can not be empty.");
        try (InputStream inputStream = dockerClient.saveImageCmd(tag).exec(); OutputStream outputStream = new FileOutputStream(toFile)) {
            byte[] buffer = new byte[10 * 1024];
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                outputStream.write(buffer, 0, length);
                outputStream.flush();
            }
        } catch (IOException e) {
            throw new GradleException("Unable to save image to " + toFile, e);
        }
    }
}
