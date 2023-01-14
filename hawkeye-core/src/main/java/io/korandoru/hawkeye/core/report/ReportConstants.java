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

package io.korandoru.hawkeye.core.report;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReportConstants {

    public static final String ACTION_CHECK = "check";
    public static final String ACTION_FORMAT = "format";
    public static final String ACTION_REMOVE = "remove";

    /**
     * For check: header is OK
     */
    public static final String RESULT_PRESENT = "present";

    /**
     * For check: means the file does not contain a header
     */
    public static final String RESULT_MISSING = "missing";

    /**
     * For format or remove when no operation were done
     */
    public static final String RESULT_NOOP = "noop";

    /**
     * For format, when header is added
     */
    public static final String RESULT_ADDED = "added";

    /**
     * For format, when header is replaced
     */
    public static final String RESULT_REPLACED = "replaced";

    /**
     * For remove, when header is removed
     */
    public static final String RESULT_REMOVED = "removed";

    /**
     * For any actions: means the file extension is unknown
     */
    public static final String RESULT_UNKNOWN = "unknown";
}
