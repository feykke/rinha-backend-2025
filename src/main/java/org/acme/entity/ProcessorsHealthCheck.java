package org.acme.entity;

public record ProcessorsHealthCheck (
        boolean defaultFailing,
        int defaultMinResponseTime,
        boolean fallbackFailing,
        int fallbackMinResponseTime
) {

}
