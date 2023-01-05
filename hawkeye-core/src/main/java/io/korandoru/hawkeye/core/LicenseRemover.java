package io.korandoru.hawkeye.core;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;

public class LicenseRemover extends LicenseProcessor {

    public LicenseRemover(HawkEyeConfig config) {
        super(config, Report.Action.REMOVE);
    }

    @Override
    protected void onHeaderNotFound(Document document, Header header, Report report) {
        remove(document, report);
    }

    @Override
    protected void onExistingHeader(Document document, Header header, Report report) {
        remove(document, report);
    }

    private void remove(Document document, Report report) {
        if (document.headerDetected()) {
            document.removeHeader();
            document.save();
            report.add(document.getFile(), Report.Result.REMOVED);
        } else {
            report.add(document.getFile(), Report.Result.NOOP);
        }
    }

    public static void main(String[] args) throws Exception {
        final var mapper = new TomlMapper();
        final var config = mapper.readValue(HawkEyeConfig.class.getResource("/hawkeye.toml"), HawkEyeConfig.class);
        final var checker = new LicenseRemover(config);
        final var report = checker.call();
        report.getResults().forEach((path, result) -> {
            if (result != Report.Result.NOOP) {
                System.out.println(path + "=" + result);
            }
        });
    }
}
