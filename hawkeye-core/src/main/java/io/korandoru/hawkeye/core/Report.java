package io.korandoru.hawkeye.core;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Data
@RequiredArgsConstructor
public class Report {
    enum Action {CHECK, FORMAT, REMOVE}

    enum Result {
        /**
         * For check: header is OK
         */
        PRESENT,

        /**
         * For check: means the file does not contain a header
         */
        MISSING,

        /**
         * For format or remove when no operation were done
         */
        NOOP,

        /**
         * For format, when header is added
         */
        ADDED,

        /**
         * For format, when header is replaced
         */
        REPLACED,

        /**
         * For remove, when header is removed
         */
        REMOVED,

        /**
         * For any actions: means the file extension is unknown
         */
        UNKNOWN,
    }

    private final Action action;
    private final Instant timestamp = Instant.now();
    private final Map<String, Result> results = new ConcurrentHashMap<>();

    @SneakyThrows
    void add(File file, Result result) {
        results.put(file.getCanonicalPath(), result);
    }
}
