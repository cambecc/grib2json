package net.nullschool.grib2json;

import com.lexicalscope.jewel.cli.*;

import java.io.File;


/**
 * 2013-10-25<p/>
 *
 * @author Cameron Beccario
 */

@CommandLineInterface(application="grib2json")
public interface Options {

    @Option(description="display help")
    boolean isHelp();

    @Option(shortName="n", description="print names of codes")
    boolean isNames();

    @Option(shortName="d", description="print record data")
    boolean isData();

    @Option(shortName="c", description="enable compact formatting")
    boolean isCompact();

    @Option(shortName="o", description="write output to file", defaultToNull=true)
    File getOutput();

    @Unparsed(name="FILE", defaultToNull=true)
    File getFile();
}
