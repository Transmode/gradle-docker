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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ApplicationPlugin

class DockerPlugin implements Plugin<Project> {

    private static Logger logger = Logging.getLogger(DockerPlugin)

    private static final String BASE_IMAGE = "ubuntu"
    private static final String BASE_IMAGE_JAVA6 = "fkautz/java6-jre"
    private static final String BASE_IMAGE_JAVA7 = "dockerfile/java"
    private static final String BASE_IMAGE_JAVA = BASE_IMAGE_JAVA7
    private static final String DOCKER_BINARY = "docker"
    private static final String BASE_IMAGE_JAVA8 = "aglover/java8-pier"

    DockerPluginExtension extension


    void apply(Project project) {
        this.extension = createExtension(project)
        addDockerTaskType(project)
        configureDockerTasks(project)

        // FIXME: Application plugin must be applied before docker plugin for this to work.
        // FIXME: Check for application AND java plugin
        project.plugins.withType(ApplicationPlugin.class).all {
            addDistDockerTask(project)
        }
    }

    private void addDistDockerTask(Project project) {
        project.task('distDocker', type: DockerTask) {
            group = 'docker'
            description = "Packs the project's JVM application as a Docker image."

            inputs.files project.distTar

            doFirst {
                baseImage = determineJavaBaseImage(project.targetCompatibility)
                applicationName = project.applicationName
                addFile project.distTar.outputs.files.singleFile

                def installDir = "/" + project.distTar.archiveName - ".${project.distTar.extension}"
                entryPoint(["$installDir/bin/${project.applicationName}"])
            }
        }
        logger.info("Adding docker task 'distDocker'");
    }

    private String determineJavaBaseImage(def projectVersion) {
        switch (projectVersion) {
            case JavaVersion.VERSION_1_6:
                return extension.baseImageJava16
            case JavaVersion.VERSION_1_7:
                return extension.baseImageJava17
            case JavaVersion.VERSION_1_8:
                return extension.baseImageJava18
            default:
                return extension.baseImageJava
        }
    }

    private void addDockerTaskType(Project project) {
        project.ext.Docker = DockerTask.class
        logger.info("Adding docker task type");
    }

    private DockerPluginExtension createExtension(Project project) {
        def extension = project.extensions.create("docker", DockerPluginExtension)
        extension.with {
            maintainer = "unknown"
            dockerBinary = DOCKER_BINARY
            baseImage = BASE_IMAGE
            baseImageJava = BASE_IMAGE_JAVA
            baseImageJava16 = BASE_IMAGE_JAVA6
            baseImageJava17 = BASE_IMAGE_JAVA7
            baseImageJava18 = BASE_IMAGE_JAVA8
            registry = ""
        }
        logger.info("Adding docker extension");
        return extension
    }

    private void configureDockerTasks(Project project) {
        project.tasks.withType(DockerTask.class).all { task ->
            applyTaskDefaults(task)
        }
        logger.info("Applying docker defaults to tasks of type 'Docker'");
    }

    private void applyTaskDefaults(task) {
        task.conventionMapping.with {
            maintainer = { extension.maintainer }
            dockerBinary = { extension.dockerBinary }
            maintainer = { extension.maintainer }
            baseImage = { extension.baseImage }
            registry = { extension.registry }
        }
    }
}