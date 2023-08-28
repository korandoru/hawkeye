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

package io.korandoru.hawkeye.core.document;

import io.korandoru.hawkeye.core.FileContent;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.header.HeaderDefinition;
import io.korandoru.hawkeye.core.header.HeaderParser;
import io.korandoru.hawkeye.core.header.HeaderType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

public final class Document {
    @Getter
    private final File file;

    @Getter
    private final HeaderDefinition headerDefinition;

    private final Charset encoding;
    private final DocumentPropertiesLoader documentPropertiesLoader;
    private final HeaderParser parser;

    public Document(
            File file,
            HeaderDefinition headerDefinition,
            Charset encoding,
            String[] keywords,
            DocumentPropertiesLoader documentPropertiesLoader) {
        this.file = file;
        this.headerDefinition = headerDefinition;
        this.encoding = encoding;
        this.documentPropertiesLoader = documentPropertiesLoader;
        this.parser = new HeaderParser(new FileContent(file, encoding), headerDefinition, keywords);
    }

    public String getFilePath() {
        return getFile().getPath().replace('\\', '/');
    }

    public boolean isNotSupported() {
        return headerDefinition == null
                || HeaderType.UNKNOWN.getDefinition().getType().equals(headerDefinition.getType());
    }

    @SneakyThrows
    public boolean hasHeader(Header header, boolean strictCheck) {
        if (!strictCheck) {
            final String fileHeaderOnOneLine = readFileHeaderOnOneLine(header);
            final String headerOnOnelIne = mergeProperties(header.getHeaderContentOneLine());
            return fileHeaderOnOneLine.contains(headerOnOnelIne);
        }

        return header.isMatchForText(this, headerDefinition, encoding);
    }

    private String readFileHeaderOnOneLine(Header header) throws IOException {
        final String firstLines = FileUtils.readLines(file, encoding).stream()
                .limit(header.getHeaderContentLines().length + 10)
                .collect(Collectors.joining());
        String fileHeader = firstLines.strip();
        fileHeader = fileHeader.replace(headerDefinition.getFirstLine().trim(), "");
        fileHeader = fileHeader.replace(headerDefinition.getEndLine().trim(), "");
        fileHeader = fileHeader.replace(headerDefinition.getBeforeEachLine().trim(), "");
        fileHeader = fileHeader.replace(headerDefinition.getAfterEachLine().trim(), "");
        return StringUtils.deleteWhitespace(fileHeader);
    }

    public void updateHeader(Header header) {
        String headerStr = header.buildForDefinition(parser.getHeaderDefinition());
        parser.getFileContent().insert(parser.getBeginPosition(), mergeProperties(headerStr));
    }

    public String mergeProperties(String str) {
        final StringSubstitutor substitutor = new StringSubstitutor(documentPropertiesLoader.load(this));
        return substitutor.replace(str);
    }

    public void save() {
        saveTo(file);
    }

    @SneakyThrows
    public void saveTo(File dest) {
        FileUtils.writeStringToFile(dest, parser.getFileContent().getContent(), encoding);
    }

    public String getContent() {
        return parser.getFileContent().getContent();
    }

    public void removeHeader() {
        if (headerDetected()) {
            parser.getFileContent().delete(parser.getBeginPosition(), parser.getEndPosition());
        }
    }

    @SneakyThrows
    public boolean is(Header header) {
        return header.getLocation().isFromUrl(this.file.toURI().toURL());
    }

    public boolean headerDetected() {
        return parser.isExistingHeader();
    }

    @Override
    public String toString() {
        return "Document " + getFilePath();
    }
}
