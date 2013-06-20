/*
 * Copyright (c) 2013, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner.autoreload;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.FakeTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.aeonbits.owner.UtilTest.save;
import static org.aeonbits.owner.UtilTest.saveJar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Luigi R. Viggiano
 */
public class SyncAutoReloadTest {

    private static final String propertyFileName = "AutoReloadConfig.properties";
    private static final String jarFile = "target/test-resources/SyncAutoReloadTest.jar";

    private static final String spec = "file:target/test-resources/" + propertyFileName;
    private static final String specJar = "jar:file:" + jarFile + "!/" + propertyFileName;

    private static File target;
    private static File jarTarget;

    private static FakeTime time;

    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        target = new File(new URL(spec).getFile());
        jarTarget = new File(jarFile);
    }

    @Before
    public void before() {
        time = new FakeTime();
        time.setup();                // become owner of time (now I can control the elapse of time in this test)
    }

    @Sources(spec)
    @HotReload(5)
    interface AutoReloadConfig extends Config {
        Integer someValue();
    }

    @Test
    public void testAutoReload() throws IOException, InterruptedException {
        save(target, new Properties() {{
            setProperty("someValue", "10");
        }});
        boolean success = target.setLastModified(target.lastModified() - 15000); // make the file 15 seconds older.
        assertTrue(success);
        time.setTime(target.lastModified());                   // set the time for this test to match the file creation.

        AutoReloadConfig cfg = ConfigFactory.create(AutoReloadConfig.class);
        assertEquals(Integer.valueOf(10), cfg.someValue());

        save(target, new Properties() {{        // file updated, the current time is set in target.lastModified().
            setProperty("someValue", "20");
        }});

        time.elapse(4, SECONDS);                             // make 4 seconds elapse.
        assertEquals(Integer.valueOf(10), cfg.someValue());  // change is not reflected yet since interval is 5 secs.

        time.elapse(1, SECONDS);                             // another second is elapsed
        assertEquals(Integer.valueOf(20), cfg.someValue());  // the changed file should be reloaded now.
    }

    @Sources(specJar)
    @HotReload(5)
    interface AutoReloadJarConfig extends Config {
        Integer someValue();
    }

    @Test
    public void testAutoReloadOnJarFile() throws Throwable {
        saveJar(jarTarget, propertyFileName,
                new Properties() {{
                    setProperty("someValue", "10");
                }});

        boolean success = jarTarget.setLastModified(jarTarget.lastModified() - 15000); // make the file 15 seconds older
        assertTrue(success);

        time.setTime(jarTarget.lastModified());              // set the time for this test to match the file creation

        AutoReloadJarConfig cfg = ConfigFactory.create(AutoReloadJarConfig.class);
        assertEquals(Integer.valueOf(10), cfg.someValue());

        saveJar(jarTarget, propertyFileName,    // file updated, the current time is set in target.lastModified().
                new Properties() {{
                    setProperty("someValue", "20");
                }});

        time.elapse(4, SECONDS);                             // make 4 seconds elapse.
        assertEquals(Integer.valueOf(10), cfg.someValue());  // change is not reflected yet since interval is 5 secs.

        time.elapse(1, SECONDS);                             // another second is elapsed
        assertEquals(Integer.valueOf(20), cfg.someValue());  // the changed file should be reloaded now.
    }


    @After
    public void after() {
        time.tearDown();
    }

}