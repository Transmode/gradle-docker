/*
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
package se.transmode.gradle.plugins.docker.client

import com.google.common.base.Preconditions
import org.gradle.api.GradleException

class NativeDockerClient implements DockerClient {

    private final String binary;

    NativeDockerClient(String binary) {
        Preconditions.checkArgument(binary as Boolean,  "Docker binary can not be empty or null.")
        this.binary = binary
    }

    @Override
    String buildImage(File buildDir, String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
        def cmdLine = "${binary} build -t ${tag} ${buildDir}"
        return executeAndWait(cmdLine)
    }

    @Override
    String pushImage(String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
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
