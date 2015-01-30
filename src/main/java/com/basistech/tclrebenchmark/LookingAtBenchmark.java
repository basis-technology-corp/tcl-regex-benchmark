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
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramReservoir;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Measure the speed of find().
 * Find is is the easy case: given a slug of text and a collection of expressions,
 * we can call find on each one add up the times. matches() and lookingAt() are trickier
 * as we might need to select many different regions of text get good coverage.
 */
public class LookingAtBenchmark {
    private final MetricRegistry metrics = new MetricRegistry();
    private final Histogram times = new Histogram(new HdrHistogramReservoir());
    private final List<RePattern> patterns;
    private final String text;
    private final int count;
    private ConsoleReporter reporter;


    public LookingAtBenchmark(List<RePattern> patterns, String text, int count) {
        this.patterns = patterns;
        this.text = text;
        this.count = count;
        metrics.register("lookingAt", times);
    }

    public void run(boolean warmup) {
        if (!warmup) {
            startReport();
        }
        for (int x = 0; x < count; x++) {
            for (RePattern pattern : patterns) {
                ReMatcher matcher = pattern.matcher(text);
                for (int start = 0; start < text.length() - 100; start += 100) {
                    int end = Math.min(text.length(), start + 100);
                    matcher.region(start, end);
                    long startTime = System.nanoTime();
                    try {
                        matcher.lookingAt();
                    } finally {
                        long time = System.nanoTime() - startTime;
                        /*
                        if (time > 8847359) {
                            System.out.println(time);
                            System.out.println("----------");
                            System.out.println(pattern.pattern());
                            System.out.println("----------");
                            System.out.println(text.substring(start, end));
                            System.out.println("----------");
                        }
                        */
                        if (!warmup) {
                            times.update(time);
                        }
                    }
                }
            }
        }
        if (!warmup) {
            stopReport();
        }
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
