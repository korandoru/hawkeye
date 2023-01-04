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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(builderClassName = "Builder")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HeaderDefinition {
    private final String type;
    private final String firstLine;
    private final String endLine;
    private final String beforeEachLine;
    private final String afterEachLine;

    private final boolean allowBlankLines;
    private final boolean multipleLines;

    private final boolean padLines;

    private final Pattern skipLinePattern;
    private final Pattern firstLineDetectionPattern;
    private final Pattern lastLineDetectionPattern;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HeaderDefinition that = (HeaderDefinition) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }

    @Override
    public String toString() {
        return type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Builder() {
            // always use HeaderDefinition.builder()
        }

        public HeaderDefinition build() {
            Objects.requireNonNull(type, "type");

            if (!type.equalsIgnoreCase("unknown")) {
                Objects.requireNonNull(firstLineDetectionPattern, "firstLineDetectionPattern");
                Objects.requireNonNull(lastLineDetectionPattern, "lastLineDetectionPattern");
            }

            if (allowBlankLines && !multipleLines) {
                final String template = "Header style %s is configured to allow blank lines, so it should be set as a multi-line header style";
                throw new IllegalArgumentException(template.formatted(type.toLowerCase()));
            }

            return new HeaderDefinition(
                    type.toLowerCase(),
                    Optional.ofNullable(firstLine).orElse(""),
                    Optional.ofNullable(endLine).orElse(""),
                    Optional.ofNullable(beforeEachLine).orElse(""),
                    Optional.ofNullable(afterEachLine).orElse(""),
                    allowBlankLines,
                    multipleLines,
                    padLines,
                    skipLinePattern,
                    firstLineDetectionPattern,
                    lastLineDetectionPattern);
        }
    }
}
