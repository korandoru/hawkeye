package io.korandoru.hawkeye.core.config;

import io.korandoru.hawkeye.core.mapping.Mapping;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class MappingModel {

    @Builder.Default
    private final List<String> extensions = Collections.emptyList();

    @Builder.Default
    private final List<String> filenames = Collections.emptyList();

    public static Set<Mapping> toMappings(Map<String, MappingModel> models) {
        final Set<Mapping> result = new HashSet<>();
        for (String headerType: models.keySet()){
            final MappingModel model = models.get(headerType);
            for (String extension : model.getExtensions()) {
                result.add(new Mapping.Extension(extension, headerType.toLowerCase()));
            }
            for (String filename : model.getFilenames()) {
                result.add(new Mapping.Filename(filename, headerType.toLowerCase()));
            }
        }
        return result;
    }

}
