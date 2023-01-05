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

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class HawkEyeConfig {

    @Builder.Default
    private final String baseDir = ".";
    private final String inlineHeader;
    private final String headerPath;
    @Builder.Default
    private final boolean strictCheck = true;
    @Builder.Default
    private final boolean useDefaultExcludes = true;
    @Builder.Default
    private final List<String> includes = Collections.emptyList();
    @Builder.Default
    private final List<String> excludes = Collections.emptyList();
    private final List<String> keywords = Collections.singletonList("copyright");
    private final Map<String, String> properties;

    @SneakyThrows
    public static HawkEyeConfig of(URL source) {
        final TomlMapper mapper = new TomlMapper();
        return mapper.readValue(source, HawkEyeConfig.class);
    }

    @SneakyThrows
    public static HawkEyeConfig of(File source) {
        final TomlMapper mapper = new TomlMapper();
        return mapper.readValue(source, HawkEyeConfig.class);
    }
}
