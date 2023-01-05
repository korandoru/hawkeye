package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.document.DocumentFactory;
import io.korandoru.hawkeye.core.document.DocumentType;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.header.HeaderType;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import io.korandoru.hawkeye.core.resource.ResourceFinder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class LicenseProcessor implements Callable<Report> {

    private final HawkEyeConfig config;
    private final Report.Action action;

    @Override
    public Report call() {
        final Report report = new Report(action);
        final ResourceFinder resourceFinder = new ResourceFinder(config.getBaseDir());
        resourceFinder.setPluginClassPath(getClass().getClassLoader());

        final HeaderSource headerSource = HeaderSource.of(
                config.getInlineHeader(),
                config.getHeaderPath(),
                resourceFinder);
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
                onExistingHeader(document, header, report);
            } else {
                onHeaderNotFound(document, header, report);
            }
        }

        return report;
    }

    protected abstract void onHeaderNotFound(Document document, Header header, Report report);

    protected abstract void onExistingHeader(Document document, Header header, Report report);

}
