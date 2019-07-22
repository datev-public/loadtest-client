package de.datev.loadtest.client;

import de.datev.loadtest.client.config.LoadClientConfiguration;
import de.datev.loadtest.client.control.ResultService;
import de.datev.loadtest.client.control.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@EnableScheduling
public class LoadTestClientApplication {

    private static final Logger log = LoggerFactory.getLogger(LoadTestClientApplication.class);

    @Autowired
    private LoadClientConfiguration configuration;

    @Autowired
    private JMeterRunner jMeterRunner;

    @Autowired
    private ResultService resultService;
    @Autowired
    private SettingsService settingsService;

    private int nrOfThreads = 1;
    private CountDownLatch latch;
    private int loopIndex = 0;
    private long totalNumberOfRequests = 0;
    private String cfInstanceIndex = "0";
    // Hash comparision not yet used
    /*
    private File currentJmxFilePath = null;
    private String currentJmxFileHashValue = null;
    private MessageDigest messageDigest = null;
    */

    public static void main(String[] args) {
        SpringApplication.run(LoadTestClientApplication.class, args);
    }

    // Hash comparision not yet used
    /*
    @PostConstruct
    public void init() throws Exception {
        messageDigest = MessageDigest.getInstance("MD5");
    }
    */

    @EventListener(ApplicationReadyEvent.class)
    public void startup() throws Exception {

        cfInstanceIndex = System.getenv("CF_INSTANCE_INDEX");
        log.info("LoadTestClientApplication ApplicationReadyEvent CF_INSTANCE_INDEX={}", cfInstanceIndex);

        if (configuration.isShowConfigOnStartup()) {
            log.info(configuration.toString());
        }
        nrOfThreads = configuration.getNumberOfThreads();

        if (configuration.isRedisEnabled()) {

            // Instances not running on CF or the CF instance 0 may purge the database
            if (configuration.isRedisPurgeOnStart() && (cfInstanceIndex == null || "0".equals(cfInstanceIndex))) {
                resultService.purge();
                settingsService.purge();
            }

            // If running locally or in PCF instance 0
            if (cfInstanceIndex == null || "0".equals(cfInstanceIndex)) {
                settingsService.storeSettings(configuration);
                log.info("Stored configuration in REDIS by CF instance 0: {}", configuration);
                log.info("Storing JMX file in REDIS by CF instance 0");
                File jmxFilePath = jMeterRunner.getFilePath(configuration.getJmxFile());
                boolean success = settingsService.storeJmxFileToRedis(jmxFilePath);
                if (success) {
                    log.info("Stored JMX file {} in REDIS", jmxFilePath);
                } else {
                    log.error("ERROR storing JMX file {} in REDIS", jmxFilePath);
                }
            } else {
                configuration.set(settingsService.retrieveSettings());
                log.info("Loaded configuration from REDIS by CF instance {}: {}", cfInstanceIndex, configuration);
                log.info("Loading JMX file from REDIS by CF instance {}", cfInstanceIndex);
                File jmxFilePath = jMeterRunner.getFilePath(configuration.getJmxFile());
                boolean success = settingsService.loadJmxFileFromRedisToFileSystem(jmxFilePath);
                if (success) {
                    log.info("Loaded JMX file {} from REDIS", jmxFilePath);
                } else {
                    log.error("ERROR loading JMX file {} from REDIS", jmxFilePath);
                }
            }
        }
        startLoop();
    }

    public int getLoopIndex() {
        return loopIndex;
    }

    public String getCfInstanceIndex() {
        return cfInstanceIndex;
    }

    public long getTotalNumberOfRequests() {
        return totalNumberOfRequests;
    }

    private void startLoop() throws Exception {

        Thread.sleep(configuration.getInitialDelayMilliseconds());
        log.debug("nrOfThreads={}", nrOfThreads);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(nrOfThreads);
        executor.setMaxPoolSize(nrOfThreads);
        executor.setThreadNamePrefix("jmeter-" + nrOfThreads + "-");
        executor.initialize();

        long start = System.currentTimeMillis();
        for (; loopIndex < configuration.getNumberOfLoops(); loopIndex++) {

            boolean first = true;
            while (!isRunning()) {
                if (first) {
                    log.info("waiting for run to be enabled");
                }
                first = false;
                Thread.sleep(configuration.getPollIntervalMilliseconds());
            }

            // Fetch configuration from redis
            if (configuration.isRedisEnabled()) {
                configuration.set(settingsService.retrieveSettings());
                log.info("Reloaded configuration from REDIS by CF instance {}: {}", cfInstanceIndex, configuration);
            }

            // The number of threads was changed
            if (configuration.getNumberOfThreads() != nrOfThreads) {
                log.info("numberOfThreads changed from {} to {}", nrOfThreads, configuration.getNumberOfThreads());
                nrOfThreads = configuration.getNumberOfThreads();
                executor.destroy();
                executor = new ThreadPoolTaskExecutor();
                executor.setCorePoolSize(nrOfThreads);
                executor.setMaxPoolSize(nrOfThreads);
                executor.setThreadNamePrefix("jmeter-" + nrOfThreads + "-");
                executor.initialize();
            }

            if (configuration.isRedisEnabled()) {
                File jmxFilePath = jMeterRunner.getFilePath(configuration.getJmxFile());
                boolean success = settingsService.loadJmxFileFromRedisToFileSystem(jmxFilePath);
                if (success) {
                    log.info("Reloaded JMX file {} from REDIS", jmxFilePath);
                } else {
                    log.error("ERROR reloading JMX file {} from REDIS", jmxFilePath);
                }
            }
            log.info("loopIndex={}", loopIndex);
            latch = new CountDownLatch(nrOfThreads);
            for (int threadNr = 0; threadNr < nrOfThreads; threadNr++) {
                executor.execute(this::singleRun);
            }
            latch.await();
            totalNumberOfRequests += nrOfThreads * configuration.getRunsInOneLoop();
            Thread.sleep(configuration.getWaitIntervalMilliseconds());
        }
        long end = System.currentTimeMillis();
        log.info("LOOPS DONE totalNumberOfRequests={}, totalSeconds={}",
                totalNumberOfRequests, (end - start) / 1000);
    }

    private void singleRun() {
        try {
            log.debug("singleRun {}", Thread.currentThread());
            jMeterRunner.run(latch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isRunning() {

        if (configuration.isRedisEnabled()) {
            return settingsService.isRunning();
        } else {
            return jMeterRunner.isRunning();
        }
    }

    /*
    // Hash comparision not yet used
    private void rememberJmxFile(File file) throws IOException {

        currentJmxFilePath = file;
        currentJmxFileHashValue = fileHash(file);
        log.info("rememberJmxFile path={}, hash={}", currentJmxFilePath, currentJmxFileHashValue);
    }

    private boolean jmxFileWasChanged(File file) throws IOException {

        final String newJmxFileHash = fileHash(file);
        boolean ret = !currentJmxFileHashValue.equals(newJmxFileHash);
        if (ret) {
            log.info("jmxFileHashWasChanged from hash {} to hash {} for path {}", currentJmxFileHashValue, newJmxFileHash, file);
        }
        return ret;
    }

    private String fileHash(File file) throws IOException {

        return DatatypeConverter.printHexBinary(messageDigest.digest(Files.readAllBytes(file.toPath())));
    }
    */
}
