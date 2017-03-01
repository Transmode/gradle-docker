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

public interface DockerClient {
    /**
     * Build a Docker image from the contents of the given directory.
     * 
     * @param buildDir the directory from which to build the image
     * @param tag the tag to apply to the image
     * @param pull wether to pull latest image or not, true enables the pull, false disables pull
     * @return the output of the command
     */
    public String buildImage(File buildDir, String tag, boolean pull);
    
    /**
     * Push the given image to the configured Docker registry.
     * 
     * @param tag the tag of the image to push
     * @return the output of the command
     */
    public String pushImage(String tag);
    
    /**
     * Run the given image in a container with the given name.
     * 
     * @param tag the image to run
     * @param containerName the name of the container to create
     * @param detached should the container be run in the background (aka detached)
     * @param autoRemove should the container be removed when execution completes
     * @param env a map containing a collection of environment variables to set
     * @param ports a map containing the ports to publish
     * @param volumes a map containing the volumes to bind
     * @param volumesFrom a list of the containers whose volumes we should mount
     * @param links a list of the containers to which the newly created container should be linked
     * @return the output of the command
     */
    public String run(String tag, String containerName, boolean detached, boolean autoRemove,
            Map<String, String> env, Map<String, String> ports, Map<String, String> volumes, 
            List<String> volumesFrom, List<String> links);
}
