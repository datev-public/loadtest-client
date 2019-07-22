package de.datev.loadtest.client.control;

import de.datev.loadtest.client.config.LoadClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private static final String KEY_SETTINGS = "settings";
    private static final String KEY_JMX_FILE = "jmxfile";
    private static final String KEY_CSV_FILES = "csvfiles";

    @Autowired
    LoadClientConfiguration loadClientConfiguration;

    @Autowired
    private RedisTemplate<String, LoadClientConfiguration> redisTemplate;

    // inject the template as ValueOperations
    @Resource(name = "redisTemplate")
    private ValueOperations<String, LoadClientConfiguration> valueOpsForSettings;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOpsForJmxFile;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, HashMap<String,String>> valueOpsForCsvFiles;

    public void purge() {

        log.info("ResultService.purge");
        redisTemplate.delete(KEY_SETTINGS);
        redisTemplate.delete(KEY_JMX_FILE);
        redisTemplate.delete(KEY_CSV_FILES);
    }

    public void storeSettings(LoadClientConfiguration configuration) {

        log.debug("storeSettings {}", configuration);
        valueOpsForSettings.set(KEY_SETTINGS, configuration);
    }

    public LoadClientConfiguration retrieveSettings() {

        final LoadClientConfiguration configuration = valueOpsForSettings.get(KEY_SETTINGS);
        log.debug("retrieveSettings = {}", configuration);
        return configuration;
    }

    public boolean storeJmxFileToRedis(File jmxFile) {

        try {
            byte[] bytes = Files.readAllBytes(jmxFile.toPath());
            log.info("storeJmxFileToRedis {} with {} bytes", jmxFile, bytes.length);
            valueOpsForJmxFile.set(KEY_JMX_FILE, new String(bytes, Charset.defaultCharset()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean storeCsvFileToRedis(File csvFile) {

        HashMap<String,String> csvFiles = valueOpsForCsvFiles.get(KEY_CSV_FILES);
        if (csvFiles == null) {
            csvFiles = new HashMap<>();
        }
        try {
            byte[] bytes = Files.readAllBytes(csvFile.toPath());
            String content =  new String(bytes, Charset.defaultCharset());
            csvFiles.put(csvFile.getName(), content);
            log.info("storeCsvFileToRedis {} with {} bytes", csvFile, bytes.length);
            valueOpsForCsvFiles.set(KEY_CSV_FILES, csvFiles);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadJmxFileFromRedisToFileSystem(File jmxFile) {

        log.info("loadJmxFileFromRedisToFileSystem {}", jmxFile);
        final String content = valueOpsForJmxFile.get(KEY_JMX_FILE);
        if (content != null) {
            try {
                final byte[] bytes = content.getBytes(Charset.defaultCharset());
                log.info("loadJmxFileFromRedisToFileSystem {} with {} bytes", jmxFile, bytes.length);
                Files.write(jmxFile.toPath(), bytes);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public void setRunning(boolean value) {

        log.info("setRunning {}", value);
        LoadClientConfiguration configuration = valueOpsForSettings.get(KEY_SETTINGS);
        if (configuration == null) {
            configuration = this.loadClientConfiguration;
        }
        configuration.setStartRunning(value);
        valueOpsForSettings.set(KEY_SETTINGS, configuration);
    }

    public boolean isRunning() {

        LoadClientConfiguration configuration = valueOpsForSettings.get(KEY_SETTINGS);
        return configuration != null && configuration.isStartRunning();
    }
}
