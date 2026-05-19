package com.vimainsurance.vimaadmin.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * F-28: Applies {@link LogSensitiveDataMasker} to formatted log lines (console, CloudWatch, files).
 */
public class MaskingPatternLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return LogSensitiveDataMasker.maskMessage(super.doLayout(event));
    }
}
