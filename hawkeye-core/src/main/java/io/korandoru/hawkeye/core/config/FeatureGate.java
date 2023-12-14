package io.korandoru.hawkeye.core.config;

public enum FeatureGate {
    /**
     * Determinate whether turn on the feature.
     */
    AUTO,

    /**
     * Force enable the feature.
     */
    ENABLE,

    /**
     * Force disable the feature.
     */
    DISABLE,
}
