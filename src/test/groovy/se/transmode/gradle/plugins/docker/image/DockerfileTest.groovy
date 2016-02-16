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
import static org.hamcrest.Matchers.is

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
    void fromBase() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.from(BASE_IMAGE)
        assertThat(dockerfile.instructions,
                equalTo(["FROM ${BASE_IMAGE}".toString()]))
    }

    @Test
    void appendInstruction() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            from BASE_IMAGE
            append MAINTAINER
        }
        assertThat(dockerfile.instructions,
                equalTo(["FROM ${BASE_IMAGE}".toString(), MAINTAINER]))
    }

    @Test
    void fallbackToDefaultMethod() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            foo('bar', 42)
            bar 'All work and no play makes Jack a dull boy'
        }
        assertThat(dockerfile.instructions,
                equalTo(['FOO bar 42',
                         'BAR All work and no play makes Jack a dull boy']))
    }

    @Test
    void cmdWithString() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.cmd '/bin/bash'
        assertThat dockerfile.instructions, is(equalTo(['CMD /bin/bash']))
    }

    @Test
    void cmdWithList() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.cmd(['/bin/bash', '-i'])
        assertThat dockerfile.instructions, is(equalTo(['CMD ["/bin/bash", "-i"]']))
    }

    @Test
    void extendDockerfile() {
        File source = createTestDockerfile()
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.extendDockerfile(source)
        assertThat(dockerfile.instructions, is(equalTo(INSTRUCTIONS)))
    }

    @Test
    void extendDockerfileAndAppend() {
        File source = createTestDockerfile()
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.extendDockerfile(source)
        dockerfile.append(MAINTAINER)
        assertThat(dockerfile.instructions,
                equalTo(INSTRUCTIONS + [MAINTAINER]))
    }

    @Test
    void addURL() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final URL url = new URL('http', 'localhost', 8080, '/download/myapp.tar')

        dockerfile.add url
        assertThat dockerfile.instructions, is(equalTo(["ADD ${url.toString()} /".toString()]))
    }

    @Test
    void addURLasString() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final String url = 'http://foo.bar/file.tar'

        dockerfile.add url, '/target'
        assertThat dockerfile.instructions, is(equalTo(["ADD ${url} /target".toString()]))
    }


    @Test
    void addFile() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final File file = new File('/tmp/adke')

        dockerfile.add file, '/target'
        assertThat dockerfile.instructions, is(equalTo(["ADD ${file.name} /target".toString()]))
    }

    @Test
    void addDirectory() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final File file = new File('gradle/wrapper')

        dockerfile.add file, '/gradle'
        assertThat dockerfile.instructions, is(equalTo(['ADD wrapper /gradle']))
    }

    @Test
    void addFileAsString() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final String file = 'gradle/wrapper/gradle-wrapper.properties'

        dockerfile.add file, '/target'
        assertThat dockerfile.instructions, is(equalTo(['ADD gradle-wrapper.properties /target']))
    }

    @Test
    void addWithCopySpec() {
        final dockerfile = new Dockerfile(new File("contextDir"))

        dockerfile.add {
            from('src/foo')
            into('dest/bar')
            exclude '*~'
        }
        assertThat dockerfile.instructions, is(equalTo(['ADD add_1.tar /']))
    }

    @Test
    void copyFile() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final File file = new File('tmp/wetoy')

        dockerfile.copy file, '/foo'
        assertThat dockerfile.instructions, is(equalTo(['COPY wetoy /foo']))
    }

    @Test
    void copyFileAsString() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        final String path = 'asdrlu/foo'

        dockerfile.copy path, '/bar'
        assertThat dockerfile.instructions, is(equalTo(['COPY foo /bar']))
    }

    @Test
    void copyWithCopySpec() {
        final dockerfile = new Dockerfile(new File("contextDir"))

        dockerfile.copy {
            from('src/foo')
            into('dest/bar')
            exclude '*~'
        }
        assertThat dockerfile.instructions, is(equalTo(['COPY copy_1.tar /']))
    }

    @Test
    void instructionsAreCaseInsesitive() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            CMD('/bin/bash')
        }
        dockerfile.ENV('FOO=bar')
        assertThat(dockerfile.instructions,
                equalTo(['CMD /bin/bash',
                         'ENV FOO=bar']))
    }

    @Test
    void exposePort() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            expose 8080
        }
        assertThat(dockerfile.instructions, equalTo(['EXPOSE 8080']))
    }

    @Test
    void exposeMultiplePort() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            expose 8080, 8081, 9090
        }
        assertThat(dockerfile.instructions, equalTo(['EXPOSE 8080 8081 9090']))
    }

    @Test
    void exposeUdpPort() {
        final dockerfile = new Dockerfile(new File("contextDir"))
        dockerfile.with {
            expose '162/udp'
        }
        assertThat(dockerfile.instructions, equalTo(['EXPOSE 162/udp']))
    }
 }
