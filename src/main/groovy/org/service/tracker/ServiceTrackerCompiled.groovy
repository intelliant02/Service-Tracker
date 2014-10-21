package org.moqui.tutorial
import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 * Created by debmalya.biswas on 5/9/14.
 */
class ServiceTrackerCompiled {
    protected final static Logger logger = LoggerFactory.getLogger(ServiceTrackerCompiled.class)

    static void testMethod() { logger.warn("The testMethod was called!") }
    static String echo(String input) { input }

}
