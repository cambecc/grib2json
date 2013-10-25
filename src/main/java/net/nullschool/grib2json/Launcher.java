package net.nullschool.grib2json;

import com.lexicalscope.jewel.JewelRuntimeException;
import com.lexicalscope.jewel.cli.CliFactory;

/**
 * 2013-10-24<p/>
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
                if (options.isHelp() || options.getFile() == null) {
                    printUsage();
                    return;
                }
            }
            catch (JewelRuntimeException t) {
                printUsage();
                System.out.println();
                System.err.println(t.getMessage());
                System.exit(-1);
                return;
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
