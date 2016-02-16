package se.transmode.gradle.plugins.docker

import groovy.util.logging.Log
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

@Log
class ExampleProjects {

    @Test
    public void simpleExample() {
        def projectDir = new File('examples/simple')
        def expectedDockerfile = new File(projectDir, 'Dockerfile.expected').text

        def actualDockerfile = runGradleTask(projectDir, 'docker')

        assertThat actualDockerfile, is(equalTo(expectedDockerfile))
    }

    @Test
    public void applicationExample() {
        def projectDir = new File('examples/application')
        def expectedDockerfile = new File(projectDir, 'Dockerfile.expected').text

        def actualDockerfile = runGradleTask(projectDir, 'distDocker')

        assertThat actualDockerfile, is(equalTo(expectedDockerfile))
    }

    private String runGradleTask(File projectDir, String taskName) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect();
        try {
            connection.newBuild()
                    .forTasks(taskName)
                    .run();
        } finally {
            connection.close();
        }
        return new File(projectDir, 'build/docker/Dockerfile').text
    }
}

