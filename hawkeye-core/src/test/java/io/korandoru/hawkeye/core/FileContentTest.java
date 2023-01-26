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

package io.korandoru.hawkeye.core;

import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileContentTest {

    @Test
    void testStates() {
        FileContent c = new FileContent(new File("src/test/data/compileCP/test2.txt"));
        Assertions.assertEquals("a", c.nextLine());
        Assertions.assertEquals(3, c.getPosition());
    }

    @Test
    void testDelete() {
        FileContent c = new FileContent(new File("src/test/data/compileCP/test2.txt"));
        c.delete(2, 8);
        Assertions.assertEquals("a\r\nd\r\ne\r\nf\r\ng\r\nh\r\ni\r\n", c.getContent());
    }

    @Test
    void testInsert() {
        FileContent c = new FileContent(new File("src/test/data/compileCP/test2.txt"));
        c.insert(4, "_hello");
        Assertions.assertEquals("a\r\nb_hello\r\nc\r\nd\r\ne\r\nf\r\ng\r\nh\r\ni\r\n", c.getContent());
    }

    @Test
    void testRemoveDuplicatedEmptyEndLines() {
        FileContent c = new FileContent(new File("src/test/data/compileCP/test3.txt"));
        c.removeDuplicatedEmptyEndLines();
        Assertions.assertEquals("a\r\nb\r\n", c.getContent());

        c = new FileContent(new File("src/test/data/compileCP/test4.txt"));
        c.removeDuplicatedEmptyEndLines();
        Assertions.assertEquals("\r\n", c.getContent());
    }

}
