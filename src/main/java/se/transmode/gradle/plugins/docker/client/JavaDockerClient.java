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

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;

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
        ClientResponse response = buildImageCmd(buildDir).withTag(tag).exec();
        return checkResponse(response);
    }

    @Override
    public String pushImage(String tag) {
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
            client = new JavaDockerClient();
        } else {
            log.info("Connecting to {}", url);
            client = new JavaDockerClient(url);
        }

        if (StringUtils.isNotEmpty(user)) {
            client.setCredentials(user, password, email);
        }

        return client;
    }
}
