package io.korandoru.hawkeye.core;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.document.DocumentFactory;
import io.korandoru.hawkeye.core.document.DocumentType;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.header.HeaderType;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import io.korandoru.hawkeye.core.resource.ResourceFinder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LicenseChecker implements Callable<Report> {

    private final HawkEyeConfig config;
    public final Collection<File> missingHeaders = new ConcurrentLinkedQueue<>();

    @Override
    public Report call() {
        this.missingHeaders.clear();

        final Report report = new Report(Report.Action.CHECK);
        final ResourceFinder finder = new ResourceFinder(config.getBaseDir());
        finder.setPluginClassPath(getClass().getClassLoader());

        final HeaderSource headerSource = HeaderSource.of(config.getInlineHeader(), config.getHeaderPath(), finder);
        final Header header = new Header(headerSource);
        final Selection selection = new Selection(
                config.getBaseDir().toFile(),
                config.getIncludes().toArray(new String[0]),
                config.getExcludes().toArray(new String[0]),
                config.isUseDefaultExcludes());
        final String[] selectedFiles = selection.getSelectedFiles();

        final DocumentFactory documentFactory = new DocumentFactory(
                config.getBaseDir().toFile(),
                DocumentType.defaultMapping(),
                HeaderType.defaultDefinitions(),
                StandardCharsets.UTF_8,
                config.getKeywords().toArray(new String[0]),
                d -> {
                    final Map<String, String> perDoc = new LinkedHashMap<>(config.getProperties());
                    perDoc.put("file.name", d.getFile().getName());
                    return perDoc;
                });

        for (final String file : selectedFiles) {
            final Document document = documentFactory.createDocuments(file);
            if (document.is(header)) {
                continue;
            }
            if (document.isNotSupported()) {
                report.add(document.getFile(), Report.Result.UNKNOWN);
            } else if (document.hasHeader(header, config.isStrictCheck())) {
                report.add(document.getFile(), Report.Result.PRESENT);
            } else {
                report.add(document.getFile(), Report.Result.MISSING);
                missingHeaders.add(document.getFile());
            }
        }

        return report;
    }

    public static void main(String[] args) throws Exception {
        final var mapper = new TomlMapper();
        final var config = mapper.readValue(HawkEyeConfig.class.getResource("/hawkeye.toml"), HawkEyeConfig.class);
        final var checker = new LicenseChecker(config);
        final var report = checker.call();
        report.getResults().forEach((path, result) -> {
            if (result != Report.Result.PRESENT) {
                System.out.println(path + "=" + result);
            }
        });
    }
}
