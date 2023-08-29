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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.File;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckMojoTest {

    CheckMojo checkMojo;

    @BeforeEach
    void setUp() {
        checkMojo = new CheckMojo();
    }

    @Test
    void execute() {
        checkMojo.config = new File("src/test/resources/licenserc_t1.toml");
        assertDoesNotThrow(() -> checkMojo.execute());
        checkMojo.config = new File("src/test/resources/licenserc_t2.toml");
        assertThrows(MojoFailureException.class, () -> checkMojo.execute(), "Missing header files found");
    }
}
