/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.nullschool.grib2json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ucar.grib.GribNumbers;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib2.*;

import java.io.*;
import java.util.*;

import ucar.unidata.io.RandomAccessFile;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;


/**
 * 2013-10-24<p/>
 *
 * @author Cameron Beccario
 */
public class Driver {

    /**
     * Dumps usage of the class.
     *
     * @param className Grib2Dump
     */
    private static void usage(String className) {
        System.out.println();
        System.out.println("Usage of " + className + ":");
        System.out.println("Parameters:");
        System.out.println("<GribFileToRead> reads/scans metadata");
        System.out.println("<output file> file to store results");
        System.out.println(
            "<true or false> whether to read/display data too");
        System.out.println();
        System.out.println(
            "java " + className
                + " <GribFileToRead> <output file> <true or false>");
        System.exit(0);
    }

    PrintStream printString(String s, PrintStream ps) {
        return s == null ? ps.append("null") : ps.append('"').append(s).append('"');
    }

    PrintStream printKey(String s, PrintStream ps) {
        return printString(s, ps).append(':');
    }

    PrintStream printNumber(Number n, PrintStream ps) {
        return n == null ? ps.append("null") : ps.append(n.toString());
    }

    /**
     * Dump of content of the Grib2 file to text.
     *
     * @param args Gribfile,  [output file], [true|false] output data.
     */
    public void gribDump(String args[]) {
        boolean displayData = false;

        // Reading of Grib files must be inside a try-catch block
        try {
            RandomAccessFile raf = null;
            PrintStream ps = System.out;
            if (args.length == 3) {  // input file, output file, get data for dump
                raf = new RandomAccessFile(args[0], "r");
                ps = new PrintStream(
                    new BufferedOutputStream(
                        new FileOutputStream(args[1], false)));
                displayData = args[2].equalsIgnoreCase("true");
            }
            else if (args.length == 2) {  // input file and output file for dump
                raf = new RandomAccessFile(args[0], "r");
                if (args[1].equalsIgnoreCase("true")
                    || args[1].equalsIgnoreCase("false")) {
                    displayData = args[1].equalsIgnoreCase("true");
                }
                else {
                    ps = new PrintStream(
                        new BufferedOutputStream(
                            new FileOutputStream(args[1], false)));
                }
            }
            else if (args.length == 1) {
                raf = new RandomAccessFile(args[0], "r");
            }
            else {
                System.exit(0);
            }
            raf.order(RandomAccessFile.BIG_ENDIAN);
            // Create Grib2Input instance
            Grib2Input g2i = new Grib2Input(raf);
            // boolean params getProductsOnly, oneRecord
            g2i.scan(false, false);


            Map<String, Object> properties = new HashMap<>();
            properties.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
            JsonGenerator jg = jgf.createGenerator(ps);

            jg.writeStartArray();
            boolean first = true;

            // record contains objects for all 8 Grib2 sections
            List<Grib2Record> records = g2i.getRecords();
            for (Grib2Record record : records) {

                jg.writeStartObject();
                jg.write("Header", record.getHeader());

                Grib2IndicatorSection is = record.getIs();
                Grib2IdentificationSection id = record.getId();
                Grib2GridDefinitionSection gds = record.getGDS();
                Grib2Pds pds = record.getPDS().getPdsVars();

                printIS(is, jg);
                printID(id, jg);
                printGDS(gds, jg);
                printPDS(is, pds, jg);

                if (displayData && first) {

                    float[] data;
                    Grib2Data gd = new Grib2Data(raf);
                    data = gd.getData(record.getGdsOffset(), record.getPdsOffset(), id.getRefTime());
                    if (data != null) {
                        jg.writeStartArray("Data");
                        for (float value : data) {
                            jg.write(new FloatValue(value));
                        }
                        jg.writeEnd();
                    }
                }

                jg.writeEnd();
                first = false;
            }

            jg.writeEnd();
            jg.close();

            raf.close();
        }
        catch (FileNotFoundException noFileError) {
            System.err.println("FileNotFoundException : " + noFileError);
        }
        catch (IOException ioError) {
            System.err.println("IOException : " + ioError);
        }
    }

    private void writeCompound(JsonGenerator jg, String name, int code, String description) {
        jg.writeStartObject(name).write(Integer.toString(code), description).writeEnd();
    }

    private void printIS(Grib2IndicatorSection is, JsonGenerator jg) {
        writeCompound(jg, "Discipline", is.getDiscipline(), is.getDisciplineName());
        jg.write("GRIB Edition", is.getGribEdition());
        jg.write("GRIB length", is.getGribLength());
    }

