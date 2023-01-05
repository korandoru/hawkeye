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

import io.korandoru.hawkeye.core.LicenseChecker;
import io.korandoru.hawkeye.core.Report;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = "check",
        version = CommandConstants.VERSION,
        mixinStandardHelpOptions = true,
        description = "Check license headers."
)
public class HawkEyeCommandCheck implements Callable<Integer> {

    @CommandLine.Mixin
    private CommandOptions options;

    @Override
    public Integer call() {
        final HawkEyeConfig config = HawkEyeConfig.of(options.config);
        final LicenseChecker checker = new LicenseChecker(config);
        final Report report = checker.call();
        final boolean hasHeaderNotFoundFiles = report.getResults()
                .values()
                .stream()
                .anyMatch(Report.Result.MISSING::equals);
        return hasHeaderNotFoundFiles ? 1 : 0;
    }
}
