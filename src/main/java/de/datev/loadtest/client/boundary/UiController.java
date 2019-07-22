package de.datev.loadtest.client.boundary;

import de.datev.loadtest.client.JMeterRunner;
import de.datev.loadtest.client.LoadTestClientApplication;
import de.datev.loadtest.client.config.LoadClientConfiguration;
import de.datev.loadtest.client.control.ResultService;
import de.datev.loadtest.client.control.SettingsService;
import de.datev.loadtest.client.entity.LogEvent;
import de.datev.loadtest.client.result.LogFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Controller
@RequestMapping("/ui")
public class UiController {

    private final Logger log = LoggerFactory.getLogger(UiController.class);
    private static DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JMeterRunner jmeterRunner;
    private LoadClientConfiguration loadClientConfiguration;
    private LoadTestClientApplication loadTestClientApplication;
    private ResultService resultService;
    private SettingsService settingsService;
    private BuildProperties buildProperties;

    @Autowired
    public UiController(JMeterRunner jmeterRunner, LoadClientConfiguration loadClientConfiguration,
                        LoadTestClientApplication loadTestClientApplication, ResultService resultService,
                        SettingsService settingsService, BuildProperties buildProperties) {
        this.jmeterRunner = jmeterRunner;
        this.loadClientConfiguration = loadClientConfiguration;
        this.loadTestClientApplication = loadTestClientApplication;
        this.resultService = resultService;
        this.settingsService = settingsService;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/results")
    public String results(@RequestParam(name = "running", required = false, defaultValue = "-") String running,
                          @RequestParam(name = "purge", required = false, defaultValue = "false") boolean purge,
                          @RequestParam(name = "deleteLogs", required = false, defaultValue = "false") boolean deleteLogs,
                          @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
                          Model model) {

        log.info("results running={}, purge={}, all={}", running, purge, all);

        if (purge) {
            log.info("results purge database!");
            this.resultService.purge();
        }

        if (deleteLogs) {
            log.info("results delete logs!");
            this.jmeterRunner.deleteLogFiles();
        }

        if (!"-".equals(running)) {
            final boolean value = "true".equals(running);
            this.setRunning(value);
        }

        model.addAttribute("buildProperties", this.buildProperties);
        model.addAttribute("instanceIndex", this.loadTestClientApplication.getCfInstanceIndex());
        model.addAttribute("running", this.isRunning());
        model.addAttribute("now", ZonedDateTime.now(ZoneId.of(loadClientConfiguration.getTimeZone())).format(Formatter));

        model.addAttribute("configuration", this.loadClientConfiguration);
        model.addAttribute("loopIndex", this.loadTestClientApplication.getLoopIndex());
        model.addAttribute("totalNumberOfRequests", this.loadTestClientApplication.getTotalNumberOfRequests());

        if (this.loadClientConfiguration.isLogFilesEnabled()) {
            List<LogFile> logFiles = this.jmeterRunner.getLogFiles();
            model.addAttribute("logFiles", logFiles);
        }

        if (this.loadClientConfiguration.isRedisEnabled()) {

            List<LogEvent> allSummaries = resultService.listAllSummaries();
            final int totalSummaries =  allSummaries.size();
            final int shownSummaries = Math.min(loadClientConfiguration.getUiLimitSummaries(), totalSummaries);

            Set<LogEvent> latestOfAllInstances = new HashSet<>();
            allSummaries.stream()
                .collect(groupingBy(LogEvent::getInstance, Collectors.toSet()))
                .values().forEach(set -> {
                    Optional<LogEvent> latest = set.stream().max(Comparator.comparing(LogEvent::getPublishTime));
                latest.ifPresent(latestOfAllInstances::add);
            });
            Optional<LogEvent> summaryOfEvents = latestOfAllInstances.stream().reduce(this::aggregate);
            model.addAttribute("summary", summaryOfEvents.orElse(new LogEvent()));

            if (all) {
                model.addAttribute("summaries", allSummaries);
                model.addAttribute("shownSummaries", allSummaries.size());
                model.addAttribute("totalSummaries", allSummaries.size());
            } else {
                List<LogEvent> newestSummaries = resultService.listLatestSummaries(shownSummaries);
                model.addAttribute("summaries", newestSummaries);
                model.addAttribute("shownSummaries", shownSummaries);
                model.addAttribute("totalSummaries", totalSummaries);
            }
        }

        return "index";
    }

    @GetMapping("/properties")
    public String startEditing(Model model) {

        log.info("startEditing model={}", model);
        model.addAttribute("instanceIndex", this.loadTestClientApplication.getCfInstanceIndex());
        model.addAttribute("configuration", this.loadClientConfiguration);
        return "edit";
    }

    @PostMapping("/properties")
    public String storeEdited(@ModelAttribute LoadClientConfiguration newConfiguration, Model model) {

        log.info("storeEdited - - - newConfiguration     = {}", newConfiguration);
        try {
            this.setConfiguration(newConfiguration);
        }
        finally {
            model.addAttribute("instanceIndex", this.loadTestClientApplication.getCfInstanceIndex());
            model.addAttribute("configuration", loadClientConfiguration);
            log.info("storeEdited - - - changedConfiguration = {}", loadClientConfiguration);
        }
        return "edit";
    }

    @PostMapping("/fileUpload")
    public String storeUploadedFiles(
            @RequestParam("jmxFile") MultipartFile jmxFile,
            @RequestParam("csvFile") MultipartFile csvFile,
            Model model) {

        log.info("storeUploadedFiles - - - jmxFile = {}, csvFile = {}",
                jmxFile != null ? jmxFile.getOriginalFilename() : "null",
                csvFile != null ? csvFile.getOriginalFilename() : "null");

        if (jmxFile != null && !jmxFile.isEmpty()) {
            try {
                List<String> csvFilesInJmxFile = this.storeJmxFile(jmxFile);
                model.addAttribute("successJmx", true);
                if (!csvFilesInJmxFile.isEmpty()) {
                    model.addAttribute("csvFilesInJmxFile", csvFilesInJmxFile);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("errorJmx", "ERROR processing JMX file: " + e.getMessage());
            }
        }

        if (csvFile != null && !csvFile.isEmpty()) {
            try {
                this.storeCsvFile(csvFile);
                model.addAttribute("successCsv", true);
            }
            catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("errorCsv", "ERROR processing CSV file: " + e.getMessage());
            }
        }

        model.addAttribute("instanceIndex", this.loadTestClientApplication.getCfInstanceIndex());
        model.addAttribute("configuration", loadClientConfiguration);
        log.info("storeNewJmxfile - - - changedConfiguration = {}", loadClientConfiguration);
        return "edit";
    }

    //------------------------------------------------------------------------------------------------------------------

    private LogEvent aggregate(LogEvent newEvent, LogEvent summarizedEvent) {

        LogEvent ret = new LogEvent();
        long totalNumSamples = newEvent.getNumSamples() + summarizedEvent.getNumSamples();
        long totalErrorCount = newEvent.getErrorCount() + summarizedEvent.getErrorCount();
        ret.setNumSamples(totalNumSamples);
        ret.setErrorCount(totalErrorCount);
        if (totalNumSamples > 0) {
            ret.setErrorPercentage(totalErrorCount / totalNumSamples);
            ret.setErrorPercentageString(String.format("%.2f%%", ret.getErrorPercentage() * 100));
            ret.setAverage(newEvent.getAverage() * newEvent.getNumSamples() / totalNumSamples
                    + summarizedEvent.getAverage() * summarizedEvent.getNumSamples() / totalNumSamples);
        }
        ret.setMin(Math.min(newEvent.getMin(), summarizedEvent.getMin()));
        ret.setMax(Math.max(newEvent.getMax(), summarizedEvent.getMax()));
        ret.setRate(newEvent.getRate() + summarizedEvent.getRate());
        ret.setElapsed(newEvent.getElapsed() + summarizedEvent.getElapsed());
        return ret;
    }

    private boolean isRunning() {

        if (loadClientConfiguration.isRedisEnabled()) {
            return settingsService.isRunning();
        } else {
            return jmeterRunner.isRunning();
        }
    }

    private void setRunning(boolean value) {

        log.info("setRunning value={}", value);
        if (loadClientConfiguration.isRedisEnabled()) {
            jmeterRunner.setRunning(value);
            settingsService.setRunning(value);
        } else {
            jmeterRunner.setRunning(value);
        }
    }

    private void setConfiguration(LoadClientConfiguration newConfiguration) {

        log.info("setConfiguration newConfiguration={}", newConfiguration);
        newConfiguration.setJmxFile(loadClientConfiguration.getJmxFile());
        loadClientConfiguration.set(newConfiguration);
        if (loadClientConfiguration.isRedisEnabled()) {
            settingsService.storeSettings(loadClientConfiguration);
        }
    }

    private List<String> storeJmxFile(MultipartFile jmxFile) throws IOException {

        log.info("storeJmxFile jmxFile={}", jmxFile);
        final String fileName = "jmx/" + jmxFile.getOriginalFilename();

        loadClientConfiguration.setJmxFile(fileName);

        List<String> csvFilesInJmxFile;
        try (InputStream in = jmxFile.getInputStream()) {
            csvFilesInJmxFile = jmeterRunner.setAndStoreJmxFile(fileName, in);
        }
        // Publish to other instances via Redis, if enabled
        if (loadClientConfiguration.isRedisEnabled()) {
            // Store the changed file name of the JMX in the settings
            settingsService.storeSettings(loadClientConfiguration);
            // Store the content
            settingsService.storeJmxFileToRedis(jmeterRunner.getFilePath(fileName));
        }

        return csvFilesInJmxFile;
    }

    private void storeCsvFile(MultipartFile csvFile) throws IOException {

        final String fileName = "jmx/" + csvFile.getOriginalFilename();
        log.info("storeCsvFile csvFile={}", fileName);

        loadClientConfiguration.addCsvFiles(fileName);
        log.info("storeCsvFile config.csvFiles={}", loadClientConfiguration.getCsvFiles());
        try (InputStream in = csvFile.getInputStream()) {
            jmeterRunner.storeSupportFile(fileName, in);
        }
        // Publish to other instances via Redis, if enabled
        if (loadClientConfiguration.isRedisEnabled()) {
            /// Store the changed file names (list!( of the CSV files in the settings
            settingsService.storeSettings(loadClientConfiguration);
            // Store the content
            settingsService.storeCsvFileToRedis(jmeterRunner.getFilePath(fileName));
        }
    }
}
