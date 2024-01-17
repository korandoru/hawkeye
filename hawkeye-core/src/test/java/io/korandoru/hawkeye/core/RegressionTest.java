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

package io.korandoru.hawkeye.core;

import static org.assertj.core.api.Assertions.assertThat;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import java.io.File;
import org.junit.jupiter.api.Test;

public class RegressionTest {
    @Test
    void testIssue110() {
        final File file = new File("src/test/data/issue-110/licenserc.toml");
        final HawkEyeConfig config = HawkEyeConfig.of(file).dryRun(true).build();
        final LicenseFormatter formatter = new LicenseFormatter(config);
        formatter.call();

        final File expected = new File("src/test/data/issue-110/main.rs.formatted.expected");
        final File actual = new File("src/test/data/issue-110/main.rs.formatted");
        assertThat(actual).hasSameTextualContentAs(expected);
    }

    @Test
    void testIssue113() {
        final File file = new File("src/test/data/issue-113/licenserc.toml");
        final HawkEyeConfig config = HawkEyeConfig.of(file).dryRun(true).build();
        final LicenseFormatter formatter = new LicenseFormatter(config);
        formatter.call();

        final File expected = new File("src/test/data/issue-113/main.rs.formatted.expected");
        final File actual = new File("src/test/data/issue-113/main.rs.formatted");
        assertThat(actual).hasSameTextualContentAs(expected);
    }
}
