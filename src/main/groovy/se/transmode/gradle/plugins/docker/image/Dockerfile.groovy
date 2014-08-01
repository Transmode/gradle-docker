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

class Dockerfile {
    List<String> instructions

    Dockerfile(List<String> instructions=[]) {
        this.instructions = instructions
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

    def methodMissing(String name, args) {
        // e.g. Dockerfile.add('source', 'dest') becomes "ADD source dest"
        this.append("${name.toUpperCase()} ${args.join(' ')}")
    }

    static Dockerfile fromExternalFile(File source) {
        def dockerfile = new Dockerfile()
        source.eachLine {
            dockerfile.append(it)
        }
        return dockerfile
    }

    static Dockerfile fromBaseImage(String base) {
        return new Dockerfile(["FROM ${base}"])
    }
}
