package se.transmode.gradle.plugins.docker.client

import org.gradle.api.GradleException

/**
 * @author Matthias Grüter (magr)
 */
class NativeDockerClient implements DockerClient {

    private final String binary;

    NativeDockerClient(String binary) {
        this.binary = binary
    }

    @Override
    String buildImage(File buildDir, String tag) {
        def cmdLine = "${binary} build -t ${tag} ${buildDir}"
        return executeAndWait(cmdLine)
    }

    @Override
    String pushImage(String tag) {
        def cmdLine = "${binary} push ${tag}"
        return executeAndWait(cmdLine)
    }

    private static String executeAndWait(String cmdLine) {
        def process = cmdLine.execute()
        process.waitFor()
        if (process.exitValue()) {
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}] returned:\n${process.err.text}")
        }
        return process.in.text
    }

}
