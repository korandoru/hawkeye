package io.korandoru.hawkeye.gradle.plugin;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HawkeyeGradlePluginTest {
    @Test void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.korandoru.hawkeye");

        // Verify the result
        assertNotNull(project.getTasks().findByName("hawkeye"));
    }
}
