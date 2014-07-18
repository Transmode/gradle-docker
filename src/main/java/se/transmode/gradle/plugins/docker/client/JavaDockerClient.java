package se.transmode.gradle.plugins.docker.client;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;

import java.io.File;

/**
 * @author Matthias Grüter (magr)
 */
public class JavaDockerClient extends com.github.dockerjava.client.DockerClient implements DockerClient {

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
        // Either create default client or one for configured URL
        JavaDockerClient client;
        if (StringUtils.isEmpty(url)) {
            client = new JavaDockerClient();
        } else {
            client = new JavaDockerClient(url);
        }

        // Do we have authentication info?
        if (StringUtils.isNotEmpty(user)) {
            client.setCredentials(user, password, email);
        }
        return client;
    }
}
