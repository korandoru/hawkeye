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

package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.header.Header;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseFormatter extends LicenseProcessor {

    public LicenseFormatter(HawkEyeConfig config) {
        super(config, Report.Action.FORMAT);
    }

    @Override
    protected void onHeaderNotFound(Document document, Header header, Report report) {
        report.add(document.getFile(), document.headerDetected() ? Report.Result.REPLACED : Report.Result.ADDED);

        if (document.headerDetected()) {
            document.removeHeader();
        }
        document.updateHeader(header);

        if (config.isDryRun()) {
            String name = document.getFile().getName() + ".formatted";
            File copy = new File(document.getFile().getParentFile(), name);
            log.info("Result saved to: {}", copy);
            document.saveTo(copy);
        } else {
            document.save();
        }
    }

    @Override
    protected void onExistingHeader(Document document, Header header, Report report) {
        report.add(document.getFile(), Report.Result.NOOP);
    }
}
