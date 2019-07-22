package de.datev.loadtest.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Component
@ConfigurationProperties(
        prefix = "app-config",
        ignoreUnknownFields = false
)
public class LoadClientConfiguration implements Serializable { // Serializable because it is stored in REDIS

    private static final long serialVersionUID = 1L;

    private long lastModified = System.currentTimeMillis();

    private boolean showConfigOnStartup = false;
    private boolean startRunning = false;
    private boolean redisEnabled = true;
    private boolean redisPurgeOnStart = false;
    private boolean logOutputEnabled = true;
    private boolean logFilesEnabled = false;
    private int numberOfThreads = 1;
    private int runsInOneLoop = 100;
    private int numberOfLoops = Integer.MAX_VALUE;
    private String jmxFile = "jmx/smoke-test-no-auth.jmx";
    private String csvFiles = null;
    private String logPath = "jmeter-log";
    private long initialDelayMilliseconds = 1000L;
    private long waitIntervalMilliseconds = 0L;
    private long pollIntervalMilliseconds = 0L;
    private int summariserInterval = 30;
    private int uiLimitSummaries = 10;
    private String targetHost = null;
    private Integer targetPort = null;
    private String targetProtocol = null;
    private String targetPathPrefix = null;
    private String timeZone = "Europe/Paris";

    public long getLastModified() {
        return lastModified;
    }

    public boolean isShowConfigOnStartup() {
        return showConfigOnStartup;
    }

    public void setShowConfigOnStartup(boolean showConfigOnStartup) {
        this.showConfigOnStartup = showConfigOnStartup;
    }

    public boolean isStartRunning() {
        return startRunning;
    }

    public void setStartRunning(boolean startRunning) {
        this.startRunning = startRunning;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public boolean isRedisPurgeOnStart() {
        return redisPurgeOnStart;
    }

    public void setRedisPurgeOnStart(boolean redisPurgeOnStart) {
        this.redisPurgeOnStart = redisPurgeOnStart;
    }

    public boolean isLogOutputEnabled() {
        return logOutputEnabled;
    }

    public void setLogOutputEnabled(boolean logOutputEnabled) {
        this.logOutputEnabled = logOutputEnabled;
    }

    public boolean isLogFilesEnabled() {
        return logFilesEnabled;
    }

    public void setLogFilesEnabled(boolean logFilesEnabled) {
        this.logFilesEnabled = logFilesEnabled;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public int getRunsInOneLoop() {
        return runsInOneLoop;
    }

    public void setRunsInOneLoop(int runsInOneLoop) {
        this.runsInOneLoop = runsInOneLoop;
    }

    public int getNumberOfLoops() {
        return numberOfLoops;
    }

    public void setNumberOfLoops(int numberOfLoops) {
        this.numberOfLoops = numberOfLoops;
    }

    public String getJmxFile() {
        return jmxFile;
    }

    public void setJmxFile(String jmxFile) {
        this.lastModified = System.currentTimeMillis();
        this.jmxFile = jmxFile;
    }

    public String getCsvFiles() {
        return csvFiles;
    }

    public void setCsvFiles(String csvFiles) {
        this.csvFiles = csvFiles;
    }

    public List<String> getCsvFilesAsList() {

        if (csvFiles == null || "".equals(csvFiles.trim())) return Collections.emptyList();
        return Arrays.asList(this.csvFiles.split(","));
    }

    public void addCsvFiles(String newCsvFile) {

        final HashSet<String> files = new HashSet<>();
        for (String file : getCsvFilesAsList()) {
            files.add(file);
        }
        files.add(newCsvFile);
        csvFiles = null;
        for (String file : files) {
            if (csvFiles == null)
                csvFiles = file;
            else
                csvFiles += "," + file;
        }
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public long getInitialDelayMilliseconds() {
        return initialDelayMilliseconds;
    }

    public void setInitialDelayMilliseconds(long initialDelayMilliseconds) {
        this.initialDelayMilliseconds = initialDelayMilliseconds;
    }

    public long getWaitIntervalMilliseconds() {
        return waitIntervalMilliseconds;
    }

    public void setWaitIntervalMilliseconds(long waitIntervalMilliseconds) {
        this.waitIntervalMilliseconds = waitIntervalMilliseconds;
    }

    public long getPollIntervalMilliseconds() {
        return pollIntervalMilliseconds;
    }

    public void setPollIntervalMilliseconds(long pollIntervalMilliseconds) {
        this.pollIntervalMilliseconds = pollIntervalMilliseconds;
    }

    public int getSummariserInterval() {
        return summariserInterval;
    }

    public void setSummariserInterval(int summariserInterval) {
        this.summariserInterval = summariserInterval;
    }

    public int getUiLimitSummaries() {
        return uiLimitSummaries;
    }

    public void setUiLimitSummaries(int uiLimitSummaries) {
        this.uiLimitSummaries = uiLimitSummaries;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    public String getTargetProtocol() {
        return targetProtocol;
    }

    public void setTargetProtocol(String targetProtocol) {
        this.targetProtocol = targetProtocol;
    }

    public String getTargetPathPrefix() {
        return targetPathPrefix;
    }

    public void setTargetPathPrefix(String targetPathPrefix) {
        this.targetPathPrefix = targetPathPrefix;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTargetUrl() {

        return this.targetProtocol + "://" + this.targetHost + ":" + this.targetPort + (this.targetPathPrefix != null ? this.targetPathPrefix : "");
    }

    public LoadClientConfiguration set(LoadClientConfiguration newConfiguration) {

        this.lastModified = System.currentTimeMillis();
        this.setTargetHost(newConfiguration.getTargetHost());
        this.setTargetPort(newConfiguration.getTargetPort());
        this.setTargetProtocol(newConfiguration.getTargetProtocol());
        this.setNumberOfThreads(newConfiguration.getNumberOfThreads());
        this.setRunsInOneLoop(newConfiguration.getRunsInOneLoop());
        this.setWaitIntervalMilliseconds(newConfiguration.getWaitIntervalMilliseconds());
        this.setLogFilesEnabled(newConfiguration.isLogFilesEnabled());
        this.setJmxFile(newConfiguration.getJmxFile());
        this.setCsvFiles(newConfiguration.getCsvFiles());
        return this;
    }

    @Override
    public String toString() {
        return "LoadClientConfiguration{" +
                "showConfigOnStartup=" + showConfigOnStartup +
                ", startRunning=" + startRunning +
                ", redisEnabled=" + redisEnabled +
                ", redisPurgeOnStart=" + redisPurgeOnStart +
                ", logOutputEnabled=" + logOutputEnabled +
                ", logFilesEnabled=" + logFilesEnabled +
                ", numberOfThreads=" + numberOfThreads +
                ", runsInOneLoop=" + runsInOneLoop +
                ", numberOfLoops=" + numberOfLoops +
                ", jmxFile='" + jmxFile + '\'' +
                ", csvFiles='" + csvFiles + '\'' +
                ", logPath='" + logPath + '\'' +
                ", initialDelayMilliseconds=" + initialDelayMilliseconds +
                ", waitIntervalMilliseconds=" + waitIntervalMilliseconds +
                ", pollIntervalMilliseconds=" + pollIntervalMilliseconds +
                ", summariserInterval=" + summariserInterval +
                ", uiLimitSummaries=" + uiLimitSummaries +
                ", targetHost='" + targetHost + '\'' +
                ", targetPort=" + targetPort +
                ", targetProtocol='" + targetProtocol + '\'' +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }
}
