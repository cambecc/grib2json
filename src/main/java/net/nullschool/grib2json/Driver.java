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

import com.lexicalscope.jewel.JewelRuntimeException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import ucar.grib.grib2.*;

import java.io.*;
import java.nio.file.*;
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
class Driver {

    static void validateOptions(Options options) {
        Path path = options.getFile().toPath();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Cannot find input file: " + path);
        }
    }

    static void gribDump(Options options) throws IOException {
        validateOptions(options);

        RandomAccessFile raf = new RandomAccessFile(options.getFile().getPath(), "r");
        raf.order(RandomAccessFile.BIG_ENDIAN);
        Grib2Input g2i = new Grib2Input(raf);
        if (!g2i.scan(false, false)) {
            throw new IllegalArgumentException("Failed to successfully scan grib file.");
        }

        OutputStream out = options.getOutput() != null ?
            new BufferedOutputStream(new FileOutputStream(options.getOutput(), false)) :
            System.out;

        Map<String, Object> properties = new HashMap<>();
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
        JsonGenerator jg = jgf.createGenerator(out);

        jg.writeStartArray();
        boolean first = true;

        List<Grib2Record> records = g2i.getRecords();
        for (Grib2Record record : records) {

            jg.writeStartObject();

            RecordWriter rw = new RecordWriter(jg, record, options.isNames());
            rw.writeHeader();

            if (options.isData() && first) {
                rw.writeData(new Grib2Data(raf));
            }

            jg.writeEnd();
            first = false;
        }

        jg.writeEnd();
        jg.close();

        raf.close();
    }

    public static void main(String args[]) {
        try {
            Cli<Options> cli = CliFactory.createCli(Options.class);
            Options options;
            try {
                options = CliFactory.parseArguments(Options.class, args);
                if (options.isHelp()) {
                    System.out.println(cli.getHelpMessage());
                    return;
                }
            }
            catch (JewelRuntimeException t) {
                System.out.println(cli.getHelpMessage());
                System.out.println();
                System.err.println(t.getMessage());
                System.exit(-1);
                return;
            }

            gribDump(options);
        }
        catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(-2);
        }
    }
}
