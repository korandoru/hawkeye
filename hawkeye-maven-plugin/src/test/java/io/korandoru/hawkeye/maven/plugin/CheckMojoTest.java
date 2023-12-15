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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckMojoTest {
    private CheckMojo checkMojo;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        checkMojo = new CheckMojo();
        checkMojo.basedir = tempDir.toFile();
        checkMojo.configLocation = new File("src/test/resources/t1.toml");
    }

    @Test
    void execute() throws Exception {
        checkMojo.execute();
    }

    @Test
    void executeFailure() throws IOException {
        final File tempFile = File.createTempFile("test", ".yaml", tempDir.toFile());
        tempFile.deleteOnExit();
        assertThatThrownBy(() -> checkMojo.execute())
                .isExactlyInstanceOf(MojoFailureException.class)
                .hasMessage("Found missing header files.");
    }
}
