package testdata;

import java.sql.Timestamp;

/**
 * Created by debmalya.biswas on 6/2/15(3:56 PM)
 */
final public class MockTime {
    private final static Timestamp TIME = Timestamp.valueOf("2014-12-08 16:49:34.008");

    public static Timestamp getTime() {
        return TIME;
    }
}
