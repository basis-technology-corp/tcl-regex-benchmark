/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2015 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclrebenchmark;

import com.basistech.tclre.ReMatcher;
import com.basistech.tclre.RePattern;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Measure the speed of find().
 * Find is is the easy case: given a slug of text and a collection of expressions,
 * we can call find on each one add up the times. matches() and lookingAt() are trickier
 * as we might need to select many different regions of text get good coverage.
 */
public class FindBenchmark {
    private final MetricRegistry metrics = new MetricRegistry();
    private final Timer responses = metrics.timer("find");
    private final List<RePattern> patterns;
    private final String text;
    private final int count;
    private ConsoleReporter reporter;


    public FindBenchmark(List<RePattern> patterns, String text, int count) {
        this.patterns = patterns;
        this.text = text;
        this.count = count;
    }

    public void run() {
        startReport();
        for (int x = 0; x < count; x++) {
            for (RePattern pattern : patterns) {
                ReMatcher matcher = pattern.matcher(text);
                final Timer.Context context = responses.time();
                try {
                    matcher.find();
                } finally {
                    context.stop();
                }
            }
        }
        stopReport();
    }

    private void stopReport() {
        reporter.report();
        reporter.stop();
    }

    private void startReport() {
        reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS); // generally have nothing to say until the end
    }

}
