package net.nullschool.grib2json;

import com.lexicalscope.jewel.cli.*;

import java.io.File;


/**
 * 2013-10-25<p/>
 *
 * @author Cameron Beccario
 */

@CommandLineInterface(application="grib2json")
interface Options {

    @Option(shortName="n", description="print names of codes")
    boolean isNames();

    @Option(shortName="d", description="print record data")
    boolean isData();

    @Option(description="display help")
    boolean isHelp();

    @Option(shortName="o", description="write output to file", defaultToNull=true)
    File getOutput();

    @Unparsed(name="FILE")
    File getFile();
}
