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

package io.korandoru.hawkeye.core.config;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Data
@Builder(builderClassName = "Builder", builderMethodName = "")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HawkEyeConfig {

    /// region options in config models
    private final Path baseDir;
    private final String inlineHeader;
    private final String headerPath;
    private final boolean strictCheck;
    private final boolean useDefaultExcludes;
    private final boolean useDefaultMapping;
    private final List<String> includes;
    private final List<String> excludes;
    private final List<String> keywords;
    private final Map<String, String> properties;
    private final Map<String, String> mapping;
    /// endregion

    private final boolean dryRun;

    @SneakyThrows
    public static Builder of(File source) {
        final TomlMapper mapper = new TomlMapper();
        final LicenseRCModel model = mapper.readValue(source, LicenseRCModel.class);
        final Builder builder = new Builder();
        return builder.baseDir(model.getBaseDir())
                .inlineHeader(model.getInlineHeader())
                .headerPath(model.getHeaderPath())
                .strictCheck(model.isStrictCheck())
                .useDefaultExcludes(model.isUseDefaultExcludes())
                .useDefaultMapping(model.isUseDefaultMapping())
                .includes(model.getIncludes())
                .excludes(model.getExcludes())
                .keywords(model.getKeywords())
                .properties(model.getProperties())
                .mapping(model.getMapping());
    }

    public static final class Builder {
        private Builder() {
            // always use #of methods
        }

        public HawkEyeConfig build() {
            return new HawkEyeConfig(
                    baseDir,
                    inlineHeader,
                    headerPath,
                    strictCheck,
                    useDefaultExcludes,
                    useDefaultMapping,
                    includes,
                    excludes,
                    keywords,
                    properties,
                    mapping,
                    dryRun);
        }
    }

}
