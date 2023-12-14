package io.korandoru.hawkeye.core.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class GitModel {
    @Builder.Default
    private final FeatureGate checkIgnore = FeatureGate.AUTO;
}
