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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveMojoTest {
    private RemoveMojo removeMojo;
    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        final File testDir = new File("src/test/resources/test_remove");
        if (!testDir.exists()) {
            assertTrue(testDir.mkdirs());
        }
        testDir.deleteOnExit();
        tempFile = File.createTempFile("test", ".yaml", testDir);
        assertTrue(tempFile.setWritable(true));

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
        # limitations under the License.""";

        final Path path = Paths.get(tempFile.getAbsolutePath());

        Files.write(path, header.getBytes());

        removeMojo = new RemoveMojo();
        removeMojo.configLocation = new File("src/test/resources/t2.toml");
    }

    @AfterEach
    void tearDown() {
        assertTrue(tempFile.delete());
    }

    @Test
    void executeWithoutDryRun() throws IOException {
        removeMojo.execute();

        final String content = new String(Files.readAllBytes(tempFile.toPath()));
        assertFalse(content.contains("Korandoru Contributors"));
    }

    @Test
    void executeWithDryRun() {
        removeMojo.dryRun = true;
        removeMojo.execute();

        final File formatedfile = new File(tempFile.getAbsolutePath() + ".removed");
        assertTrue(formatedfile.exists());
        formatedfile.deleteOnExit();
    }
}
