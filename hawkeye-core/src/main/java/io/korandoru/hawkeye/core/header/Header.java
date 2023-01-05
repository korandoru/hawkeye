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

package io.korandoru.hawkeye.core.header;

import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

@Getter
public final class Header {

    private final HeaderSource location;
    private final String headerContent;
    private final String headerContentOneLine;
    private final String[] headerContentLines;
    private final int maxLength;

    public Header(HeaderSource location) {
        this.location = location;
        this.headerContent = location.getContent();
        this.headerContentLines = location.getContent().lines().toArray(String[]::new);
        this.headerContentOneLine = StringUtils.deleteWhitespace(this.headerContent);
        this.maxLength = location.getContent().lines().map(String::length).max(Integer::compareTo).orElse(0);
    }

    @Override
    public String toString() {
        return headerContent;
    }

    public String buildForDefinition(HeaderDefinition type) {
        return buildForDefinition(type, System.lineSeparator());
    }

    @VisibleForTesting
    String buildForDefinition(HeaderDefinition type, String endOfLine) {
        final StringBuilder newHeader = new StringBuilder();

        if (StringUtils.isNotEmpty(type.getFirstLine())) {
            final String firstLine = type.getFirstLine().replace("EOL", endOfLine);
            newHeader.append(firstLine);
            if (!firstLine.equals(endOfLine)) {
                newHeader.append(endOfLine);
            }
        }

        for (String line : this.headerContentLines) {
            final String before = type.getBeforeEachLine().replace("EOL", endOfLine);
            final String after = type.getAfterEachLine().replace("EOL", endOfLine);
            final String str;

            if (type.isPadLines()) {
                str = before + StringUtils.rightPad(line, maxLength) + after;
            } else {
                str = before + line + after;
            }

            newHeader.append(str.stripTrailing());
            newHeader.append(endOfLine);
        }

        if (StringUtils.isNotEmpty(type.getEndLine())) {
            String endLine = type.getEndLine().replace("EOL", endOfLine);
            newHeader.append(endLine);
            if (!endLine.equals(endOfLine)) {
                newHeader.append(endOfLine);
            }
        }

        return newHeader.toString();
    }

    public boolean isMatchForText(Document d, HeaderDefinition headerDefinition, Charset encoding) throws IOException {
        final String firstLines = FileUtils.readLines(d.getFile(), encoding)
                .stream()
                .limit(headerContentLines.length + 10)
                .collect(Collectors.joining("\n", "", "\n"));
        final String fileHeader = firstLines.replaceAll(" *\r?\n", "\n");
        final String expectedHeader = d.mergeProperties(buildForDefinition(headerDefinition));
        return fileHeader.contains(expectedHeader);
    }
}
