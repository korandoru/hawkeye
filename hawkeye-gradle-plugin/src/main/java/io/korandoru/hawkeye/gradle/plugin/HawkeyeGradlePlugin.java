package io.korandoru.hawkeye.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class HawkeyeGradlePlugin implements Plugin<Project> {
    public void apply(Project project) {
        // Register a task
        project.getTasks().register("hawkeye", task -> {
            task.doLast(s -> System.out.println("Hello from plugin 'io.korandoru.hawkeye'"));
        });
    }
}
