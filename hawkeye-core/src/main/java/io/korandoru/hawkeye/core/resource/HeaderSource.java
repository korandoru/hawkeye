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

package io.korandoru.hawkeye.core.resource;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * A wrapper class of license template text
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract sealed class HeaderSource permits LiteralHeaderSource, UrlHeaderSource {

    private final String content;
    private final boolean inline;

    public abstract boolean isFromUrl(URL location);

    public static HeaderSource of(String inlineHeader, String headerPath, ResourceFinder finder) {
        return of(inlineHeader, headerPath, finder, StandardCharsets.UTF_8);
    }

    public static HeaderSource of(String inlineHeader, String headerPath, ResourceFinder finder, Charset charset) {
        if (StringUtils.isNotEmpty(inlineHeader)) {
            return new LiteralHeaderSource(inlineHeader);
        }

        if (StringUtils.isNotEmpty(headerPath)) {
            try {
                final URL headerUrl = finder.findResource(headerPath);
                return new UrlHeaderSource(headerUrl, charset);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot read header document " + headerPath, e);
            }
        }

        throw new IllegalArgumentException("Either inlineHeader or header path needs to be specified");
    }
}
