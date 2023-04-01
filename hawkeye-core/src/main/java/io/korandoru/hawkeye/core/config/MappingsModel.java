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
        for (String headerType: models.keySet()){
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
