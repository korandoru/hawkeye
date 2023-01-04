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
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public final class HeaderParser {

    private final int beginPosition;
    private final int endPosition;
    private final boolean existingHeader;
    private final FileContent fileContent;
    private final String[] keywords;
    private final HeaderDefinition headerDefinition;

    @Getter(AccessLevel.NONE)
    private String line;

    public HeaderParser(FileContent fileContent, HeaderDefinition headerDefinition, String[] keywords) {
        Objects.requireNonNull(fileContent, "Cannot create a header parser for null file content");
        Objects.requireNonNull(headerDefinition, "Cannot work on file header if the header definition is null");
        this.keywords = keywords.clone();
        this.headerDefinition = headerDefinition;
        this.fileContent = fileContent;
        this.beginPosition = findBeginPosition();
        this.existingHeader = hasHeader();
        this.endPosition = this.existingHeader ? findEndPosition() : -1;
    }

    private int findBeginPosition() {
        int beginPos = 0;
        line = fileContent.nextLine();
        if (headerDefinition.getSkipLinePattern() == null) {
            return beginPos;
        }

        // the format expect to find lines to be skipped
        while (line != null && !headerDefinition.isSkipLine(line)) {
            beginPos = fileContent.getPosition();
            line = fileContent.nextLine();
        }

        // at least we have found the line to skip, or we are the end of the file
        // this time we are going to skip next lines if they match the skip pattern
        while (line != null && headerDefinition.isSkipLine(line)) {
            beginPos = fileContent.getPosition();
            line = fileContent.nextLine();
        }

        if (line == null) {
            // After skipping everything we are at the end of the file
            // Header has to be at the file beginning
            beginPos = 0;
            fileContent.reset();
            line = fileContent.nextLine();
        }

        return beginPos;
    }

    private boolean hasHeader() {
        // skip blank lines
        while (line != null && "".equals(line.trim())) {
            line = fileContent.nextLine();
        }
        // check if there is already a header
        boolean gotHeader = false;
        if (headerDefinition.isFirstHeaderLine(line)) {
            StringBuilder inPlaceHeader = new StringBuilder();
            inPlaceHeader.append(line.toLowerCase());

            line = fileContent.nextLine();

            // skip blank lines before header text
            if (headerDefinition.isAllowBlankLines()) {
                while (line != null && "".equals(line.trim())) {
                    line = fileContent.nextLine();
                }
            }

            // first header detected line & potential blank lines have been detected
            // following lines should be header lines
            if (line == null) {
                // we detected previously a one line comment block that matches the header detection
                // it is not a header it is a comment
                return false;

            } else {
                inPlaceHeader.append(line.toLowerCase());
            }

            String before = StringUtils.stripEnd(headerDefinition.getBeforeEachLine(), null);
            if ("".equals(before) && !headerDefinition.isMultipleLines()) {
                before = headerDefinition.getBeforeEachLine();
            }

            boolean foundEnd = false;
            if (headerDefinition.isMultipleLines() && headerDefinition.isLastHeaderLine(line)) {
                foundEnd = true;

            } else {
                while ((line = fileContent.nextLine()) != null && line.startsWith(before)) {
                    inPlaceHeader.append(line.toLowerCase());
                    if (headerDefinition.isMultipleLines() && headerDefinition.isLastHeaderLine(line)) {
                        foundEnd = true;
                        break;
                    }
                }
            }

            // skip blank lines after header text
            if (headerDefinition.isMultipleLines() && headerDefinition.isAllowBlankLines() && !foundEnd) {
                do {
                    line = fileContent.nextLine();
                } while (line != null && "".equals(line.trim()));
                fileContent.rewind();

            } else if (!headerDefinition.isMultipleLines() && !foundEnd) {
                fileContent.rewind();
            }

            if (!headerDefinition.isMultipleLines()) {
                // keep track of the position for headers where the end line is the same as the before each line
                int pos = fileContent.getPosition();
                // check if the line is the end line
                while (line != null
                        && !headerDefinition.isLastHeaderLine(line)
                        && (headerDefinition.isAllowBlankLines() || !"".equals(line.trim()))
                        && line.startsWith(before)) {
                    line = fileContent.nextLine();
                }
                if (line == null) {
                    fileContent.resetTo(pos);
                }
            } else if (line != null) {
                // we could end up there if we still have some lines, but not matching "before".
                // This can be the last line in a multi line header
                int pos = fileContent.getPosition();
                line = fileContent.nextLine();
                if (line == null || !headerDefinition.isLastHeaderLine(line)) {
                    fileContent.resetTo(pos);
                }
            }
            gotHeader = true;
            for (String keyword : keywords) {
                if (inPlaceHeader.indexOf(keyword.toLowerCase()) == -1) {
                    gotHeader = false;
                    break;
                }
            }
        }
        return gotHeader;
    }

    private int findEndPosition() {
        // we check if there is a header, if the next line is the blank line of the header
        int end = fileContent.getPosition();
        line = fileContent.nextLine();
        if (beginPosition == 0) {
            while (line != null && "".equals(line.trim())) {
                end = fileContent.getPosition();
                line = fileContent.nextLine();
            }
        }
        if (headerDefinition.getEndLine().endsWith("EOL") && line != null && "".equals(line.trim())) {
            end = fileContent.getPosition();
        }
        return end;
    }
}
