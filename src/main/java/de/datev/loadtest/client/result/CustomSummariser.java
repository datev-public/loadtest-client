package de.datev.loadtest.client.result;

import de.datev.loadtest.client.config.LoadClientConfiguration;
import de.datev.loadtest.client.control.ResultService;
import de.datev.loadtest.client.entity.LogEvent;
import org.apache.jmeter.reporters.Summariser2;
import org.apache.jmeter.reporters.SummariserRunningSample2;
import org.apache.jmeter.threads.JMeterContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class CustomSummariser extends Summariser2 {

    @Autowired
    private LoadClientConfiguration configuration;
    @Autowired
    private ResultService resultService;

    private String instance;
    private String instanceId;

    private int activeThreads = 0;
    private int startedThreads = 0;
    private int finishedThreads = 0;

    public CustomSummariser() {
        super("summariser");
        this.instance = ManagementFactory.getRuntimeMXBean().getName();
        final String cfInstanceIndex = System.getenv("CF_INSTANCE_INDEX");
        this.instanceId =  cfInstanceIndex != null && cfInstanceIndex.trim().length() > 0 ? cfInstanceIndex : "?";
    }

    @Override
    protected void formatAndWriteToLog(String name, SummariserRunningSample2 summariserRunningSample, String type) {

        final String threadName = Thread.currentThread().getName();
        if (configuration.isLogOutputEnabled()) {
            super.formatAndWriteToLog("INSTANCE-" + this.instanceId + " " + threadName, summariserRunningSample, type);
        }
        if (!configuration.isRedisEnabled()) return;

        JMeterContextService.ThreadCounts tc = JMeterContextService.getThreadCounts();
        if ("=".equals(type)) {
            final ZonedDateTime publishTime = ZonedDateTime.now(ZoneId.of(configuration.getTimeZone()));
            final LogEvent logEvent = LogEvent.build(instance, instanceId, threadName, configuration.getJmxFile(),
                    type, publishTime, summariserRunningSample, activeThreads, startedThreads, finishedThreads);
            resultService.publishSummary(logEvent);
        } else {
            activeThreads = tc.activeThreads;
            startedThreads = tc.startedThreads;
            finishedThreads = tc.finishedThreads;
        }
    }
}
