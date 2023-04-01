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

package io.korandoru.hawkeye.core.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
class ConfigFileModel {

    @Builder.Default
    private final Path baseDir = Path.of(".");
    private final String inlineHeader;
    private final String headerPath;
    @Builder.Default
    private final List<String> additionalHeaders = Collections.emptyList();
    @Builder.Default
    private final boolean strictCheck = true;
    @Builder.Default
    private final boolean useDefaultExcludes = true;
    @Builder.Default
    private final boolean useDefaultMapping = true;
    @Builder.Default
    private final List<String> includes = Collections.emptyList();
    @Builder.Default
    private final List<String> excludes = Collections.emptyList();
    private final List<String> keywords = Collections.singletonList("copyright");
    @Builder.Default
    private final Map<String, String> properties = Collections.emptyMap();
    private final MappingsModel mapping;
}
