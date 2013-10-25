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
    }

    /**
     * Dump of content of the Grib2 file to text.
     *
     * @param args Gribfile,  [output file], [true|false] output data.
     */
    public void gribDump(String args[]) throws IOException {
        boolean displayData = false;

        RandomAccessFile raf;
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
            throw new IllegalArgumentException("unexpected arg count: " + Arrays.toString(args));
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
        Grib2Data gd = new Grib2Data(raf);

        jg.writeStartArray();
        boolean first = true;

        // record contains objects for all 8 Grib2 sections
        List<Grib2Record> records = g2i.getRecords();
        for (Grib2Record record : records) {

            jg.writeStartObject();

            RecordWriter rw = new RecordWriter(jg, record, true);
            rw.writeHeader();

            if (displayData && first) {
                rw.writeData(gd);
            }

            jg.writeEnd();
            first = false;
        }

        jg.writeEnd();
        jg.close();

        raf.close();
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

        try {
            g2d.gribDump(args);
        }
        catch (Throwable t) {
            System.err.println(t);
        }
    }
}
