/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.korandoru.hawkeye.command;

import io.korandoru.hawkeye.core.LicenseFormatter;
import io.korandoru.hawkeye.core.Report;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@CommandLine.Command(
        name = "format",
        version = CommandConstants.VERSION,
        mixinStandardHelpOptions = true,
        description = "Format license headers."
)
@Slf4j
public class HawkEyeCommandFormat implements Callable<Integer> {

    @CommandLine.Mixin
    private CommandOptions options;

    @CommandLine.Option(names = "--dry-run", description = "whether update file in place")
    public boolean dryRun;

    @Override
    public Integer call() {
        final HawkEyeConfig config = HawkEyeConfig.of(options.config).dryRun(dryRun).build();
        final LicenseFormatter formatter = new LicenseFormatter(config);
        final Report report = formatter.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> Report.Result.UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        final List<Map.Entry<String, Report.Result>> updatedHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> e.getValue() != Report.Result.UNKNOWN)
                .filter(e -> e.getValue() != Report.Result.NOOP)
                .toList();

        if (!unknownHeaderFiles.isEmpty()) {
            log.warn("Processing unknown files: {}", unknownHeaderFiles);
        }

        if (updatedHeaderFiles.isEmpty()) {
            log.info("All files have proper header.");
        } else if (!dryRun) {
            log.info("Updated header for files: {}", updatedHeaderFiles);
        }

        return updatedHeaderFiles.isEmpty() ? 0 : 1;
    }
}
