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

import io.korandoru.hawkeye.core.header.HeaderDefinition;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

public final class DocumentFactory {

    private final Map<String, String> mapping;
    private final Map<String, HeaderDefinition> definitions;
    private final File basedir;
    private final Charset encoding;
    private final String[] keywords;
    private final DocumentPropertiesLoader documentPropertiesLoader;

    public DocumentFactory(
            final File basedir,
            final Map<String, String> mapping,
            final Map<String, HeaderDefinition> definitions,
            final Charset encoding,
            final String[] keywords,
            final DocumentPropertiesLoader documentPropertiesLoader
    ) {
        this.mapping = mapping;
        this.definitions = definitions;
        this.basedir = basedir;
        this.encoding = encoding;
        this.keywords = keywords.clone();
        this.documentPropertiesLoader = documentPropertiesLoader;
    }

    public Document createDocuments(final String file) {
        return getWrapper(file);
    }

    private Document getWrapper(final String file) {
        final String lowerFileName = FilenameUtils.getName(file).toLowerCase();

        String headerType = mapping.get("");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String lowerKey = entry.getKey().toLowerCase();
            if (lowerFileName.endsWith("." + lowerKey) || lowerFileName.equals(lowerKey)) {
                headerType = entry.getValue().toLowerCase();
                break;
            }
        }

        return new Document(
                new File(basedir, file),
                definitions.get(headerType),
                encoding,
                keywords,
                documentPropertiesLoader);
    }

}
