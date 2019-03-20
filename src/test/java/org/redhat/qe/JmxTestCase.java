package org.redhat.qe;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class JmxTestCase {

    /**
     * PerfMon configuration in a JMX file
     * @throws IOException
     */
    @Test
    public void testAddNewPerfMon() throws IOException {
        PerfMonHelper helper = new PerfMonHelper();
        Map<String,String> targetSet = new HashMap<>();
        targetSet.put("host1","4444");
        targetSet.put("host2","4444");
        String filename = "/tmp/perfmon.csv";

        InputStream jmxFileInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("Sample-TestPlanHTTP-no-perfmon.jmx");
        File inputFile = new File("/tmp/no-perfmon-in.jmx");
        helper.copyStream(jmxFileInputStream,inputFile);

        File outputFile = new File("/tmp/no-perfmon-out.jmx");

        helper.configurePerfMon(inputFile, outputFile, filename, targetSet);

        String jmx = org.apache.commons.io.IOUtils.toString(new FileInputStream(outputFile));
        assertTrue(jmx.contains("PerfMonCollector"));

        Files.delete(Paths.get(inputFile.getAbsolutePath()));
        Files.delete(Paths.get(outputFile.getAbsolutePath()));
    }

    /**
     * PerfMon configuration in a JMX file
     * @throws IOException
     */
    @Test
    public void testConfigureExistingPerfMon() throws IOException {
        PerfMonHelper helper = new PerfMonHelper();
        Map<String,String> targetSet = new HashMap<>();
        targetSet.put("host1","4444");
        targetSet.put("host2","4444");
        String filename = "/tmp/perfmon.csv";

        InputStream jmxFileInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("Sample-TestPlanHTTP-perfmon.jmx");
        File inputFile = new File("/tmp/perfmon-in.jmx");
        helper.copyStream(jmxFileInputStream,inputFile);

        File outputFile = new File("/tmp/perfmon-out.jmx");

        helper.configurePerfMon(inputFile, outputFile, filename, targetSet);

        String jmx = org.apache.commons.io.IOUtils.toString(new FileInputStream(outputFile));
        assertTrue(jmx.contains("PerfMonCollector"));

        Files.delete(Paths.get(inputFile.getAbsolutePath()));
        Files.delete(Paths.get(outputFile.getAbsolutePath()));
    }

}
