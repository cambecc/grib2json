package net.nullschool.grib2json;

import org.junit.Test;


/**
 * 2013-10-24<p/>
 *
 * @author Cameron Beccario
 */
public class DriverTest {

    @Test
    public void test_1() {
        Driver.main(new String[] {"c:/users/cambecc/desktop/gfs/gfs.t18z.pgrbf00.2p5deg.grib2", "out.txt", "true"});
    }
}
