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

package io.korandoru.hawkeye.core.config;

import io.korandoru.hawkeye.core.header.HeaderDefinition;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
class HeaderStyleModel {
    /**
     * The first fixed line of this header. Default to none.
     */
    @Builder.Default
    private final String firstLine = "";

    /**
     * The last fixed line of this header. Default to none.
     */
    @Builder.Default
    private final String endLine = "";

    /**
     * The characters to prepend before each license header lines. Default to empty.
     */
    @Builder.Default
    private final String beforeEachLine = "";

    /**
     * The characters to append after each license header lines. Default to empty.
     */
    @Builder.Default
    private final String afterEachLine = "";

    /**
     * Only for multi-line comments: specify if blank lines are allowed.
     * <p>
     * Default to false because most of the time, a header has some characters on each line.
     */
    @Builder.Default
    private final boolean allowBlankLines = false;

    /**
     * Specify whether this is a multi-line comment style or not.
     * <p>
     * A multi-line comment style is equivalent to what we have in Java, where a first line and line will delimit a whole
     * multi-line comment section.
     * <p>
     * A style that is not multi-line is usually repeating in each line the characters before and after each line to delimit a one-line comment.
     */
    @Builder.Default
    private final boolean multipleLines = true;

    /**
     * Only for non multi-line comments: specify if some spaces should be added after the header line and before the {@link #afterEachLine} characters so that all the lines are aligned.
     * <p>
     * Default to false.
     */
    @Builder.Default
    private final boolean padLines = false;

    /**
     * A regex to define a first line in a file that should be skipped and kept untouched, like the XML declaration at the top of XML documents.
     * <p>
     * Default to none.
     */
    private final String skipLinePattern;

    /**
     * The regex used to detect the start of a header section or line.
     */
    private final String firstLineDetectionPattern;

    /**
     * The regex used to detect the end of a header section or line.
     */
    private final String lastLineDetectionPattern;

    HeaderDefinition toHeaderDefinition(String name) {
        return HeaderDefinition.builder()
                .type(name)
                .firstLine(firstLine)
                .endLine(endLine)
                .beforeEachLine(beforeEachLine)
                .afterEachLine(afterEachLine)
                .skipLinePattern(Optional.ofNullable(skipLinePattern).map(Pattern::compile).orElse(null))
                .firstLineDetectionPattern(Optional.ofNullable(firstLineDetectionPattern).map(Pattern::compile).orElse(null))
                .lastLineDetectionPattern(Optional.ofNullable(lastLineDetectionPattern).map(Pattern::compile).orElse(null))
                .allowBlankLines(allowBlankLines)
                .multipleLines(multipleLines)
                .padLines(padLines)
                .build();
    }
}
