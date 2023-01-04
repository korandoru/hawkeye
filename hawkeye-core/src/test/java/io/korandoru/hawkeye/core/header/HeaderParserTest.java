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

import static org.assertj.core.api.Assertions.assertThat;
import io.korandoru.hawkeye.core.FileContent;
import java.io.File;
import org.junit.jupiter.api.Test;

class HeaderParserTest {

    @Test
    void testNoHeader() {
        final FileContent content = new FileContent(new File("src/test/resources/doc/doc1.txt"));
        final HeaderParser parser = new HeaderParser(content, HeaderType.TEXT.getDefinition(), new String[]{"copyright"});
        assertThat(parser.isExistingHeader()).isFalse();
    }

    @Test
    void testHasHeader() {
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc2.txt"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.TEXT.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isTrue();
            assertThat(parser.getBeginPosition()).isEqualTo(0);
            assertThat(parser.getEndPosition()).isEqualTo(43);
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc3.txt"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.TEXT.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isTrue();
            assertThat(parser.getBeginPosition()).isEqualTo(0);
            assertThat(parser.getEndPosition()).isEqualTo(49);
        }
    }

    @Test
    void testParsingXML() {
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc4.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isTrue();
            assertThat(parser.getBeginPosition()).isEqualTo(45);
            assertThat(parser.getEndPosition()).isEqualTo(862);
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc5.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isTrue();
            assertThat(parser.getBeginPosition()).isEqualTo(45);
            assertThat(parser.getEndPosition()).isEqualTo(864);
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc6.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isFalse();
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc7.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isFalse();
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc8.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isFalse();
        }
        {
            final FileContent content = new FileContent(new File("src/test/resources/doc/doc9.xml"));
            final HeaderParser parser = new HeaderParser(content, HeaderType.XML_STYLE.getDefinition(), new String[]{"copyright"});
            assertThat(parser.isExistingHeader()).isTrue();
            assertThat(parser.getBeginPosition()).isEqualTo(45);
            assertThat(parser.getEndPosition()).isEqualTo(864);
        }
    }
}
