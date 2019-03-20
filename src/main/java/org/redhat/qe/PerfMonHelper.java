package org.redhat.qe;

import kg.apc.jmeter.perfmon.PerfMonCollector;
import kg.apc.jmeter.vizualizers.PerfMonGui;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.apache.jorphan.collections.ListedHashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Utility class for configuring PerfMon inside an existing JMeter JMX file
 */
public class PerfMonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PerfMonHelper.class.getName());
    private int tagName = 900_000_000;

    public void copyStream(InputStream in, File outFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.close();
    }

    protected void configurePerfMon(File inputFile, File outputFile, String filename, Map<String, String> targetSet) throws IOException {
        /*
         * The following files must be available due to the GUI nature of the project:
         * - jmeter.properties
         * - saveservice.properties
         * - upgrade.properties
         */
        JMeterUtils.loadJMeterProperties(
                Thread.currentThread().getContextClassLoader().getResource("jmeter.properties").getFile()
        );
        JMeterUtils.setProperty(
                "saveservice_properties",
                Thread.currentThread().getContextClassLoader().getResource("saveservice.properties").getFile()
        );
        JMeterUtils.setProperty(
                "upgrade_properties",
                Thread.currentThread().getContextClassLoader().getResource("upgrade.properties").getFile()
        );
        SaveService.loadProperties();
        HashTree tree = SaveService.loadTree(inputFile);

        final HashTree[] hashTreePerfMon = {null};
        final PerfMonCollector[] perfMonCollector = {null};
        tree.traverse(new HashTreeTraverser() {
            @Override
            public void addNode(Object key, HashTree hashTree) {
                if (key instanceof PerfMonCollector) {
                    LOG.debug("\n\nFOUND PerfMonCollector!!!!\n\n");
                    perfMonCollector[0] = PerfMonCollector.class.cast(key);
                } else if (key instanceof org.apache.jmeter.threads.ThreadGroup) {
                    LOG.debug("\n\nFOUND ThreadGroup!!!!\n\n");
                    // that is the element where to append the PerfMonCollector
                    hashTreePerfMon[0] = hashTree;
                }
                //LOG.debug(key + " ==> " + hashTree);
            }

            @Override
            public void subtractNode() {

            }

            @Override
            public void processPath() {

            }
        });

        if (perfMonCollector[0] != null) {
            // PerfMonCollector exists: we have to configure it properly
            LOG.debug("\n\nCONFIGURING PerfMonCollector!!!!\n\n");
            configurePerfMonCollector(perfMonCollector[0], filename, targetSet);
        } else if (hashTreePerfMon[0] != null) {
            // PerfMonCollector does not exist: we have to create it
            LOG.debug("\n\nCREATING PerfMonCollector!!!!\n\n");
            addPerfMonCollector(hashTreePerfMon[0], filename, targetSet);
        } else {
            throw new IllegalStateException("JMX file does not contain a ThreadGroup where to append PerfMonCollector!");
        }

        SaveService.saveTree(
                tree,
                new FileOutputStream(outputFile)
        );
    }

    private void addPerfMonCollector(HashTree tree, String filename, Map<String, String> targetSet) {
        PerfMonGui instance = new PerfMonGui();
        PerfMonCollector collector = (PerfMonCollector) instance.createTestElement();
        configurePerfMonCollector(collector, filename, targetSet);
        tree.add(
                collector,
                new ListedHashTree()
        );
    }

    private void configurePerfMonCollector(PerfMonCollector collector, String filename, Map<String, String> targetSet) {
        collector.setFilename(filename);
        collector.setEnabled(true);
        // targets
        configureMetricConnections(collector, targetSet);
    }

    private CollectionProperty createCollectionProperty(Map.Entry<String,String> target, String metricName) {
        CollectionProperty metricConnection1 = new CollectionProperty();
        metricConnection1.setName("-" + tagName++);
        StringProperty prop1 = new StringProperty();
        prop1.setName("" + tagName++);
        prop1.setValue(target.getKey());
        StringProperty prop2 = new StringProperty();
        prop2.setName("" + tagName++);
        prop2.setValue(target.getValue());
        StringProperty prop3 = new StringProperty();
        prop3.setName("" + tagName++);
        prop3.setValue(metricName);
        StringProperty prop4 = new StringProperty();
        prop4.setName("0");
        metricConnection1.addProperty(prop1);
        metricConnection1.addProperty(prop2);
        metricConnection1.addProperty(prop3);
        metricConnection1.addProperty(prop4);
        return metricConnection1;
    }

    private void configureMetricConnections(PerfMonCollector collector, Map<String, String> targetSet) {
        CollectionProperty metricConnections;
        LOG.debug("\n\nCREATING " + PerfMonCollector.DATA_PROPERTY + "!!!!\n\n");
        metricConnections = new CollectionProperty();
        metricConnections.setName(PerfMonCollector.DATA_PROPERTY);

        for (Map.Entry<String,String> target: targetSet.entrySet()) {
            CollectionProperty metricConnection1 = createCollectionProperty(target, "CPU");
            metricConnections.addProperty(metricConnection1);

            CollectionProperty metricConnection2 = createCollectionProperty(target, "Memory");
            metricConnections.addProperty(metricConnection2);

            CollectionProperty metricConnection3 = createCollectionProperty(target, "Network I/O");
            metricConnections.addProperty(metricConnection3);
        }

        collector.setProperty(metricConnections);
    }
}
