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

package io.korandoru.hawkeye.command;

import io.korandoru.hawkeye.core.LicenseRemover;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@CommandLine.Command(
        name = "remove",
        version = CommandConstants.VERSION,
        mixinStandardHelpOptions = true,
        description = "Remove license headers."
)
@Slf4j
public class HawkEyeCommandRemove implements Callable<Integer> {

    @CommandLine.Mixin
    private CommandOptions options;

    @CommandLine.Option(names = "--dry-run", description = "whether update file in place")
    public boolean dryRun;

    @Override
    public Integer call() {
        final HawkEyeConfig config = HawkEyeConfig.of(options.config).dryRun(dryRun).build();
        final LicenseRemover remover = new LicenseRemover(config);
        final Report report = remover.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        final List<String> removedHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_REMOVED.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (!unknownHeaderFiles.isEmpty()) {
            log.warn("Processing unknown files: {}", unknownHeaderFiles);
        }

        if (removedHeaderFiles.isEmpty()) {
            log.info("No file has been removed header.");
        } else if (!dryRun) {
            log.info("Removed header for files: {}", removedHeaderFiles);
        }

        return removedHeaderFiles.isEmpty() ? 0 : 1;
    }
}
