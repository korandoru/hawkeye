package io.korandoru.hawkeye.core;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.model.HawkEyeConfig;

public class LicenseChecker extends LicenseProcessor {

    public LicenseChecker(HawkEyeConfig config) {
        super(config, Report.Action.CHECK);
    }

    @Override
    protected void onHeaderNotFound(Document document, Header header, Report report) {
        report.add(document.getFile(), Report.Result.MISSING);
    }

    @Override
    protected void onExistingHeader(Document document, Header header, Report report) {
        report.add(document.getFile(), Report.Result.PRESENT);
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
