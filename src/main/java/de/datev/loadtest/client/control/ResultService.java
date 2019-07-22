package de.datev.loadtest.client.control;

import de.datev.loadtest.client.entity.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);

    private static final String KEY_EVENTS = "summary-events";

    @Autowired
    private RedisTemplate<String, LogEvent> redisTemplate;

    // inject the template as ListOperations
    @Resource(name = "redisTemplate")
    private ListOperations<String, LogEvent> listOpsForEvents;

    public void purge() {

        log.info("purge");
        redisTemplate.delete(KEY_EVENTS);
    }

    public void publishSummary(LogEvent logEvent) {

        listOpsForEvents.leftPush(KEY_EVENTS, logEvent);
    }

    public List<LogEvent> listAllSummaries() {

        if (redisTemplate.hasKey(KEY_EVENTS)) {
            final long end = redisTemplate.opsForList().size(KEY_EVENTS);
            final List<LogEvent> summaries = listOpsForEvents.range(KEY_EVENTS, 0, end);
            return summaries;
        } else {
            return Collections.emptyList();
        }
    }

    public List<LogEvent> listLatestSummaries(int limit) {

        if (redisTemplate.hasKey(KEY_EVENTS)) {
            //listOpsForEvents.trim(KEY_EVENTS, 0, limit);
            //final long end = redisTemplate.opsForList().size(KEY_EVENTS);
            try {
                return listOpsForEvents.range(KEY_EVENTS, 0, limit);
            } catch (Exception ice) { // java.io.InvalidClassException
                ice.printStackTrace();
                this.purge();
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }
}
