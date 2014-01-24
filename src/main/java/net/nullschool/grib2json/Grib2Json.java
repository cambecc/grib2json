package net.nullschool.grib2json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.grib.grib2.*;
import ucar.nc2.NetcdfFile;
import ucar.unidata.io.RandomAccessFile;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.*;
import java.util.*;

import static java.util.Collections.*;

/**
 * 2013-10-25<p/>
 *
 * Converts a GRIB2 file to Json. GRIB2 decoding is performed by the netCDF-Java GRIB decoder.
 *
 * This class was initially based on Grib2Dump, part of the netCDF-Java library written by University
 * Corporation for Atmospheric Research/Unidata. However, what appears below is a complete rewrite.
 *
 * @author Cameron Beccario
 */
public final class Grib2Json {

    private static final Logger log = LoggerFactory.getLogger(Grib2Json.class);


    private final File file;
    private final List<Options> optionGroups;

    public Grib2Json(File file, List<Options> optionGroups) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Cannot find input file: " + file);
        }
        this.file = file;
        this.optionGroups = optionGroups;
    }

    private JsonGenerator newJsonGenerator(Options options) throws IOException {
        JsonGeneratorFactory jgf =
            Json.createGeneratorFactory(
                options.isCompactFormat() ?
                    null :
                    singletonMap(JsonGenerator.PRETTY_PRINTING, true));

        OutputStream output = options.getOutput() != null ?
            new BufferedOutputStream(new FileOutputStream(options.getOutput(), false)) :
            System.out;

        return jgf.createGenerator(output);
    }

    private void write(RandomAccessFile raf, Grib2Input input, Options options) throws IOException {
        JsonGenerator jg = newJsonGenerator(options);
        jg.writeStartArray();

        List<Grib2Record> records = input.getRecords();
        for (Grib2Record record : records) {
            GribRecordWriter rw = new GribRecordWriter(jg, record, options);
            if (rw.isSelected()) {
                jg.writeStartObject();
                rw.writeHeader();
                if (options.getPrintData()) {
                    rw.writeData(new Grib2Data(raf));
                }
                jg.writeEnd();
            }
        }

        jg.writeEnd();
        jg.close();
    }

    private void write(NetcdfFile netcdfFile, Options options) throws IOException {
        JsonGenerator jg = newJsonGenerator(options);
        jg.writeStartArray();

        int days = netcdfFile.findVariable("time").readScalarInt();
        DateTime date = new DateTime(1992, 10, 5, 0, 0, DateTimeZone.UTC).plusDays(days);
        double depth = netcdfFile.findVariable("depth").readScalarDouble();

        new OscarRecordWriter(jg, netcdfFile.findVariable("u"), date, depth, options).writeRecord();
        new OscarRecordWriter(jg, netcdfFile.findVariable("v"), date, depth, options).writeRecord();

        jg.writeEnd();
        jg.close();
    }

    /**
     * Convert the input file to Json as specified by the command line options.
     */
    public void write() throws IOException {

        // Try opening the file as GRIB format.
        RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
        raf.order(RandomAccessFile.BIG_ENDIAN);
        Grib2Input input = new Grib2Input(raf);
        if (input.scan(false, false)) {
            for (Options options : optionGroups) {
                write(raf, input, options);
            }
            raf.close();
        }
        else {
            raf.close();

            // Otherwise, process it as NetCDF format.
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            log.info("File contents:\n{}", netcdfFile);
            for (Options options : optionGroups) {
                write(netcdfFile, options);
            }
        }
    }
}
