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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

public final class UrlHeaderSource extends HeaderSource {
    private final URL url;

    public UrlHeaderSource(URL url) throws IOException {
        super(IOUtils.toString(url, StandardCharsets.UTF_8), false);
        this.url = url;
    }

    public UrlHeaderSource(URL url, Charset charset) throws IOException {
        super(IOUtils.toString(url, charset), false);
        this.url = url;
    }

    @Override
    public boolean isFromUrl(URL location) {
        return Objects.equals(this.url, location);
    }

    @Override
    public String toString() {
        return url + ": " + getContent();
    }
}
