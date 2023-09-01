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

import io.korandoru.hawkeye.core.LicenseFormatter;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "format")
public class FormatMojo extends AbstractMojo {
    @Override
    public void execute() {
        final Log log = getLog();
        log.info("Formatting license headers... with config: %s, dryRun: %s".formatted(configLocation, dryRun));

        final LicenseFormatter formatter = new LicenseFormatter(configBuilder().build());
        final Report report = formatter.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        final List<Map.Entry<String, String>> updatedHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> !ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .filter(e -> !ReportConstants.RESULT_NOOP.equals(e.getValue()))
                .toList();

        if (!unknownHeaderFiles.isEmpty()) {
            log.warn("Processing unknown files: %s".formatted(unknownHeaderFiles));
        }

        if (updatedHeaderFiles.isEmpty()) {
            log.info("All files have proper header.");
        } else if (!dryRun) {
            log.info("Updated header for files: %s".formatted(updatedHeaderFiles));
        }
    }
}
