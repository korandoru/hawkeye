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
}
