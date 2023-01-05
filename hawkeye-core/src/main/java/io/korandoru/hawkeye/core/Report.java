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

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Data
@RequiredArgsConstructor
public class Report {
    public enum Action {CHECK, FORMAT, REMOVE}

    public enum Result {
        /**
         * For check: header is OK
         */
        PRESENT,

        /**
         * For check: means the file does not contain a header
         */
        MISSING,

        /**
         * For format or remove when no operation were done
         */
        NOOP,

        /**
         * For format, when header is added
         */
        ADDED,

        /**
         * For format, when header is replaced
         */
        REPLACED,

        /**
         * For remove, when header is removed
         */
        REMOVED,

        /**
         * For any actions: means the file extension is unknown
         */
        UNKNOWN,
    }

    private final Action action;
    private final Instant timestamp = Instant.now();
    private final Map<String, Result> results = new ConcurrentHashMap<>();

    @SneakyThrows
    void add(File file, Result result) {
        results.put(file.getCanonicalPath(), result);
    }
}
