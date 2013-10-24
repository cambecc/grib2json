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
import java.math.BigDecimal;
import java.math.BigInteger;
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
//                printGDS(gds, jg);
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

    private void printIS(Grib2IndicatorSection is, JsonGenerator jg) {
        jg.writeStartObject("Discipline")
            .write(Integer.toString(is.getDiscipline()), is.getDisciplineName())
            .writeEnd();
        jg.write("GRIB Edition", is.getGribEdition());
        jg.write("GRIB length", is.getGribLength());
    }

    private void printID(
        Grib2IdentificationSection id,
        JsonGenerator jg) {

        jg.writeStartObject("Originating Center")
            .write(Integer.toString(id.getCenter_id()), Grib1Tables.getCenter_idName(id.getCenter_id()))
            .writeEnd();
        jg.write("Originating Sub-Center", id.getSubcenter_id());
        jg.writeStartObject("Significance of Reference Time")
            .write(Integer.toString(id.getSignificanceOfRT()), id.getSignificanceOfRTName())
            .writeEnd();
        jg.write("Reference Time", new DateTime(id.getBaseTime()).withZone(DateTimeZone.UTC).toString());
        jg.writeStartObject("Product Status")
            .write(Integer.toString(id.getProductStatus()), id.getProductStatusName())
            .writeEnd();
        jg.writeStartObject("Product Type")
            .write(Integer.toString(id.getProductType()), id.getProductTypeName())
            .writeEnd();
    }

    /**
     * Prints a GDS
     *
     * @param gds Grib2GridDefinitionSection
     * @param ps PrintStream
     */
    private void printGDS(
        Grib2GridDefinitionSection gds,
        PrintStream ps) {

        Grib2GDSVariables gdsv = gds.getGdsVars();

        ps.println(
            "         Number of data points : "
                + gdsv.getNumberPoints());
        ps.println(
            "                     Grid Name : " + gdsv.getGdtn() + " "
                + Grib2Tables.codeTable3_1(gdsv.getGdtn()));

        String winds = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
            ? "Relative"
            : "True";

        switch (gdsv.getGdtn()) {  // Grid Definition Template Number

            case 0:
            case 1:
            case 2:
            case 3:                // Latitude/Longitude Grid

                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "         Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "         Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println(
                    "                   Basic angle : "
                        + gdsv.getBasicAngle());
                ps.println(
                    "    Subdivisions of basic angle: "
                        + gdsv.getSubDivisions());
                ps.println("  Latitude of first grid point : " + gdsv.getLa1());
                ps.println(" Longitude of first grid point : " + gdsv.getLo1());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("   Latitude of last grid point : " + gdsv.getLa2());
                ps.println("  Longitude of last grid point : " + gdsv.getLo2());
                ps.println("         i direction increment : " + gdsv.getDx());
                ps.println("         j direction increment : " + gdsv.getDy());
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());

                if (gdsv.getGdtn() == 1) {  //Rotated Latitude/longitude
                    ps.println(
                        "     Latitude of southern pole : "
                            + gdsv.getSpLat());
                    ps.println(
                        "    Longitude of southern pole : "
                            + gdsv.getSpLon());
                    ps.println(
                        "                Rotation angle : "
                            + gdsv.getRotationAngle());

                }
                else if (gdsv.getGdtn() == 2) {  //Stretched Latitude/longitude
                    ps.println(
                        "              Latitude of pole : "
                            + gdsv.getPoleLat());
                    ps.println(
                        "             Longitude of pole : "
                            + gdsv.getPoleLon());
                    ps.println(
                        "             Stretching factor : "
                            + gdsv.getStretchingFactor());

                }
                else if (gdsv.getGdtn() == 3) {  //Stretched and Rotated
                    // Latitude/longitude
                    ps.println(
                        "     Latitude of southern pole : "
                            + gdsv.getSpLat());
                    ps.println(
                        "    Longitude of southern pole : "
                            + gdsv.getSpLon());
                    ps.println(
                        "                Rotation angle : "
                            + gdsv.getRotationAngle());
                    ps.println(
                        "              Latitude of pole : "
                            + gdsv.getPoleLat());
                    ps.println(
                        "             Longitude of pole : "
                            + gdsv.getPoleLon());
                    ps.println(
                        "             Stretching factor : "
                            + gdsv.getStretchingFactor());
                }
                break;

            case 10:  // Mercator
                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "         Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "         Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println("  Latitude of first grid point : " + gdsv.getLa1());
                ps.println(" Longitude of first grid point : " + gdsv.getLo1());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("   Latitude of last grid point : " + gdsv.getLa2());
                ps.println("  Longitude of last grid point : " + gdsv.getLo2());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());
                ps.println(
                    "                   Basic angle : "
                        + gdsv.getAngle());
                ps.println("         i direction increment : " + gdsv.getDx());
                ps.println("         j direction increment : " + gdsv.getDy());
                ps.println("                    Grid Units : " + gdsv.getGridUnits());

                break;

            case 20:  // Polar stereographic projection
                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "         Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "         Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println("  Latitude of first grid point : " + gdsv.getLa1());
                ps.println(" Longitude of first grid point : " + gdsv.getLo1());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());

                break;

            case 30:  // Lambert Conformal
                ps.println(
                    "                    Grid Shape : " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "         Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "         Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("                            Nx : " + gdsv.getNx());
                ps.println("                            Ny : " + gdsv.getNy());
                ps.println("                           La1 : " + gdsv.getLa1());
                ps.println("                           Lo1 : " + gdsv.getLo1());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("                           LaD : " + gdsv.getLaD());
                ps.println("                           LoV : " + gdsv.getLoV());
                ps.println("                            Dx : " + gdsv.getDx());
                ps.println("                            Dy : " + gdsv.getDy());
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println(
                    "             Projection center : "
                        + gdsv.getProjectionFlag());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());
                ps.println(
                    "                        Latin1 : "
                        + gdsv.getLatin1());
                ps.println(
                    "                        Latin2 : "
                        + gdsv.getLatin2());
                ps.println(
                    "                         SpLat : "
                        + gdsv.getSpLat());
                ps.println(
                    "                         SpLon : "
                        + gdsv.getSpLon());

                break;

            case 40:
            case 41:
            case 42:
            case 43:  // Gaussian latitude/longitude
                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "         Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "         Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println(
                    "                   Basic angle : "
                        + gdsv.getAngle());
                ps.println(
                    "    Subdivisions of basic angle: "
                        + gdsv.getSubDivisions());
                ps.println("  Latitude of first grid point : " + gdsv.getLa1());
                ps.println(" Longitude of first grid point : " + gdsv.getLo1());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println("   Latitude of last grid point : " + gdsv.getLa2());
                ps.println("  Longitude of last grid point : " + gdsv.getLo2());
                ps.println("         i direction increment : " + gdsv.getDx());
                ps.println(
                    "             Stretching factor : "
                        + gdsv.getStretchingFactor());
                ps.println("           Number of parallels : " + gdsv.getNp());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());

                if (gdsv.getGdtn() == 41) {  //Rotated Gaussian Latitude/longitude
                    ps.println(
                        "     Latitude of southern pole : "
                            + gdsv.getSpLat());
                    ps.println(
                        "    Longitude of southern pole : "
                            + gdsv.getSpLon());
                    ps.println(
                        "                Rotation angle : "
                            + gdsv.getRotationAngle());

                }
                else if (gdsv.getGdtn() == 42) {  //Stretched Gaussian
                    // Latitude/longitude
                    ps.println(
                        "              Latitude of pole : "
                            + gdsv.getPoleLat());
                    ps.println(
                        "             Longitude of pole : "
                            + gdsv.getPoleLon());
                    ps.println(
                        "             Stretching factor : "
                            + gdsv.getStretchingFactor());

                }
                else if (gdsv.getGdtn() == 43) {  //Stretched and Rotated Gaussian
                    // Latitude/longitude
                    ps.println(
                        "     Latitude of southern pole : "
                            + gdsv.getSpLat());
                    ps.println(
                        "    Longitude of southern pole : "
                            + gdsv.getSpLon());
                    ps.println(
                        "                Rotation angle : "
                            + gdsv.getRotationAngle());
                    ps.println(
                        "              Latitude of pole : "
                            + gdsv.getPoleLat());
                    ps.println(
                        "             Longitude of pole : "
                            + gdsv.getPoleLon());
                    ps.println(
                        "             Stretching factor : "
                            + gdsv.getStretchingFactor());
                }
                break;

        /*  no test files so not implemented
        case 50:
        case 51:
        case 52:
        case 53:  // Spherical harmonic coefficients
          ps.println("     J - pentagonal resolution : " + gdsv.getJ());
          ps.println("     K - pentagonal resolution : " + gdsv.getK());
          ps.println("     M - pentagonal resolution : " + gdsv.getM());
          ps.println("Method used to define the norm : "
                  + gdsv.getMethod());
          ps.println("     Mode indicating the order : " + gdsv.getMode());
          ps.println("                    Grid Units : " + gdsv.getGridUnits());
          if (gdsv.getGdtn() == 51) {  //Rotated Spherical harmonic coefficients
            ps.println("     Latitude of southern pole : "
                    + gdsv.getSpLat());
            ps.println("    Longitude of southern pole : "
                    + gdsv.getSpLon());
            ps.println("                Rotation angle : "
                    + gdsv.getRotationAngle());

          } else if (gdsv.getGdtn() == 52) {  //Stretched Spherical
            // harmonic coefficients
            ps.println("              Latitude of pole : "
                    + gdsv.getPoleLat());
            ps.println("             Longitude of pole : "
                    + gdsv.getPoleLon());
            ps.println("             Stretching factor : "
                    + gdsv.getStretchingFactor());

          } else if (gdsv.getGdtn() == 53) {  //Stretched and Rotated
            // Spherical harmonic coefficients
            ps.println("     Latitude of southern pole : "
                    + gdsv.getSpLat());
            ps.println("    Longitude of southern pole : "
                    + gdsv.getSpLon());
            ps.println("                Rotation angle : "
                    + gdsv.getRotationAngle());
            ps.println("              Latitude of pole : "
                    + gdsv.getPoleLat());
            ps.println("             Longitude of pole : "
                    + gdsv.getPoleLon());
            ps.println("             Stretching factor : "
                    + gdsv.getStretchingFactor());
          }
          break;
          */

            case 90:  // Space view perspective or orthographic
                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "        Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "        Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println("Latitude of sub-satellite point: " + gdsv.getLap());
                ps.println("  Longitude of sub-satellite pt: " + gdsv.getLop());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                ps.println("      Dx i direction increment : " + gdsv.getDx());
                ps.println("      Dy j direction increment : " + gdsv.getDy());
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println("Xp-coordinate of sub-satellite : " + gdsv.getXp());
                ps.println("Yp-coordinate of sub-satellite : " + gdsv.getYp());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());
                ps.println(
                    "                   Basic angle : "
                        + gdsv.getAngle());
                ps.println(
                    "     Nr Altitude of the camera : "
                        + gdsv.getNr());
                ps.println("       Xo-coordinate of origin : " + gdsv.getXo());
                ps.println("       Yo-coordinate of origin : " + gdsv.getYo());

                break;

        /* no test data, so not implemented
        case 100:  // Triangular grid based on an icosahedron
          ps.println("   Exponent of 2 for intervals : " + gdsv.getN2());
          ps.println("   Exponent of 3 for intervals : " + gdsv.getN3());
          ps.println("           Number of intervals : " + gdsv.getNi());
          ps.println("            Number of diamonds : " + gdsv.getNd());
          ps.println("              Latitude of pole : "
                  + gdsv.getPoleLat());
          ps.println("             Longitude of pole : "
                  + gdsv.getPoleLon());
          ps.println("           Grid point position : "
                  + gdsv.getPosition());
          ps.println("      Number order of diamonds : "
                  + gdsv.getOrder());
          ps.println("                 Scanning mode : "
                  + gdsv.getScanMode());
          ps.println("           Number of parallels : " + gdsv.getN());
          ps.println("                    Grid Units : " + gdsv.getGridUnits());
          break;

        case 110:  // Equatorial azimuthal equidistant projection
          ps.println("                     Grid Shape: " + gdsv.getShape()
                  + " " + Grib2Tables.getShapeName( gdsv.getShape() ));
          if (gdsv.getShape() == 1) {
            ps.println("         Spherical earth radius: "
                    + gdsv.getEarthRadius());

          } else if (gdsv.getShape() == 3) {
            ps.println("         Oblate earth major axis: "
                    + gdsv.getMajorAxis());
            ps.println("         Oblate earth minor axis: "
                    + gdsv.getMinorAxis());
          }
          ps.println("Number of points along parallel: " + gdsv.getNx());
          ps.println("Number of points along meridian: " + gdsv.getNy());
          ps.println("  Latitude of first grid point : " + gdsv.getLa1());
          ps.println(" Longitude of first grid point : " + gdsv.getLo1());
          ps.println("  Resolution & Component flags : "
                  + gdsv.getResolution());
          ps.println("                         Winds : " + winds);
          ps.println("         i direction increment : " + gdsv.getDx());
          ps.println("         j direction increment : " + gdsv.getDy());
          ps.println("                    Grid Units : " + gdsv.getGridUnits());
          ps.println("             Projection center : "
                  + gdsv.getProjectionCenter());
          ps.println("                 Scanning mode : "
                  + gdsv.getScanMode());

          break;
          */

          /* no test data, so not implemented
        case 120:  // Azimuth-range Projection
          ps.println("           Number of data bins : " + gdsv.getNb());
          ps.println("             Number of radials : " + gdsv.getNr());
          ps.println("Number of points along parallel: " + gdsv.getNx());
          ps.println("  Latitude of first grid point : " + gdsv.getLa1());
          ps.println(" Longitude of first grid point : " + gdsv.getLo1());
          ps.println("         i direction increment : " + gdsv.getDx());
          ps.println("                    Grid Units : " + gdsv.getGrid());
          ps.println("            Offset from origin : "
                  + gdsv.getDstart());
          ps.println("need code to get azi and adelta");

          break;
          */

            case 204:  // Curvilinear orthographic grib
                ps.println(
                    "                     Grid Shape: " + gdsv.getShape()
                        + " " + Grib2Tables.codeTable3_2(gdsv.getShape()));
                if (gdsv.getShape() == 1) {
                    ps.println(
                        "         Spherical earth radius: "
                            + gdsv.getEarthRadius());

                }
                else if (gdsv.getShape() == 3) {
                    ps.println(
                        "        Oblate earth major axis: "
                            + gdsv.getMajorAxis());
                    ps.println(
                        "        Oblate earth minor axis: "
                            + gdsv.getMinorAxis());
                }
                ps.println("Number of points along parallel: " + gdsv.getNx());
                ps.println("Number of points along meridian: " + gdsv.getNy());
                ps.println(
                    "  Resolution & Component flags : "
                        + gdsv.getResolution());
                ps.println("                         Winds : " + winds);
                //              ps.println("      Dx i direction increment : " + gdsv.getDx());
                //              ps.println("      Dy j direction increment : " + gdsv.getDy());
                ps.println("                    Grid Units : " + gdsv.getGridUnits());
                ps.println(
                    "                 Scanning mode : "
                        + gdsv.getScanMode());

                break;

            default:
                ps.println("Unknown Grid Type" + gdsv.getGdtn());

        }  // end switch gdtn
    }      // end printGDS

    private void printPDS(
        Grib2IndicatorSection is,
        Grib2Pds pdsv,
        JsonGenerator jg) {

        int productDefinition = pdsv.getProductDefinitionTemplate();

        jg.writeStartObject("Product Definition")
            .write(Integer.toString(productDefinition), Grib2Tables.codeTable4_0(productDefinition))
            .writeEnd();
        jg.writeStartObject("Parameter Category")
            .write(
                Integer.toString(pdsv.getParameterCategory()),
                ParameterTable.getCategoryName(is.getDiscipline(), pdsv.getParameterCategory()))
            .writeEnd();
        jg.writeStartObject("Parameter Name")
            .write(
                Integer.toString(pdsv.getParameterNumber()),
                ParameterTable.getParameterName(
                    is.getDiscipline(),
                    pdsv.getParameterCategory(),
                    pdsv.getParameterNumber()))
            .writeEnd();
        jg.write(
            "Parameter Units",
            ParameterTable.getParameterUnit(is.getDiscipline(), pdsv.getParameterCategory(), pdsv.getParameterNumber()));
        int tgp = pdsv.getGenProcessType();
        jg.writeStartObject("Generating Process Type")
            .write(Integer.toString(tgp), Grib2Tables.codeTable4_3(tgp))
            .writeEnd();
        jg.write("ForecastTime", pdsv.getForecastTime());
        jg.writeStartObject("First Surface Type")
            .write(Integer.toString(pdsv.getLevelType1()), Grib2Tables.codeTable4_5(pdsv.getLevelType1()))
            .writeEnd();
        jg.write("First Surface value", pdsv.getLevelValue1());
        jg.writeStartObject("Second Surface Type")
            .write(Integer.toString(pdsv.getLevelType2()), Grib2Tables.codeTable4_5(pdsv.getLevelType2()))
            .writeEnd();
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
