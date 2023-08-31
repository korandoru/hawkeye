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

import io.korandoru.hawkeye.core.LicenseChecker;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check", aggregator = true, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoFailureException {
        final Log log = getLog();
        log.info("Checking license headers... with config: %s".formatted(config));

        final HawkEyeConfig heConfig = HawkEyeConfig.of(config).build();
        final LicenseChecker checker = new LicenseChecker(heConfig);
        final Report report = checker.call();

        final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (String unknownHeaderFile : unknownHeaderFiles) {
            log.warn("Processing unknown file: %s".formatted(unknownHeaderFile));
        }

        final List<String> missingHeaderFiles = report.getResults().entrySet().stream()
                .filter(e -> ReportConstants.RESULT_MISSING.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        if (missingHeaderFiles.isEmpty()) {
            log.info("No missing header file has been found.");
            return;
        }

        for (String missingHeaderFile : missingHeaderFiles) {
            log.error("Found missing header file: %s".formatted(missingHeaderFile));
        }

        throw new MojoFailureException("Missing header files found");
    }
}
