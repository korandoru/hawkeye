package io.korandoru.hawkeye.core.model;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
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
public class HawkEyeConfig {

    private final Path baseDir;
    private final String inlineHeader;
    private final String headerPath;
    private final Map<String, String> properties;

    @Builder.Default
    private final boolean useDefaultExcludes = true;
    @Builder.Default
    private final List<String> includes = Collections.emptyList();
    @Builder.Default
    private final List<String> excludes = Collections.emptyList();

    @Builder.Default
    private final boolean strictCheck = true;


    public static void main(String[] args) throws Exception {
        final var mapper = new TomlMapper();
        final var config = mapper.readValue(HawkEyeConfig.class.getResource("/hawkeye.toml"), HawkEyeConfig.class);
        System.out.println(config);
    }

}
