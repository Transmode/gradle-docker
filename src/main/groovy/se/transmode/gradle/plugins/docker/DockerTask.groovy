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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction

class DockerTask extends DefaultTask {

    private static Logger logger = Logging.getLogger(DockerTask)

    // full path to the docker executable
    String dockerBinary
    // Name and Email of the image maintainer
    String maintainer
    // Name of the application being wrapped into a docker image (default: project.name)
    String applicationName
    // What to tag the created docker image with (default: group/applicationName)
    String tag
    // Which version to use along with the tag (default: latest)
    String tagVersion
    // Whether or not to execute docker to build the image (default: false)
    Boolean dryRun
    // Whether or not to push the image into the registry (default: false)
    Boolean push
    // Hostname, port of the docker image registry unless Docker index is used
    String registry

    /**
     * Name of the base docker image
    */
    String baseImage
    public String getBaseImage() {
        return determineBaseImage()
    }

    /**
     * Determine the name of the base docker image.
     *
     * If the base image is set in the task, return it. Otherwise return the base image
     * defined in the 'docker' extension. If the extension base image is not set determine
     * base image based on the 'targetCompatibility' property from the java plugin.
     *
     * @return Name of base docker image
     */
    private String determineBaseImage() {
        return baseImage ?: (project[DockerPlugin.EXTENSION_NAME].baseImage ?: JavaBaseImage.imageFor(project.targetCompatibility).imageName)
    }

    // Executable to run when image is instantiated
    def entryPoint
    // Executable to run when image is instantiated without a parameter or  executable
    def defaultCommand
    // Dockerfile instructions (ADD, RUN, etc.)
    def instructions
    // Dockerfile staging area i.e. context dir
    final File stageDir


    DockerTask() {
        entryPoint = []
        defaultCommand = []
        instructions = []
        stageDir = new File(project.buildDir, "docker")
    }

    void addFile(File file) {
        stageDir.mkdir()
        project.copy {
            from file
            into stageDir
        }
        instructions.add("ADD ${file.name} /")
    }

    void addFile(Closure copySpec) {
        stageDir.mkdir()
        project.copy(copySpec)
        instructions.add("ADD ${copySpec} ${destPath}")
    }

    void workingDir(String wd) {
        instructions.add("WORKDIR ${wd}")
    }

    void addInstruction(String cmd, String value) {
        instructions.add("${cmd} ${value}")
    }

    void runCommand(String command) {
        instructions.add("RUN ${command}")
    }

    void exposePort(Integer port) {
        instructions.add("EXPOSE ${port}")
    }

    void setEnvironment(String key, String value) {
        instructions.add("ENV ${key} ${value}")
    }

    void setTagVersion(String version) {
        tagVersion = version;
    }

    void setTagVersionToLatest() {
        tagVersion = null;
    }

    void volume(String... paths) {
        instructions.add('VOLUME ["' + paths.join('", "') + '"]')
    }

    List getPreamble() {
        def preamble = []
        preamble.add("FROM ${determineBaseImage()}")
        preamble.add("MAINTAINER ${-> maintainer}")
        return preamble
    }

    List getEpilogue() {
        def epilogue = []
        if (entryPoint) {
            epilogue.add('ENTRYPOINT ["' + entryPoint.join('", "') + '"]')
        }
        if (defaultCommand) {
            epilogue.add('CMD ["' + defaultCommand.join('", "') + '"]')
        }
        return epilogue
    }

    public List buildDockerFile() {
        return preamble + instructions + epilogue
    }


    private File createDirIfNotExists(File dir) {
        if (!dir.exists())
            dir.mkdirs()
        return dir
    }

    @TaskAction
    void build() {

        createDirIfNotExists(stageDir)

        new File(stageDir, "Dockerfile").withWriter { out ->
            buildDockerFile().each() { line ->
                out.writeLine(line)
            }
        }

        if (registry) {
            tag = "${-> registry}/${-> applicationName}"
        }
        else if (project.group) {
            tag = "${-> project.group}/${-> applicationName}"
        }
        else {
            tag = "${-> applicationName}"
        }

        if (tagVersion) {
            tag += ":${-> tagVersion}"
        }

        if (!dryRun) {
            println buildDockerImage(tag)

            if (push) {
                println pushDockerImage(tag)
            }
        }

    }

    private String executeAndWait(GString cmdLine) {
        logger.info("Executing command '" + cmdLine + "'.")
        def process = cmdLine.execute()
        process.waitFor()
        if (process.exitValue()) {
           throw new GradleException("docker execution failed\nCommand line [${cmdLine}] returned:\n${process.err.text}")
        }
        return process.in.text
    }

    private String pushDockerImage(String tag) {
        def cmdLine = "${-> dockerBinary} push ${tag}"
        return executeAndWait(cmdLine)
    }

    private String buildDockerImage(String tag) {
        def cmdLine = "${-> dockerBinary} build -t ${-> tag} ${-> stageDir}"
        return executeAndWait(cmdLine)
    }
}
