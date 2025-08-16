package org.acme.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    public final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);


    public static Long parseIsoUtcToEpochMilli(String isoUtcString) {
        if (isoUtcString == null || isoUtcString.isBlank()) return null;
        return Instant.from(FORMATTER.parse(isoUtcString)).toEpochMilli();
    }
}
