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

import io.korandoru.hawkeye.core.LicenseFormatter;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Map;

@Mojo(name = "format")
public class FormatMojo extends AbstractMojo {

    @Parameter(name = "dryRun", defaultValue = "false")
    public boolean dryRun;

    @Override
    public void execute() {
        final Log log = getLog();
        log.info("Formatting license headers... with cfg: %s, dryRun: %s".formatted(config, dryRun));

        final HawkEyeConfig heConfig = HawkEyeConfig.of(config).dryRun(dryRun).build();
        final LicenseFormatter checker = new LicenseFormatter(heConfig);
        final Report report = checker.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        for (String unknownHeaderFile : unknownHeaderFiles) {
            log.warn("Processing unknown file: %s".formatted(unknownHeaderFile));
        }

        final List<Map.Entry<String, String>> updatedHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> !ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .filter(e -> !ReportConstants.RESULT_NOOP.equals(e.getValue()))
                .toList();

        if (updatedHeaderFiles.isEmpty()) {
            log.info("All files have proper header.");
            return;
        }

        if (!dryRun) {
            for (Map.Entry<String, String> updatedHeaderFile : updatedHeaderFiles) {
                log.info("Updated header for file: %s".formatted(updatedHeaderFile.getKey()));
            }
        }
    }
}
