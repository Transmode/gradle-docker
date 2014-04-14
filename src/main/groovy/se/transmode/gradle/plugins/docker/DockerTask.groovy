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
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import se.transmode.gradle.plugins.docker.client.DockerClient
import se.transmode.gradle.plugins.docker.client.JavaDockerClient
import se.transmode.gradle.plugins.docker.client.NativeDockerClient

class DockerTask extends DefaultTask {

    private static Logger logger = Logging.getLogger(DockerTask)
    public static final String DEFAULT_IMAGE = 'ubuntu'

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
        def defaultImage = project.hasProperty('targetCompatibility') ? JavaBaseImage.imageFor(project.targetCompatibility).imageName : DEFAULT_IMAGE
        return baseImage ?: (project[DockerPlugin.EXTENSION_NAME].baseImage ?: defaultImage)
    }

    // Executable to run when image is instantiated
    def entryPoint
    // Executable to run when image is instantiated without a parameter or  executable
    def defaultCommand
    // Dockerfile instructions (ADD, RUN, etc.)
    def instructions
    // Dockerfile staging area i.e. context dir
    File stageDir
    
    // Should we use Docker's remote API instead of the docker executable
    Boolean useApi
    // URL of the remote Docker host (default: localhost)
    String hostUrl
    // Docker remote API credentials
    String apiUsername
    String apiPassword
    String apiEmail

    DockerTask() {
        entryPoint = []
        defaultCommand = []
        instructions = []
        applicationName = project.name
        stageDir = new File(project.buildDir, "docker")
    }

    void addFile(File file) {
        stageDir.mkdir()
        project.copy {
            from file
            into stageDir
        }
        addFileFromStageDir(file)
    }

    void addFile(Closure copySpec) {

        //@fixme: don't actually do the work here. add it to backlog and execute in @TaskAction method
        stageDir.mkdir()

        log.warn("Stage dir: {}", stageDir.toString())
        final File tarPath = File.createTempFile('add_', '.tar', stageDir)
        tarPath.delete()
        File tarFile = createTarArchive(tarPath) {
            into('/') {
                with copySpec
            }
        }
        addFileFromStageDir(tarFile)
    }

    private static File createTarArchive(File tarPath, File dir) {
        log.warn("Creating tar archive {} from {}", tarPath, dir)
        new AntBuilder().tar(
                destfile: tarPath,
                basedir: dir
        )
        return tarPath
    }

    private File createTarArchive(File tarPath, Closure copySpec) {
        final File tmpDir = Files.createTempDir()
        project.copy {
            with copySpec
            into tmpDir
        }
        return createTarArchive(tarPath, tmpDir)
    }

    private void addFileFromStageDir(File file) {
        logger.info("ADD ${file} /")
        instructions.add("ADD ${file.name} /")
    }

    private void addArchive(File archive) {
        addFile(archive)
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
    
    void contextDir(String contextDir) {
        stageDir = new File(stageDir, contextDir)
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

        tag = getImageTag()
        logger.info('Determining image tag: {}', tag)

        if (!dryRun) {
            DockerClient client = getClient()
            println client.buildImage(stageDir, tag)
            if (push) {
                println client.pushImage(tag)
            }
        }

    }

    private String getImageTag() {
        String tag
        tag = this.tag ?: getDefaultImageTag()
        return appendImageTagVersion(tag)
    }

    private String getDefaultImageTag() {
        String tag
        if (registry) {
            tag = "${-> registry}/${-> applicationName}"
        } else if (project.group) {
            tag = "${-> project.group}/${-> applicationName}"
        } else {
            tag = "${-> applicationName}"
        }
        return tag
    }

    private String appendImageTagVersion(String tag) {
        def version = tagVersion ?: project.version
        if(version == 'unspecified') {
            version = 'latest'
        }
        return "${tag}:${version}"

    }

    private DockerClient getClient() {
        DockerClient client
        if(getUseApi()) {
            logger.info("Using the Docker remote API.")
            client = JavaDockerClient.create(
                    getHostUrl(),
                    getApiUsername(),
                    getApiPassword(),
                    getApiEmail())
        } else {
            logger.info("Using the native docker binary.")
            client = new NativeDockerClient(getDockerBinary())
        }
        return client
    }
}
