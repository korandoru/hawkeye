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

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;

public class LicenseFormatter extends LicenseProcessor {

    public LicenseFormatter(HawkEyeConfig config) {
        super(config, Report.Action.FORMAT);
    }

    @Override
    protected void onHeaderNotFound(Document document, Header header, Report report) {
        if (document.headerDetected()) {
            document.removeHeader();
            report.add(document.getFile(), Report.Result.REPLACED);
        } else {
            report.add(document.getFile(), Report.Result.ADDED);
        }
        document.updateHeader(header);
        document.save();
    }

    @Override
    protected void onExistingHeader(Document document, Header header, Report report) {
        report.add(document.getFile(), Report.Result.NOOP);
    }

    public static void main(String[] args) throws Exception {
        final var mapper = new TomlMapper();
        final var config = mapper.readValue(HawkEyeConfig.class.getResource("/hawkeye.toml"), HawkEyeConfig.class);
        final var checker = new LicenseFormatter(config);
        final var report = checker.call();
        report.getResults().forEach((path, result) -> {
            if (result != Report.Result.NOOP) {
                System.out.println(path + "=" + result);
            }
        });
    }
}
