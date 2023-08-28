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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.korandoru.hawkeye.core.mapping.Mapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MappingsModel {

    private final Map<String, MappingModel> models = new HashMap<>();

    public Set<Mapping> toMappings() {
        final Set<Mapping> result = new HashSet<>();
        for (String headerType : models.keySet()) {
            final MappingModel model = models.get(headerType);
            final String lowerType = headerType.toLowerCase();
            for (String extension : model.getExtensions()) {
                result.add(new Mapping.Extension(extension, lowerType));
            }
            for (String filename : model.getFilenames()) {
                result.add(new Mapping.Filename(filename, lowerType));
            }
        }
        return result;
    }

    @JsonAnySetter
    public void populate(String name, MappingModel model) {
        models.put(name, model);
    }
}
