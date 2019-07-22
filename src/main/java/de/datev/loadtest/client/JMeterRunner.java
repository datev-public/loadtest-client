package de.datev.loadtest.client;

import com.blazemeter.jmeter.RandomCSVDataSetConfig;
import de.datev.loadtest.client.config.LoadClientConfiguration;
import de.datev.loadtest.client.result.CustomSummariser;
import de.datev.loadtest.client.result.LogCollector;
import de.datev.loadtest.client.result.LogFile;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.ResultCollector2;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
public class JMeterRunner {

    private static final Logger log = LoggerFactory.getLogger(JMeterRunner.class);

    private boolean running = false;
    private File resourcesDir;
    private LogCollector logCollector;
    private LoadClientConfiguration loadClientConfiguration;
    private CustomSummariser summariser;

    @Autowired
    public JMeterRunner(LogCollector logCollector, LoadClientConfiguration loadClientConfiguration, CustomSummariser summariser) {

        log.info("JMeterRunner CTOR");
        this.logCollector = logCollector;
        this.loadClientConfiguration = loadClientConfiguration;
        this.summariser = summariser;
    }

    @PostConstruct
    private void init() throws IOException {

        log.info("init");
        this.resourcesDir = fileSetup();
        this.resourcesDir.deleteOnExit();

        JMeterUtils.loadJMeterProperties(resourcesDir.getPath() + "/jmeter/bin/jmeter.properties");
        File logDir = new File(loadClientConfiguration.getLogPath());
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new IOException("Cannot create directory \"" + logDir + "\"");
        }

        summariser.setProperty("summariser.interval", loadClientConfiguration.getSummariserInterval());

