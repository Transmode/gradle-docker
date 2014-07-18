package se.transmode.gradle.plugins.docker.client;

import java.io.File;

/**
 * @author Matthias Grüter (magr)
 */
public interface DockerClient {
    public String buildImage(File buildDir, String tag);
    public String pushImage(String tag);
}
