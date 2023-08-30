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

import io.korandoru.hawkeye.core.LicenseRemover;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "remove")
public class RemoveMojo extends AbstractMojo {
    @Override
    public void execute() {
        final Log log = getLog();
        log.info("Removing license headers... with cfg: %s, dryRun: %s".formatted(config, dryRun));

        final HawkEyeConfig heConfig = HawkEyeConfig.of(config).dryRun(dryRun).build();
        final LicenseRemover remover = new LicenseRemover(heConfig);
        final Report report = remover.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        for (String unknownHeaderFile : unknownHeaderFiles) {
            log.warn("Processing unknown file: %s".formatted(unknownHeaderFile));
        }

        final List<String> removedHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_REMOVED.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (removedHeaderFiles.isEmpty()) {
            log.info("No file has been removed header.");
            return;
        }
        if (!dryRun) {
            for (String removedHeaderFile : removedHeaderFiles) {
                log.info("Removed header for file: %s".formatted(removedHeaderFile));
            }
        }
    }
}
