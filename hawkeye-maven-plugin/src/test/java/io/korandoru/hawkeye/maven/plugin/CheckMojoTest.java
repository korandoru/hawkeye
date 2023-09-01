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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckMojoTest {
    private CheckMojo checkMojo;

    @BeforeEach
    void setUp() {
        checkMojo = new CheckMojo();
        checkMojo.configLocation = new File("src/test/resources/t1.toml");
    }

    @Test
    void execute() {
        assertDoesNotThrow(() -> checkMojo.execute());
    }

    @Test
    void executeFailure() throws IOException {
        final File tempFile = File.createTempFile("test", ".yaml", new File("src/test/resources"));
        assertTrue(tempFile.setWritable(true));
        assertThrows(MojoFailureException.class, () -> checkMojo.execute(), "Missing header files found");
        tempFile.deleteOnExit();
    }
}
