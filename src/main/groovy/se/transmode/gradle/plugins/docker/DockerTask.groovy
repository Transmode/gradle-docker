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

import com.google.common.annotations.VisibleForTesting
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import se.transmode.gradle.plugins.docker.client.DockerClient
import se.transmode.gradle.plugins.docker.image.Dockerfile

class DockerTask extends DockerTaskBase {

    private static Logger logger = Logging.getLogger(DockerTask)
    public static final String DEFAULT_IMAGE = 'ubuntu'

    // Name and Email of the image maintainer
    String maintainer
    // Whether or not to execute docker to build the image (default: false)
    Boolean dryRun
    // Whether or not to push the image into the registry (default: false)
    Boolean push
    // Wether or not the plugin will use pull flag when building
    boolean pull

    @Delegate(deprecated=true)
    LegacyDockerfileMethods legacyMethods

    // fixme: all of this should work: dockerfile = File()/String() and dockerfile { ... } how can I achieve this?
    private Dockerfile dockerfile

    @Override
    Task configure(Closure configureClosure) {
        def resolveClosure = { path -> project.file(path) }
        def copyClosure = { Closure copyClosure -> project.copy(copyClosure) }
        this.dockerfile = new Dockerfile(stageDir, resolveClosure, copyClosure)
        this.legacyMethods = new LegacyDockerfileMethods(dockerfile)
        super.configure(configureClosure)
    }

    /**
     * Configure Dockerfile with a closure, e.g.:
     *   dockerfile {
     *       FROM 'ubuntu'
     *       ADD myFile
     *   }
     * @param closure to configure dockerfile
     */
    public void dockerfile(Closure closure) {
        dockerfile.with(closure)
    }

    /**
     * Start off with existing external Dockerfile and extend it
     * @param Path to external Dockerfile
     */
    public void setDockerfile(String path) {
        dockerfile(project.file(path))
    }

    /**
     * Start off with existing external Dockerfile and extend it
     * @param External Dockerfile
     */
    public void setDockerfile(File baseFile) {
        logger.info('Creating Dockerfile from file {}.', baseFile)
        dockerfile.extendDockerfile(baseFile)
    }

    /**
     * Name of base docker image
    */
    String baseImage

    /**
     * Return the base docker image.
     *
     * If the base image is set in the task, return it. Otherwise return the base image
     * defined in the 'docker' extension. If the extension base image is not set determine
     * base image based on the 'targetCompatibility' property from the java plugin.
     *
     * @return Name of base docker image
     */
    public String getBaseImage() {
        if (baseImage) {
            return baseImage
        }
        def projectBaseImage = project[DockerPlugin.EXTENSION_NAME].baseImage
        if (projectBaseImage) {
            return projectBaseImage
        } else if (project.hasProperty('targetCompatibility')) {
            return JavaBaseImage.imageFor(project.targetCompatibility).imageName
        } else {
            return DEFAULT_IMAGE
        }
    }

    // Dockerfile instructions (ADD, RUN, etc.)
    def instructions
    // Dockerfile staging area i.e. context dir
    File stageDir
    DockerTask() {
        instructions = []
        stageDir = new File(project.buildDir, "docker")
    }
    
    void contextDir(String contextDir) {
        stageDir = new File(stageDir, contextDir)
    }

    protected File createDirIfNotExists(File dir) {
        if (!dir.exists())
            dir.mkdirs()
        return dir
    }

    @VisibleForTesting
    protected void setupStageDir() {
        logger.info('Setting up staging directory.')
        createDirIfNotExists(stageDir)
        dockerfile.stagingBacklog.each() { closure -> closure() }
    }

    @VisibleForTesting
    protected Dockerfile buildDockerfile() {
        if (!dockerfile.hasBase()) {
            def baseImage = getBaseImage()
            logger.info('Creating Dockerfile from base {}.', baseImage)
            dockerfile.from(baseImage)
        }
        // fixme: only add maintainer if not already set in external dockerfile or via dockerfile.maintainer
        if (getMaintainer()) {
            dockerfile.maintainer(getMaintainer())
        }
        return dockerfile.appendAll(instructions)
    }

    @TaskAction
    void build() {
        setupStageDir()
        buildDockerfile().writeToFile(new File(stageDir, 'Dockerfile'))
        tag = getImageTag()
        logger.info('Determining image tag: {}', tag)

        if (!dryRun) {
            DockerClient client = getClient()
            println client.buildImage(stageDir, tag, pull)
            if (push) {
                println client.pushImage(tag)
            }
        }

    }
    
}
