package net.nullschool.grib2json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ucar.ma2.*;
import ucar.nc2.Variable;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import java.io.IOException;

import static ucar.grib.grib2.Grib2Tables.*;
import static ucar.grib.grib2.ParameterTable.getCategoryName;
import static ucar.grib.grib2.ParameterTable.getParameterName;
import static ucar.grib.grib2.ParameterTable.getParameterUnit;
import static ucar.grib.grib2.ParameterTable.getDisciplineName;


/**
 * 2014-01-15<p/>
 *
 * Writes OSCAR (Ocean Surface Current Analyses Real-time) data to a JSON generator.
 *
 * Lots of magic values in here, as we're mapping OSCAR data in NetCDF format to JSON using GRIB header constants.
 * The input is assumed to be in the range [20E:80N, 380E:80S], with 1/3º resolution. To reduce download size, the
 * precision of the data is reduced to a resolution of 2cm/s.
 *
 * This is not a generic processing of NetCDF records.
 *
 * For more information on OSCAR, see http://www.esr.org/oscar_index.html.
 *
 * @author Cameron Beccario
 */
final class OscarRecordWriter extends AbstractRecordWriter {

    private static final int OCEAN_PRODUCTS = 10;
    private static final int NX = 1080;  // Number of points on x-axis or parallel
    private static final int NY = 481;   // Number of points on y-axis or meridian
    private static final String RANGE = "0,0,0:480,0:1079";


    private final Variable var;
    private final DateTime date;
    private final double depth;

    OscarRecordWriter(JsonGenerator jg, Variable var, DateTime date, double depth, Options options) {
        super(jg, options);
        this.var = var;
        this.date = date;
        this.depth = depth;
    }

    private void writeIndicator() {
        write("discipline", OCEAN_PRODUCTS, getDisciplineName(OCEAN_PRODUCTS));
    }

    private void writeIdentification() {
        write("center", -3, "Earth & Space Research");
        write("refTime", date.withZone(DateTimeZone.UTC).toString());
        write("significanceOfRT", 0, "Analysis");
    }

    private static int variableToParam(Variable var) {
        switch (var.getFullName()) {
            case "u": return 2;  // U component of current
            case "v": return 3;  // V component of current
            default:
                throw new IllegalArgumentException("unknown variable: " + var);
        }
    }

    private void writeProduct() {
        final int paramCategory = 1;  // Currents
        final int paramNumber = variableToParam(var);
        final int surfaceType = 160;  // Depth below sea level

        write("parameterCategory", 1, getCategoryName(OCEAN_PRODUCTS, paramCategory));
        write("parameterNumber", paramNumber, getParameterName(OCEAN_PRODUCTS, paramCategory, paramNumber));
        write("parameterUnit", getParameterUnit(OCEAN_PRODUCTS, paramCategory, paramNumber));
        write("forecastTime", 0);
        write("surface1Type", surfaceType, codeTable4_5(surfaceType));
        write("surface1Value", depth);
    }

    private void writeGridShape() {
        write("shape", 0, codeTable3_2(0));
    }

    private void writeGridSize() {
        write("scanMode", 0);
        write("nx", NX);  // Number of points on x-axis or parallel
        write("ny", NY);  // Number of points on y-axis or meridian
    }

    private void writeLonLatBounds() {
        write("lo1", 20);  // longitude of first grid point
        write("la1", 80);  // latitude of first grid point
        write("lo2", (20 + 359) + 2/3d);  // longitude of last grid point
        write("la2", -80);  // latitude of last grid point
        write("dx", 1/3d);    // i direction increment
        write("dy", 1/3d);    // j direction increment
    }

    private void writeGridDefinition() {
        write("numberPoints", NX * NY);
        writeGridShape();
        writeGridSize();
        writeLonLatBounds();
    }

    /**
     * Write the record's header as a Json object: "header": { ... }
     */
    private void writeHeader() {
        jg.writeStartObject("header");
        writeIndicator();
        writeIdentification();
        writeProduct();
        writeGridDefinition();
        jg.writeEnd();
    }

    /**
     * Round to nearest fraction of 1/denominator.
     */
    private float round(float value, float denominator) {
        return Math.round(value * denominator) / denominator;
    }

    /**
     * Write the record's data as a Json array: "data": [ ... ]
     */
    private void writeData() throws IOException {
        if (!options.getPrintData()) {
            return;
        }
        try {
            jg.writeStartArray("data");
            Array data = var.read(RANGE).reduce();
            IndexIterator ii = data.getIndexIterator();
            while (ii.hasNext()) {
                float value = ii.getFloatNext();
                jg.write(Float.isNaN(value) ? JsonValue.NULL : new FloatValue(round(value, 50)));
            }
            jg.writeEnd();
        }
        catch (InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the record as a Json object: { "header": { ... }, "data": [ ... ] }
     */
    void writeRecord() throws IOException {
        jg.writeStartObject();
        writeHeader();
        writeData();
        jg.writeEnd();
    }
}
