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

package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.document.Document;
import java.io.File;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class LicenseProcessUtils {

    public static void save(Document document, boolean dryRun, String suffix) {
        if (!dryRun) {
            document.save();
            return;
        }

        final String filename = document.getFile().getName() + suffix;
        final File copy = new File(document.getFile().getParentFile(), filename);
        document.saveTo(copy);

        log.info("Result saved to: {}", copy);
    }

}
