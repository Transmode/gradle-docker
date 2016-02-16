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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ApplicationPlugin

class DockerPlugin implements Plugin<Project> {

    private static Logger logger = Logging.getLogger(DockerPlugin)

    private static final String DOCKER_BINARY = "docker"
    static final String EXTENSION_NAME = "docker"

    DockerPluginExtension extension


    void apply(Project project) {
        this.extension = createExtension(project)
        addDockerTaskType(project)
        addDockerRunTaskType(project)
        project.plugins.withType(ApplicationPlugin).all {
            logger.info('Detected application plugin.')
            addDistDockerTask(project)
        }
        configureDockerTasks(project)
        configureDockerRunTasks(project)
    }

    private void addDistDockerTask(Project project) {
        logger.info('Adding docker task "distDocker"');
        project.task('distDocker', type: DockerTask) {
            group = 'docker'
            description = "Packs the project's JVM application as a Docker image."

            inputs.files project.distTar

            def installDir = "/" + project.distTar.archiveName - ".${project.distTar.extension}"

            doFirst {
                applicationName = project.applicationName
                dockerfile {
                    ADD(project.distTar.outputs.files.singleFile)
                    ENTRYPOINT(["$installDir/bin/${project.applicationName}"])
                }
            }
        }
    }

    private void addDockerTaskType(Project project) {
        logger.info("Adding docker task type");
        project.ext.Docker = DockerTask.class
    }

    private void addDockerRunTaskType(Project project) {
        project.ext.DockerRun = DockerRunTask.class
        logger.info("Adding docker run task type");
    }

    private DockerPluginExtension createExtension(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, DockerPluginExtension)
        extension.with {
            maintainer = ''
            dockerBinary = DOCKER_BINARY
            registry = ''
            useApi = Boolean.FALSE
            hostUrl = ''
            apiUsername = ''
            apiEmail = ''
            apiPassword = ''
        }
        logger.info("Adding docker extension");
        return extension
    }

    private void configureDockerTasks(Project project) {
        project.tasks.withType(DockerTask.class).all { task ->
            logger.info('Applying docker defaults to task {}', task.name);
            applyTaskDefaults(task)
        }
    }

    private void configureDockerRunTasks(Project project) {
        project.tasks.withType(DockerRunTask.class).all { task ->
            applyRunTaskDefaults(task)
        }
        logger.info("Applying docker defaults to tasks of type 'DockerRun'");
    }

    private void applyTaskDefaults(task) {
        // @todo: don't use conventionMapping as it is an internal mechanism
        //        see http://forums.gradle.org/gradle/topics/how_do_you_use_a_conventionmapping_to_do_the_following
        task.conventionMapping.with {
            dockerBinary = { extension.dockerBinary }
            maintainer = { extension.maintainer }
            registry = { extension.registry }
            useApi = { extension.useApi }
            hostUrl = { extension.hostUrl }
            apiUsername = { extension.apiUsername }
            apiPassword = { extension.apiPassword }
            apiEmail = { extension.apiEmail }
        }
    }
    
    private void applyRunTaskDefaults(task) {
        // @todo: don't use conventionMapping as it is an internal mechanism
        //        see http://forums.gradle.org/gradle/topics/how_do_you_use_a_conventionmapping_to_do_the_following
        task.conventionMapping.with {
            dockerBinary = { extension.dockerBinary }
            registry = { extension.registry }
            useApi = { extension.useApi }
            hostUrl = { extension.hostUrl }
            apiUsername = { extension.apiUsername }
            apiPassword = { extension.apiPassword }
            apiEmail = { extension.apiEmail }
        }
    }
}
