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

package io.korandoru.hawkeye;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormatMojoTest {

  FormatMojo formatMojo;
  File tempFile;

  @BeforeEach
  void setUp() throws IOException {
    tempFile = File.createTempFile("test", ".yaml", new File("src/test/resources"));
    assertTrue(tempFile.setWritable(true));
    formatMojo = new FormatMojo();
    formatMojo.config = new File("src/test/resources/licenserc_t1.toml");
  }

  @AfterEach
  void tearDown() {
    assertTrue(tempFile.delete());
  }

  @Test
  void executeWithAddingHeader() {
    formatMojo.execute();
    try (final InputStream is = new FileInputStream(tempFile)) {
      final String content = new String(is.readAllBytes());
      assertTrue(content.contains("Korandoru Contributors"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void executeWithDryRun() {
    formatMojo.dryRun = true;
    formatMojo.execute();

    File formatedfile = new File(tempFile.getAbsolutePath() + ".formatted");
    assertTrue(formatedfile.exists());
    formatedfile.deleteOnExit();
  }
}