    private void printID(Grib2IdentificationSection id, JsonGenerator jg) {
        writeCompound(jg, "Originating Center", id.getCenter_id(), Grib1Tables.getCenter_idName(id.getCenter_id()));
        jg.write("Originating Sub-Center", id.getSubcenter_id());
        writeCompound(jg, "Significance of Reference Time", id.getSignificanceOfRT(), id.getSignificanceOfRTName());
        jg.write("Reference Time", new DateTime(id.getBaseTime()).withZone(DateTimeZone.UTC).toString());
        writeCompound(jg, "Product Status", id.getProductStatus(), id.getProductStatusName());
        writeCompound(jg, "Product Type", id.getProductType(), id.getProductTypeName());
    }

    private void printGDS(Grib2GridDefinitionSection gds, JsonGenerator jg) {

        Grib2GDSVariables gdsv = gds.getGdsVars();

        jg.write("Number of data points", gdsv.getNumberPoints());
        writeCompound(jg, "Grid Name", gdsv.getGdtn(), Grib2Tables.codeTable3_1(gdsv.getGdtn()));

        String winds = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
            ? "Relative"
            : "True";

        switch (gdsv.getGdtn()) {  // Grid Definition Template Number

            case 0:
            case 1:
            case 2:
            case 3:                // Latitude/Longitude Grid

                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Basic angle", gdsv.getBasicAngle());
                jg.write("Subdivisions of basic angle", gdsv.getSubDivisions());
                jg.write("Latitude of first grid point", gdsv.getLa1());
                jg.write("Longitude of first grid point", gdsv.getLo1());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Latitude of last grid point", gdsv.getLa2());
                jg.write("Longitude of last grid point", gdsv.getLo2());
                jg.write("i direction increment", gdsv.getDx());
                jg.write("j direction increment", gdsv.getDy());
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Scanning mode", gdsv.getScanMode());

                if (gdsv.getGdtn() == 1) {  // Rotated Latitude/longitude
                    jg.write("Latitude of southern pole", gdsv.getSpLat());
                    jg.write("Longitude of southern pole", gdsv.getSpLon());
                    jg.write("Rotation angle", gdsv.getRotationAngle());
                }
                else if (gdsv.getGdtn() == 2) {  // Stretched Latitude/longitude
                    jg.write("Latitude of pole", gdsv.getPoleLat());
                    jg.write("Longitude of pole", gdsv.getPoleLon());
                    jg.write("Stretching factor", gdsv.getStretchingFactor());
                }
                else if (gdsv.getGdtn() == 3) {  // Stretched and Rotated
                    // Latitude/longitude
                    jg.write("Latitude of southern pole", gdsv.getSpLat());
                    jg.write("Longitude of southern pole", gdsv.getSpLon());
                    jg.write("Rotation angle", gdsv.getRotationAngle());
                    jg.write("Latitude of pole", gdsv.getPoleLat());
                    jg.write("Longitude of pole", gdsv.getPoleLon());
                    jg.write("Stretching factor", gdsv.getStretchingFactor());
                }
                break;

            case 10:  // Mercator
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Latitude of first grid point", gdsv.getLa1());
                jg.write("Longitude of first grid point", gdsv.getLo1());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Latitude of last grid point", gdsv.getLa2());
                jg.write("Longitude of last grid point", gdsv.getLo2());
                jg.write("Scanning mode", gdsv.getScanMode());
                jg.write("Basic angle", gdsv.getAngle());
                jg.write("i direction increment", gdsv.getDx());
                jg.write("j direction increment", gdsv.getDy());
                jg.write("Grid Units", gdsv.getGridUnits());
                break;

            case 20:  // Polar stereographic projection
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Latitude of first grid point", gdsv.getLa1());
                jg.write("Longitude of first grid point", gdsv.getLo1());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Scanning mode", gdsv.getScanMode());
                break;

            case 30:  // Lambert Conformal
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Nx", gdsv.getNx());
                jg.write("Ny", gdsv.getNy());
                jg.write("La1", gdsv.getLa1());
                jg.write("Lo1", gdsv.getLo1());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("LaD", gdsv.getLaD());
                jg.write("LoV", gdsv.getLoV());
                jg.write("Dx", gdsv.getDx());
                jg.write("Dy", gdsv.getDy());
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Projection center", gdsv.getProjectionFlag());
                jg.write("Scanning mode", gdsv.getScanMode());
                jg.write("Latin1", gdsv.getLatin1());
                jg.write("Latin2", gdsv.getLatin2());
                jg.write("SpLat", gdsv.getSpLat());
                jg.write("SpLon", gdsv.getSpLon());
                break;

            case 40:
            case 41:
            case 42:
            case 43:  // Gaussian latitude/longitude
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Basic angle", gdsv.getAngle());
                jg.write("Subdivisions of basic angle", gdsv.getSubDivisions());
                jg.write("Latitude of first grid point", gdsv.getLa1());
                jg.write("Longitude of first grid point", gdsv.getLo1());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Latitude of last grid point", gdsv.getLa2());
                jg.write("Longitude of last grid point", gdsv.getLo2());
                jg.write("i direction increment", gdsv.getDx());
                jg.write("Stretching factor", gdsv.getStretchingFactor());
                jg.write("Number of parallels", gdsv.getNp());
                jg.write("Scanning mode", gdsv.getScanMode());

                if (gdsv.getGdtn() == 41) {  //Rotated Gaussian Latitude/longitude
                    jg.write("Latitude of southern pole", gdsv.getSpLat());
                    jg.write("Longitude of southern pole", gdsv.getSpLon());
                    jg.write("Rotation angle", gdsv.getRotationAngle());
                }
                else if (gdsv.getGdtn() == 42) {  //Stretched Gaussian
                    // Latitude/longitude
                    jg.write("Latitude of pole", gdsv.getPoleLat());
                    jg.write("Longitude of pole", gdsv.getPoleLon());
                    jg.write("Stretching factor", gdsv.getStretchingFactor());
                }
                else if (gdsv.getGdtn() == 43) {  //Stretched and Rotated Gaussian
                    // Latitude/longitude
                    jg.write("Latitude of southern pole", gdsv.getSpLat());
                    jg.write("Longitude of southern pole", gdsv.getSpLon());
                    jg.write("Rotation angle", gdsv.getRotationAngle());
                    jg.write("Latitude of pole", gdsv.getPoleLat());
                    jg.write("Longitude of pole", gdsv.getPoleLon());
                    jg.write("Stretching factor", gdsv.getStretchingFactor());
                }
                break;

            case 90:  // Space view perspective or orthographic
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Latitude of sub-satellite point", gdsv.getLap());
                jg.write("Longitude of sub-satellite pt", gdsv.getLop());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Dx i direction increment", gdsv.getDx());
                jg.write("Dy j direction increment", gdsv.getDy());
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Xp-coordinate of sub-satellite", gdsv.getXp());
                jg.write("Yp-coordinate of sub-satellite", gdsv.getYp());
                jg.write("Scanning mode", gdsv.getScanMode());
                jg.write("Basic angle", gdsv.getAngle());
                jg.write("Nr Altitude of the camera", gdsv.getNr());
                jg.write("Xo-coordinate of origin", gdsv.getXo());
                jg.write("Yo-coordinate of origin", gdsv.getYo());
                break;

            case 204:  // Curvilinear orthographic grib
                writeCompound(jg, "Grid Shape", gdsv.getShape(), Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    jg.write("Spherical earth radius", gdsv.getEarthRadius());
                }
                else if (gdsv.getShape() == 3) {
                    jg.write("Oblate earth major axis", gdsv.getMajorAxis());
                    jg.write("Oblate earth minor axis", gdsv.getMinorAxis());
                }
                jg.write("Number of points along parallel", gdsv.getNx());
                jg.write("Number of points along meridian", gdsv.getNy());
                jg.write("Resolution & Component flags", gdsv.getResolution());
                jg.write("Winds", winds);
                jg.write("Grid Units", gdsv.getGridUnits());
                jg.write("Scanning mode", gdsv.getScanMode());
                break;

            default:
                jg.write("Unknown Grid Type", gdsv.getGdtn());
        }
    }

