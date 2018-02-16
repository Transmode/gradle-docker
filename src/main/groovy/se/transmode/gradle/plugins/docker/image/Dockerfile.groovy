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
package se.transmode.gradle.plugins.docker.image

import com.google.common.io.Files
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class Dockerfile {
    private static Logger log = Logging.getLogger(Dockerfile)

    private List<String> instructions
    private List<String> baseInstructions
    private File contextDir
    // Actions needed to setup the build stage before building an image
    List<Closure> stagingBacklog

    final private Closure copyCallback
    final private Object resolvePathCallback

    Dockerfile(File contextDir) {
        this(contextDir, { String path -> new File(path) }, { -> })
    }

    Dockerfile(File contextDir, resolvePathCallback, copyCallback) {
        this.contextDir = contextDir
        this.resolvePathCallback = resolvePathCallback
        this.copyCallback = copyCallback
        this.baseInstructions = []
        this.instructions = []
        this.stagingBacklog = []
    }

    Dockerfile append(def instruction) {
        this.instructions.add(instruction.toString())
        return this
    }

    Dockerfile appendAll(List instructions) {
        this.instructions.addAll(instructions*.toString())
        return this
    }

    void writeToFile(File destination) {
        destination.withWriter { out ->
            instructions.each() { line ->
                out.writeLine(line)
            }
        }
    }

    /**
     * Default method if method not found to support all Dockerfile instructions.
     *
     * Example: foo('bar', 42) becomes "FOO bar 42"
     */
    def methodMissing(String name, args) {
        // fixme: uppercase methods don't seem to work without parentheses (e.g. "RUN 'echo'")
        //        see discussion on the Groovy User list: http://groovy.329449.n5.nabble.com/Optional-parentheses-for-methods-with-all-uppercase-name-tp5731174.html
        if (name.toLowerCase() != name) {
            return callWithLowerCaseName(name, args)
        }
        log.debug('No explicit method declaration for "{}({})" found. Using default implementation.', name, args.join(', '))
        this.append("${name.toUpperCase()} ${args.join(' ')}")
    }

    def callWithLowerCaseName(String name, args) {
        name = name.toLowerCase()
        return this."$name"(*args)
    }

    // todo: consider removing "extendDockerfile" as method and add it as a parameter to dockerfile DockerTask.dockerfile as it is not a real Dockerfile instruction
    /**
     * Add instructions from an external Dockerfile.
     *
     * @param baseFile -- Path to external Dockerfile
     */
    void extendDockerfile(File baseFile) {
        baseInstructions = baseFile as String[]
    }

    /**
     * Set base image (i.e. FROM <baseImage>)
     *
     * @param baseImage -- Name of the base image
     */
    void from(String baseImage) {
        baseInstructions = ["FROM ${baseImage}"]
    }

    void cmd(List cmd) {
        this.append('CMD ["' + cmd.join('", "') + '"]')
    }

    void entrypoint(List cmd) {
        this.append('ENTRYPOINT ["' + cmd.join('", "') + '"]')
    }

    private static boolean isUrl(String url) {
        try {
            new URL(url)
        } catch (MalformedURLException e) {
            return false
        }
        return true;
    }

    void add(URL source, String destination='/') {
        this.append("ADD ${source.toString()} ${destination}")
    }

    void add(String source, String destination='/') {
        if(isUrl(source)) {
            this.append("ADD ${source} ${destination}")
        } else {
            add(resolvePathCallback(source), destination)
        }
    }

    void add(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(contextDir, source.name)
        }
        else {
            target = contextDir
        }
        stagingBacklog.add { ->
            copyCallback {
                from source
                into target
            }
        }
        this.append("ADD ${source.name} ${destination}")
    }

    void add(Closure copySpec) {
        final tarFile = new File(contextDir, "add_${instructions.size()+1}.tar")
        stagingBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("ADD ${tarFile.name} ${'/'}")
    }

    void copy(String source, String destination='/') {
        copy(resolvePathCallback(source), destination)
    }

    void copy(File source, String destination='/') {
        File target
        if (source.isDirectory()) {
            target = new File(contextDir, source.name)
        }
        else {
            target = contextDir
        }
        stagingBacklog.add { ->
            copyCallback {
                from source
                into target
            }
        }
        this.append("COPY ${source.name} ${destination}")
    }

    void copy(Closure copySpec) {
        final tarFile = new File(contextDir, "copy_${instructions.size()+1}.tar")
        stagingBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("COPY ${tarFile.name} ${'/'}")
    }

    private void createTarArchive(File tarFile, Closure copySpec) {
        final tmpDir = Files.createTempDir()
        log.lifecycle("Creating tar archive {} from {}", tarFile, tmpDir)
        try {
            /* copy all files to temporary directory */
            copyCallback {
                with {
                    into('/') {
                        with copySpec
                    }
                }
                into tmpDir
            }
            /* create tar archive */
            new AntBuilder().tar(
                    destfile: tarFile,
                    basedir: tmpDir
            )
        } finally {
            if (!tmpDir.deleteDir())
                println "tmpDir $tmpDir not deleted"
        }
    }

    void label(Map labels) {
         if(labels) {
             instructions.add("LABEL " + labels.collect { k,v -> "\"$k\"=\"$v\"" }.join(' '))
         }
    }

    /**
     * Get the contents of the Dockerfile row by row as a list of strings.
     *
     * @return Dockerfile instructions as a list of Strings.
     */
    List<String> getInstructions() {
        return (baseInstructions + instructions)*.toString()
    }

    /**
     * Return true if base image or base dockerfile to extend has been defined.
     *
     * @return Boolean true if base is set
     */
    Boolean hasBase() {
        return baseInstructions.size() > 0
    }
}
