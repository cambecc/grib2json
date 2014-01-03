package net.nullschool.grib2json;

import ucar.grib.grib2.*;
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

    private final File gribFile;
    private final List<Options> optionGroups;

    public Grib2Json(File gribFile, List<Options> optionGroups) {
        if (!gribFile.exists()) {
            throw new IllegalArgumentException("Cannot find input file: " + gribFile);
        }
        this.gribFile = gribFile;
        this.optionGroups = optionGroups;
    }

    private void write(RandomAccessFile raf, Grib2Input input, Options options) throws IOException {
        JsonGeneratorFactory jgf =
            Json.createGeneratorFactory(
                options.isCompactFormat() ?
                    null :
                    singletonMap(JsonGenerator.PRETTY_PRINTING, true));

        OutputStream output = options.getOutput() != null ?
            new BufferedOutputStream(new FileOutputStream(options.getOutput(), false)) :
            System.out;

        JsonGenerator jg = jgf.createGenerator(output);

        jg.writeStartArray();

        List<Grib2Record> records = input.getRecords();
        for (Grib2Record record : records) {
            RecordWriter rw = new RecordWriter(jg, record, options);
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

    /**
     * Convert the GRIB2 file to Json as specified by the command line options.
     */
    public void write() throws IOException {

        RandomAccessFile raf = new RandomAccessFile(gribFile.getPath(), "r");
        raf.order(RandomAccessFile.BIG_ENDIAN);
        Grib2Input input = new Grib2Input(raf);
        if (!input.scan(false, false)) {
            throw new IllegalArgumentException("Failed to successfully scan grib file.");
        }

        for (Options options : optionGroups) {
            write(raf, input, options);
        }

        raf.close();
    }
}
