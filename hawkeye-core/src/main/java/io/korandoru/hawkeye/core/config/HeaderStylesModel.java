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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.korandoru.hawkeye.core.header.HeaderDefinition;
import java.util.HashMap;
import java.util.Map;

public class HeaderStylesModel {
    private final Map<String, HeaderStyleModel> models = new HashMap<>();

    public Map<String, HeaderDefinition> toHeaderDefinitions() {
        final Map<String, HeaderDefinition> result = new HashMap<>();
        for (String name: models.keySet()){
            final HeaderStyleModel model = models.get(name);
            result.put(name, model.toHeaderDefinition(name));
        }
        return result;
    }

    @JsonAnySetter
    public void populate(String name, HeaderStyleModel model) {
        models.put(name, model);
    }
}
