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

package io.korandoru.hawkeye.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveMojoTest {
    private RemoveMojo removeMojo;
    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("test", ".yaml", null);

        final String header =
                """
        # Copyright 2023 Korandoru Contributors
        #
        # Licensed under the Apache License, Version 2.0 (the "License");
        # you may not use this file except in compliance with the License.
        # You may obtain a copy of the License at
        #
        #     http://www.apache.org/licenses/LICENSE-2.0
        #
        # Unless required by applicable law or agreed to in writing, software
        # distributed under the License is distributed on an "AS IS" BASIS,
        # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        # See the License for the specific language governing permissions and
        # limitations under the License.

        name: testfile""";

        final Path path = Paths.get(tempFile.getAbsolutePath());
        Files.write(path, header.getBytes());

        removeMojo = new RemoveMojo();
        removeMojo.basedir = tempFile.getParentFile();
        removeMojo.configLocation = new File("src/test/resources/t2.toml");
    }

    @Test
    void executeWithoutDryRun() throws IOException {
        removeMojo.execute();

        final String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertThat(content).doesNotContain("Korandoru Contributors");
        assertThat(content).contains("testfile");
    }

    @Test
    void executeWithDryRun() {
        removeMojo.dryRun = true;
        removeMojo.execute();

        final File formatedfile = new File(tempFile.getAbsolutePath() + ".removed");
        assertThat(formatedfile).exists();
    }

    @Test
    void executeWithSkip() throws IOException {
        removeMojo.skip = true;
        removeMojo.execute();

        final String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertThat(content).contains("Korandoru Contributors");
        assertThat(content).contains("testfile");
    }
}
