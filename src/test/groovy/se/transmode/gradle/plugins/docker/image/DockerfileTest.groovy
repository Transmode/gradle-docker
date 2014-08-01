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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

class DockerfileTest {

    public static final String BASE_IMAGE = 'ubuntu:14.04'
    public static final String MAINTAINER = 'MAINTAINER john doe'
    public static final ArrayList<String> INSTRUCTIONS = [
            'FROM debian:jessie',
            'CMD /bin/bash'
    ]

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder()

    private File createTestDockerfile() {
        def source = testFolder.newFile('Dockerfile')
        source.withWriter { out ->
            INSTRUCTIONS.each { out.writeLine(it) }
        }
        return source
    }

    @Test
    void createFromBase() {
        assertThat(Dockerfile.fromBaseImage(BASE_IMAGE).instructions as ArrayList<GString>,
                equalTo(["FROM ${BASE_IMAGE}"]))
    }

    @Test
    void createFromBaseAndAppend() {
        def dockerfile = Dockerfile.fromBaseImage(BASE_IMAGE)
        dockerfile.append(MAINTAINER)
        assertThat(dockerfile.instructions as ArrayList<String>,
                equalTo(["FROM ${BASE_IMAGE}", MAINTAINER]))
    }

    @Test
    void missingMethodTest() {
        def dockerfile = Dockerfile.fromBaseImage(BASE_IMAGE)
        dockerfile.with {
            expose 80
            cmd '/bin/bash'
        }
        assertThat(dockerfile.instructions as ArrayList<String>,
                equalTo(["FROM ${BASE_IMAGE}", "EXPOSE 80", "CMD /bin/bash", ]))
    }

    @Test
    void createFromFile() {
        File source = createTestDockerfile()
        def dockerfile = Dockerfile.fromExternalFile(source)
        assertThat(dockerfile.instructions as ArrayList<String>,
                equalTo(INSTRUCTIONS))
    }

    @Test
    void createFromFileAndAppend() {
        File source = createTestDockerfile()
        def dockerfile = Dockerfile.fromExternalFile(source)
        dockerfile.append(MAINTAINER)
        assertThat(dockerfile.instructions,
                equalTo(INSTRUCTIONS + [MAINTAINER]))
    }
}
