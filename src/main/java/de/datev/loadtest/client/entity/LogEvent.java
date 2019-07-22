package de.datev.loadtest.client.entity;

import org.apache.jmeter.reporters.SummariserRunningSample2;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

//@RedisHash("jmeter-summaries") - only when using Repository
public class LogEvent implements Serializable { // Serializable because it is stored in REDIS

    private static final long serialVersionUID = 1L;

    private static DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //@Id - only when using Repository
    //private String id;

    private ZonedDateTime publishTime;
    private String instance;
    private String instanceIndex;
    private String thread;
    private String type;
    private long numSamples;
    private long elapsed;
    private double rate;
    private double min;
    private double max;
    private double average;
    private long errorCount;
    private String errorPercentageString;
    private double errorPercentage;
    private String jmxFile;
    private int activeThreads;
    private int startedThreads;
    private int finishedThreads;

    public static LogEvent build(String instance, String instanceIndex, String thread,  String jmxFile,
                                 String type, ZonedDateTime publishTime, SummariserRunningSample2 summary,
                                 int activeThreads, int startedThreads, int finishedThreads) {
        LogEvent ret = new LogEvent();
        ret.publishTime = publishTime;
        ret.instance = instance;
        ret.instanceIndex = instanceIndex;
        ret.thread = thread;
        ret.jmxFile = jmxFile;
        ret.type = type;
        ret.numSamples = summary.getNumSamples();
        ret.elapsed = summary.getElapsed();
        ret.rate = summary.getRate();
        ret.min = summary.getMin();
        ret.max = summary.getMax();
        ret.average = summary.getAverage();
        ret.errorCount = summary.getErrorCount();
        ret.errorPercentageString = summary.getErrorPercentageString();
        ret.errorPercentage = summary.getErrorPercentage();
        ret.activeThreads = activeThreads;
        ret.startedThreads = startedThreads;
        ret.finishedThreads = finishedThreads;
        return ret;
    }

    public ZonedDateTime getPublishTime() {
        return publishTime;
    }

    public String getPublishTimeString() {
        return publishTime.format(Formatter);
    }

    public String getInstance() {
        return instance;
    }

    public String getInstanceIndex() {
        return instanceIndex;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setInstanceIndex(String instanceIndex) {
        this.instanceIndex = instanceIndex;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getJmxFile() {
        return jmxFile;
    }

    public void setJmxFile(String jmxFile) {
        this.jmxFile = jmxFile;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getNumSamples() {
        return numSamples;
    }

    public void setNumSamples(long numSamples) {
        this.numSamples = numSamples;
    }

    public long getElapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorPercentageString() {
        return errorPercentageString;
    }

    public void setErrorPercentageString(String errorPercentageString) {
        this.errorPercentageString = errorPercentageString;
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public void setErrorPercentage(double errorPercentage) {
        this.errorPercentage = errorPercentage;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(int activeThreads) {
        this.activeThreads = activeThreads;
    }

    public int getStartedThreads() {
        return startedThreads;
    }

    public void setStartedThreads(int startedThreads) {
        this.startedThreads = startedThreads;
    }

    public int getFinishedThreads() {
        return finishedThreads;
    }

    public void setFinishedThreads(int finishedThreads) {
        this.finishedThreads = finishedThreads;
    }
}
