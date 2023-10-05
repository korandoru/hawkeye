/*
 * Copyright 2023 Korandoru Contributors
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

package io.korandoru.hawkeye.gradle.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HawkEyeGradlePluginFunctionalTest {
    @TempDir
    private File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    @Test
    void testRunTasks() throws IOException {
        final String expectedLicenseHeader =
                """
                /*
                 * Copyright 2023 Korandoru Contributors
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
                """;

        writeString(getSettingsFile(), expectedLicenseHeader);
        writeString(
                getBuildFile(),
                """
                        %s

                        plugins {  id('io.korandoru.hawkeye') }
                        hawkeye {
                            addConfig('src/test/resources/t1.toml') {
                                baseDir = $/%s/$
                            }
                        }
                        """
                        .formatted(expectedLicenseHeader, projectDir.getAbsolutePath()));
        testRunTask("hawkeyeCheck");
        testRunTask("hawkeyeFormat");
        testRunTask("hawkeyeRemove");
    }

    private void testRunTask(String taskName) {
        final GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments(taskName);
        runner.withProjectDir(projectDir);
        final BuildResult result = runner.build();
        assertThat(result).isNotNull();
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
