package io.korandoru.hawkeye.core.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.korandoru.hawkeye.core.mapping.Mapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class MappingsModel {

    private final Map<String, MappingModel> models = new HashMap<>();

    public Set<Mapping> toMappings() {
        final Set<Mapping> result = new HashSet<>();
        for (String headerType: models.keySet()){
            final MappingModel model = models.get(headerType);
            for (String extension : model.getExtensions()) {
                result.add(new Mapping.Extension(extension, headerType));
            }
            for (String filename : model.getFilenames()) {
                result.add(new Mapping.Filename(filename, headerType));
            }
        }
        return result;
    }

    @JsonAnySetter
    public void populate(String name, MappingModel model) {
        models.put(name, model);
    }

}
