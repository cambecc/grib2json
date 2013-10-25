package net.nullschool.grib2json;

import ch.qos.logback.classic.LoggerContext;
import com.lexicalscope.jewel.JewelRuntimeException;
import com.lexicalscope.jewel.cli.CliFactory;
import org.slf4j.LoggerFactory;

/**
 * 2013-10-24<p/>
 *
 * Execution shim for the grib2json utility. Parses command line options and invokes the {@link Grib2Json} converter.
 *
 * @author Cameron Beccario
 */
class Launcher {

    private static void printUsage() {
        System.out.println(CliFactory.createCli(Options.class).getHelpMessage());
    }

    public static void main(String args[]) {
        try {
            Options options;
            try {
                options = CliFactory.parseArguments(Options.class, args);
            }
            catch (JewelRuntimeException t) {
                printUsage();
                System.out.println();
                System.err.println(t.getMessage());
                System.exit(-1);
                return;
            }

            if (options.getShowHelp() || options.getFile() == null) {
                printUsage();
                return;
            }

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            if (!options.getEnableLogging()) {
                lc.stop();
            }

            new Grib2Json(options).write();
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
