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

import com.basistech.tclre.HsrePattern;
import com.basistech.tclre.PatternFlags;
import com.basistech.tclre.RePattern;
import com.basistech.tclre.RegexException;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * CLI to run the benchmark
 */
public class BenchmarkDriver {

    enum Task {
        find,
        lookingAt,
        compile
    }

    @Argument(required = true)
    Task task;

    @Argument(index = 1, required = true)
    File text;

    @Argument(index = 2, required = true)
    List<File> regexes;

    String textContent;
    List<RePattern> patterns;

    public static void main(String[] args) {
        new BenchmarkDriver().doMain(args);
    }

    private void doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("BenchmarkDriver TASK textFile patternFile1 ... patternFileN");
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        }

        try {
            setupInputs();
        } catch (IOException e) {
            System.err.println("Error reading text or regexes");
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        } catch (RegexException e) {
            System.err.println("Error parsing regex");
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        // for now we only have one task.
        switch (task) {
        case find:
            doFind();
            break;
        case lookingAt:
            doLookingAt();
            break;
        case compile:
            doCompile();
            break;
        default:
            break;
        }
    }

    private void doCompile() {
        new CompileBenchmark(patterns, textContent, 1000).run();
    }

    private void setupInputs() throws IOException, RegexException {
        textContent = Files.toString(text, Charsets.UTF_8);
        patterns = Lists.newArrayList();
        for (File regexFile : regexes) {
            List<String> regexLines = Files.readLines(regexFile, Charsets.UTF_8);
            // each one is three fields separated by tabs, but with no escaping on the last field
            int lineCount = 1;
            for (String line : regexLines) {
                int tx = line.indexOf('\t');
                tx = line.indexOf('\t', tx + 1);
                String regex = line.substring(tx + 1);
                try {
                    patterns.add(HsrePattern.compile(regex, PatternFlags.ADVANCED));
                    lineCount++;
                } catch (RegexException e) {
                    System.err.printf("Error parsing line %d of file %s: %s%n", lineCount, regexFile.getAbsolutePath(), regex);
                    throw e;
                }
            }
        }
    }

    private void doFind() {
        new FindBenchmark(patterns, textContent, 10).run();
    }

    private void doLookingAt() {
        LookingAtBenchmark lookingAtBenchmark = new LookingAtBenchmark(patterns, textContent, 10);
        lookingAtBenchmark.run(true);
        lookingAtBenchmark.run(false);
    }
}