        JMeterUtils.setJMeterHome(resourcesDir.getPath() + "/jmeter");
        JMeterUtils.setLocale(Locale.ENGLISH);
    }

    void run(CountDownLatch latch) throws Exception {

        try {
            this.run();
        } finally {
            latch.countDown();
        }
    }

    private void run() throws Exception {

        // JMeter Engine
        final StandardJMeterEngine jmeter = new StandardJMeterEngine();

        // Initialize JMeter SaveService
        SaveService.loadProperties();

        // Load existing .jmx Test Plan from local temp directory
        final File jmxFile = new File(resourcesDir, loadClientConfiguration.getJmxFile());
        log.debug("run jmxFile={}", jmxFile);

        final HashTree hashTree = SaveService.loadTree(jmxFile);
        replaceFileNameInCsvDataSets(hashTree);

        // Overwrite target host
        if (loadClientConfiguration.getTargetHost() != null) {
            ConfigTestElement httpRequestDefaults = this.findRequestDefaults(hashTree);
            if (httpRequestDefaults == null) {
                throw new IllegalStateException("HttpRequestDefaults not found in JMX file \"" + jmxFile + "\"");
            }
            httpRequestDefaults.setProperty("HTTPSampler.domain", loadClientConfiguration.getTargetHost());
            if (loadClientConfiguration.getTargetProtocol() != null) {
                httpRequestDefaults.setProperty("HTTPSampler.protocol", loadClientConfiguration.getTargetProtocol());
            }
            if (loadClientConfiguration.getTargetPort() != null) {
                httpRequestDefaults.setProperty("HTTPSampler.port", loadClientConfiguration.getTargetPort());
            }
            log.debug("run overwrite target: targetHost={}, targetProtocol={}, targetPort={}",
                    httpRequestDefaults.getPropertyAsString("HTTPSampler.domain"),
                    httpRequestDefaults.getPropertyAsString("HTTPSampler.protocol"),
                    httpRequestDefaults.getPropertyAsInt("HTTPSampler.port"));
        }
        // Overwrite number of threads - threading is done outside
        final ThreadGroup threadGroup = this.findThreadGroup(hashTree);
        if (threadGroup == null) {
            throw new IllegalStateException("ThreadGroup not found in JMX file \"" + jmxFile + "\"");
        }
        threadGroup.setNumThreads(1);
        final LoopController loopController = (org.apache.jmeter.control.LoopController) threadGroup.getSamplerController();
        // Overwrite loops
        loopController.setLoops(loadClientConfiguration.getRunsInOneLoop());
        log.debug("run with {} loops", loopController.getLoops());

        final ResultCollector2 resultCollector = new ResultCollector2(summariser);
        final TestPlan testPlan = (TestPlan) hashTree.getArray()[0];
        hashTree.add(testPlan, resultCollector);

        if (loadClientConfiguration.isLogFilesEnabled()) {

            final String threadName = Thread.currentThread().getName();
            File logFile = new File(this.getLogDir(), "/" + threadName + ".csv");
            log.info("run setting log file {} - exists = {}", logFile, logFile.exists());
            if (!logFile.exists()) {
                this.logCollector.addLogFile(logFile);
            }
            resultCollector.setFilename(logFile.getPath());
        }

        // Run JMeter Test
        jmeter.configure(hashTree);
        final long start = System.currentTimeMillis();
        jmeter.run();
        final long end = System.currentTimeMillis();
        log.info("done after {} secs", (end - start) / 1000);
    }

    //------------------------------------------------------------------------------------------------------------------

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        log.info("setRunning running={}", running);
        this.running = running;
    }

    /**
     * Store a JMS file for the load.
     * @param filePath  Path of JMX file
     * @param in The input stream to read from
     * @return The list of file names, which are CSV data sets within the JMX file and have to be uploaded
     * @throws IOException on any read error
     */
    public List<String> setAndStoreJmxFile(String filePath, InputStream in) throws IOException {
        log.info("setAndStoreJmxFile {}", filePath);
        File target = new File(resourcesDir, filePath);
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        final HashTree hashTree = SaveService.loadTree(target);
        return this.findCsvDataSets(hashTree)
                .stream()
                .map(configTestElement -> configTestElement.getPropertyAsString("filename"))
                .filter(name -> name != null && name.trim().length() > 0)
                .collect(Collectors.toList());
    }

    public void storeSupportFile(String filePath, InputStream in) throws IOException {
        log.info("storeSupportFile {}", filePath);
        File target = new File(resourcesDir, filePath);
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public File getFilePath(String filePath) {
        return new File(this.resourcesDir, filePath);
    }

    public File getLogDir() {
        return new File(this.resourcesDir, loadClientConfiguration.getLogPath());
    }

    public List<LogFile> getLogFiles() {
        return this.logCollector.getLogFiles().stream()
                .map(LogFile::new).collect(Collectors.toList());
    }

    public void deleteLogFiles() {
        this.logCollector.getLogFiles()
                .forEach(File::delete);
    }

    //------------------------------------------------------------------------------------------------------------------

    private void fileCopy(Path tmpDir, String resourceName) throws IOException {
        File target = new File(tmpDir.toFile(), resourceName);
        if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            throw new IOException("Cannot create parent directory for \"" + target + "\"");
        }
        InputStream in = getClass().getResourceAsStream("/" + resourceName);
        if (in == null) {
            throw new IllegalArgumentException("The resource name \"" + resourceName + "\" is not found!");
        }
        Files.copy(in, target.toPath());
    }

    private File fileSetup() throws IOException {

        Path tmpDir = Files.createTempDirectory("jmeter");
        fileCopy(tmpDir, "jmeter/bin/jmeter.properties");
        fileCopy(tmpDir, "jmeter/bin/saveservice.properties");
        fileCopy(tmpDir, "jmeter/bin/upgrade.properties");
        fileCopy(tmpDir, loadClientConfiguration.getJmxFile());

        for (String fileName : loadClientConfiguration.getCsvFilesAsList()) {
            log.info("copying CSV file \"{}\"", fileName);
            fileCopy(tmpDir, fileName);
        }
        return tmpDir.toFile();
    }

    private ThreadGroup findThreadGroup(HashTree hashTree) {

        TestPlan testPlan = (TestPlan) hashTree.getArray()[0];
        for (Object o : hashTree.getArray(testPlan)) {
            if (o instanceof ThreadGroup) {
                return (ThreadGroup) o;
            }
        }
        return null;
    }

    private ConfigTestElement findRequestDefaults(HashTree hashTree) {

        TestPlan testPlan = (TestPlan) hashTree.getArray()[0];
        for (Object o : hashTree.getArray(testPlan)) {
            if (o instanceof ConfigTestElement) {
                ConfigTestElement configTestElement = (ConfigTestElement) o;
                if (configTestElement.getProperty("HTTPSampler.domain") != null) {
                    return configTestElement;
                }
            }
        }
        return null;
    }

    private List<ConfigTestElement> findCsvDataSets(HashTree testPlanTree) {

        List<ConfigTestElement> ret = new ArrayList<>();
        testPlanTree.traverse(new HashTreeTraverser() {
            @Override
            public void addNode(Object o, HashTree hashTree) {
                if (o instanceof CSVDataSet) {
                    ret.add((CSVDataSet) o);
                } else if (o instanceof RandomCSVDataSetConfig) {
                    ret.add((RandomCSVDataSetConfig) o);
                }
            }

            @Override
            public void subtractNode() {

            }

            @Override
            public void processPath() {

            }
        });
        return ret;
    }

    private void replaceFileNameInCsvDataSets(HashTree testPlanTree) {

        this.findCsvDataSets(testPlanTree).forEach(this::replaceFilename);
    }

    private boolean replaceFilename(ConfigTestElement csvDataSet) {

        log.debug("replaceFilename in \"{}\"", csvDataSet.getName());
        final String filename = csvDataSet.getPropertyAsString("filename");
        if (filename != null && !filename.startsWith("/")) {
            csvDataSet.setProperty("filename", new File(this.resourcesDir, "jmx/" + filename)
                    .getAbsolutePath().replace("\\", "/"));
            log.info("replaced file name in \"{}\" by \"{}\"", csvDataSet.getName(), csvDataSet.getPropertyAsString("filename"));
            return true;
        } else {
            return false;
        }
    }
}