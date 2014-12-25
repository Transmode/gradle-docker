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
import org.gradle.api.file.CopySpec
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class Dockerfile {
    private static Logger log = Logging.getLogger(Dockerfile)

    private List<String> instructions
    private List<String> baseInstructions
    private File contextDir
    // Tasks necessary to setup the stage before building an image
    private List stagingBacklog

    // fixme: is there a better way to define default methods and make them overridable
    private def resolvePathLamba
    private File resolvePath(String path) {
        return new File(path)
    }

    private def copyLambda
    private void copy(CopySpec copySpec) {
        // default: do nothing
    }

    // fixme: do we really need a no-args constructor?
    Dockerfile(resolvePathLambda={ -> this.resolvePath(it)}, copyLambda={ -> this.copy(it)}) {
        this.baseInstructions = []
        this.instructions = []
        this.contextDir
        this.stagingBacklog = []
        this.resolvePathLamba = resolvePathLambda
        this.copyLambda = copyLambda
    }

    Dockerfile(String base,
               resolvePathLambda={ -> this.resolvePath(it)}, copyLambda={ -> this.copy(it)}) {
        this(resolvePathLambda, copyLambda)
        from base
    }

    Dockerfile(File externalDockerfile,
               resolvePathLambda={ -> this.resolvePath(it)}, copyLambda={ -> this.copy(it)}) {
        this(resolvePathLambda, copyLambda)
        if(externalDockerfile.isFile()) {
            this.contextDir = externalDockerfile.parentFile
        }
        from externalDockerfile
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
        // fixme: check for case insensitive method name match before falling back to default method
        log.debug('No explicit method declaration for "{}({})" found. Using default implementation.', name, args.join(', '))
        this.append("${name.toUpperCase()} ${args.join(' ')}")
    }

    /**
     * Add instructions from an external Dockerfile.
     *
     * @param baseFile -- Path to external Dockerfile
     */
    void from(File baseFile) {
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

    void add(String source, String destination='/') {
        if(isUrl(source)) {
            this.append("ADD ${source} ${destination}")
        } else if(isFile(source)) {
            add(resolvePath(source), destination)
        }
    }

    boolean isFile(String file) {
        return resolvePath(file).exists()
    }

    private boolean isUrl(String url) {
        return !resolvePath(url).exists()
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
            copy {
                from source
                into target
            }
        }
        this.append("ADD ${source.name} ${destination}")
    }

    void addFile(Closure copySpec) {
        final tarFile = new File(stageDir, "add_${instructions.size()+1}.tar")
        stagingBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("ADD ${tarFile.name} ${'/'}")
    }

    void createTarArchive(File tarFile, Closure copySpec) {
        final tmpDir = Files.createTempDir()
        log.info("Creating tar archive {} from {}", tarFile, tmpDir)
        /* copy all files to temporary directory */
        copy {
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
    }

    /**
     * Get the contents of the Dockerfile row by row as a list of strings.
     *
     * @return Dockerfile instructions as a list of Strings.
     */
    List<String> getInstructions() {
        return (baseInstructions + instructions)*.toString()
    }
}
