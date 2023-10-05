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

package io.korandoru.hawkeye.gradle.plugin;

import io.korandoru.hawkeye.core.LicenseChecker;
import io.korandoru.hawkeye.core.LicenseFormatter;
import io.korandoru.hawkeye.core.LicenseRemover;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskProvider;

public class HawkEyeExtension {
    public static final String NAME = "hawkeye";

    private final List<HawkEyeConfig> configs = new ArrayList<>();
    private final Project project;

    public void addConfig(String configFile, Action<HawkEyeConfig.Builder> action) {
        addConfig(Path.of(configFile), action);
    }

    public void addConfig(Path configFile, Action<HawkEyeConfig.Builder> action) {
        addConfig(configFile.toFile(), action);
    }

    public void addConfig(File configFile, Action<HawkEyeConfig.Builder> action) {
        final HawkEyeConfig.Builder builder = HawkEyeConfig.of(configFile);
        action.execute(builder);
        configs.add(builder.build());
    }

    public HawkEyeExtension(Project project) {
        this.project = project;

        final TaskProvider<?> checkTask = project.getTasks().register("hawkeyeCheck");
        final TaskProvider<?> formatTask = project.getTasks().register("hawkeyeFormat");
        final TaskProvider<?> removeTask = project.getTasks().register("hawkeyeRemove");

        project.afterEvaluate(ignore -> {
            checkTask.configure(task -> doCheck());
            formatTask.configure(task -> doFormat());
            removeTask.configure(task -> doRemove());
        });
    }

    private void doCheck() {
        final Logger log = project.getLogger();

        for (HawkEyeConfig config : configs) {
            final Report report = new LicenseChecker(config).call();

            final List<String> unknownHeaderFiles = report.getResults().entrySet().stream()
                    .filter(e -> ReportConstants.RESULT_UNKNOWN.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
            final List<String> missingHeaderFiles = report.getResults().entrySet().stream()
                    .filter(e -> ReportConstants.RESULT_MISSING.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();

            if (!unknownHeaderFiles.isEmpty()) {
                log.warn("Processing unknown files: %s".formatted(unknownHeaderFiles));
            }

            if (missingHeaderFiles.isEmpty()) {
                log.info("No missing header file has been found.");
                return;
            }

            for (String filename : missingHeaderFiles) {
                log.error("Found missing header files: %s".formatted(filename));
            }
            throw new GradleException("Found missing header files.");
        }
    }

    private void doFormat() {
        final Logger log = project.getLogger();

        for (HawkEyeConfig config : configs) {
            final LicenseFormatter formatter = new LicenseFormatter(config);
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
            } else if (!config.isDryRun()) {
                log.info("Updated header for files: %s".formatted(updatedHeaderFiles));
            }
        }
    }

    private void doRemove() {
        final Logger log = project.getLogger();

        for (HawkEyeConfig config : configs) {
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
                log.warn("Processing unknown files: %s".formatted(unknownHeaderFiles));
            }

            if (removedHeaderFiles.isEmpty()) {
                log.info("No file has been removed header.");
            } else if (!config.isDryRun()) {
                log.info("Removed header for files: %s".formatted(removedHeaderFiles));
            }
        }
    }
}
