package io.korandoru.hawkeye.core.config;

import java.util.Collections;
import java.util.List;
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

}
