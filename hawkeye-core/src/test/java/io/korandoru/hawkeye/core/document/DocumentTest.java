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

import static org.assertj.core.api.Assertions.assertThat;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.resource.UrlHeaderSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class DocumentTest {

    @Test
    void testCreate() {
        final Document doc = new Document(
                new File("src/test/resources/doc/doc1.txt"),
                DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                StandardCharsets.UTF_8,
                new String[]{"copyright"},
                d -> Map.of("year", "2008"));
        assertThat(doc.getFile().getName()).isEqualTo("doc1.txt");
        assertThat(doc.isNotSupported()).isFalse();
    }

    @Test
    void testUnsupported() {
        final Document doc = new Document(
                new File("src/test/resources/doc/doc1.txt"),
                DocumentType.UNKNOWN.getDefaultHeaderType().getDefinition(),
                StandardCharsets.UTF_8,
                new String[]{"copyright"},
                d -> Map.of("year", "2008"));
        assertThat(doc.getFile().getName()).isEqualTo("doc1.txt");
        assertThat(doc.isNotSupported()).isTrue();
    }

    @Test
    void testHasHeader() throws Exception {
        final Document doc = new Document(
                new File("src/test/resources/doc/doc1.txt"),
                DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                StandardCharsets.UTF_8,
                new String[]{"copyright"},
                d -> Map.of("year", "2008"));
        final Header header = new Header(new UrlHeaderSource(new File("src/test/resources/test-header1.txt").toURI().toURL()));
        assertThat(doc.hasHeader(header, true)).isFalse();
    }

    @Test
    void testIsHeader() throws Exception {
        final Header header = new Header(new UrlHeaderSource(new File("src/test/resources/test-header1.txt").toURI().toURL()));
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc1.txt"),
                    DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            assertThat(doc.is(header)).isFalse();
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/test-header1.txt"),
                    DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            assertThat(doc.is(header)).isTrue();
        }
    }

    @Test
    void testRemoveHeader() throws Exception {
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc1.txt"),
                    DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo(FileUtils.readFileToString(new File("src/test/resources/doc/doc1.txt"), StandardCharsets.UTF_8));
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc2.txt"),
                    DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("some data\r\n");
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc3.txt"),
                    DocumentType.TXT.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("some data\r\nand other data\r\n");
        }
    }

    @Test
    void testRemoveHeaderXML() {
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc4.xml"),
                    DocumentType.XML.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n<web-app/>\r\n");
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc5.xml"),
                    DocumentType.XML.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n\r\n<web-app/>\r\n");
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc6.xml"),
                    DocumentType.XML.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8,
                    new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n<web-app/>\r\n");
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc8.xml"),
                    DocumentType.XML.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8, new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).contains("no key word");
        }
        {
            final Document doc = new Document(
                    new File("src/test/resources/doc/doc9.xml"),
                    DocumentType.XML.getDefaultHeaderType().getDefinition(),
                    StandardCharsets.UTF_8, new String[]{"copyright"},
                    d -> Map.of("year", "2008"));
            doc.removeHeader();
            assertThat(doc.getContent()).isEqualTo("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n\r\n\r\n\r\n<web-app>\r\n\r\n</web-app>\r\n");
        }
    }
}
