package de.datev.loadtest.client.result;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class LogCollector {

    private List<File> logFiles = new ArrayList<>();

    public List<File> getLogFiles() {
        return logFiles;
    }

    public void addLogFile(File logFile) {
        this.logFiles.add(logFile);
    }
}