    private void printPDS(Grib2IndicatorSection is, Grib2Pds pdsv, JsonGenerator jg) {

        int productDefinition = pdsv.getProductDefinitionTemplate();

        writeCompound(jg, "Product Definition", productDefinition, Grib2Tables.codeTable4_0(productDefinition));
        writeCompound(jg, "Parameter Category", pdsv.getParameterCategory(), ParameterTable.getCategoryName(is.getDiscipline(), pdsv.getParameterCategory()));
        writeCompound(jg, "Parameter Name", pdsv.getParameterNumber(), ParameterTable.getParameterName(is.getDiscipline(), pdsv.getParameterCategory(), pdsv.getParameterNumber()));
        jg.write("Parameter Units", ParameterTable.getParameterUnit(is.getDiscipline(), pdsv.getParameterCategory(), pdsv.getParameterNumber()));
        int tgp = pdsv.getGenProcessType();
        writeCompound(jg, "Generating Process Type", tgp, Grib2Tables.codeTable4_3(tgp));
        jg.write("ForecastTime", pdsv.getForecastTime());
        writeCompound(jg, "First Surface Type", pdsv.getLevelType1(), Grib2Tables.codeTable4_5(pdsv.getLevelType1()));
        jg.write("First Surface value", pdsv.getLevelValue1());
        writeCompound(jg, "Second Surface Type", pdsv.getLevelType2(), Grib2Tables.codeTable4_5(pdsv.getLevelType2()));
        jg.write("Second Surface value", pdsv.getLevelValue2());
    }

    /**
     * Dump of content of the Grib2 file to text.
     *
     * @param args Gribfile,  [output file], [true|false] output data.
     */
    public static void main(String args[]) {

        // Function References
        Driver g2d = new Driver();

        // Test usage
        if (args.length < 1) {
            // Get class name as String
            Class cl = g2d.getClass();
            Driver.usage(cl.getName());
            return;
        }
        g2d.gribDump(args);
    }
}
