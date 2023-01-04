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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Defines the default header definitions available out of the box.
 */
public enum HeaderType {
    ASP("<%", "' ", "%>", "", null, "(\\s|\\t)*<%( .*)?$", ".*%>(\\s|\\t)*$", true, true, false),
    JAVADOC_STYLE("/**", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    SCRIPT_STYLE("#", "# ", "#EOL", "", "^#!.*$", "#.*$", "#.*$", false, false, false),
    SLASHSTAR_STYLE("/*", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    UNKNOWN("", "", "", "", null, null, null, false, false, false);

    private final HeaderDefinition definition;

    HeaderType(
            String firstLine,
            String beforeEachLine,
            String endLine,
            String afterEachLine,
            String skipLinePattern,
            String firstLineDetectionPattern,
            String lastLineDetectionPattern,
            boolean allowBlankLines,
            boolean multiLine,
            boolean padLines
    ) {
        this.definition = HeaderDefinition.builder()
                .type(name())
                .firstLine(firstLine)
                .endLine(endLine)
                .beforeEachLine(beforeEachLine)
                .afterEachLine(afterEachLine)
                .skipLinePattern(Optional.ofNullable(skipLinePattern).map(Pattern::compile).orElse(null))
                .firstLineDetectionPattern(Optional.ofNullable(firstLineDetectionPattern).map(Pattern::compile).orElse(null))
                .lastLineDetectionPattern(Optional.ofNullable(lastLineDetectionPattern).map(Pattern::compile).orElse(null))
                .allowBlankLines(allowBlankLines)
                .multipleLines(multiLine)
                .padLines(padLines)
                .build();
    }

    /**
     * Returns the {@link HeaderDefinition} which corresponds to this enumeration instance.
     *
     * @return The header definition.
     */
    public HeaderDefinition getDefinition() {
        return definition;
    }

    private static final Map<String, HeaderDefinition> DEFINITIONS = new HashMap<>(values().length);

    static {
        for (HeaderType type : values()) {
            DEFINITIONS.put(type.getDefinition().getType(), type.getDefinition());
        }
    }

    /**
     * Returns the header definitions of every default definitions declared by this enumeration as a map using the
     * header type name as key.
     *
     * @return The default definitions declared by this enumeration.
     */
    public static Map<String, HeaderDefinition> defaultDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }
}
