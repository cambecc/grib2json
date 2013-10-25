package net.nullschool.grib2json;

import com.lexicalscope.jewel.cli.*;

import java.io.File;


/**
 * 2013-10-25<p/>
 *
 * Command line options for the grib2json utility. This interface is proxied by the Jewel Cli options parsing library.
 *
 * @author Cameron Beccario
 */
@CommandLineInterface(application="grib2json")
public interface Options {

    @Option(longName="help", shortName={"h", "?"}, description="display help")
    boolean getShowHelp();

    @Option(longName="names", shortName="n", description="print names of the numeric codes")
    boolean getPrintNames();

    @Option(longName="data", shortName="d", description="print record data")
    boolean getPrintData();

    @Option(longName="compact", shortName="c", description="enable compact formatting")
    boolean isCompactFormat();

    @Option(longName="verbose", shortName="v", description="enable logging")
    boolean getEnableLogging();

    @Option(longName="output", shortName="o", description="print to specified file", defaultToNull=true)
    File getOutput();

    @Unparsed(name="FILE", defaultToNull=true)
    File getFile();

    // ============================
    // options to perform filtering

    @Option(
        longName={"filter.category", "fc"},
        description="select records with this numeric category",
        defaultToNull=true)
    Integer getFilterCategory();

    @Option(
        longName={"filter.parameter", "fp"},
        description="select records with this numeric parameter",
        defaultToNull=true)
    Integer getFilterParameter();

    @Option(
        longName={"filter.surface", "fs"},
        description="select records with this surface type",
        defaultToNull=true)
    Integer getFilterSurface();

    @Option(
        longName={"filter.value", "fv"},
        description="select records with this surface value",
        defaultToNull=true)
    Double getFilterValue();
}
