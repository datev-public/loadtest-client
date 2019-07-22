package de.datev.loadtest.client.result;

import java.io.File;

public class LogFile {

    String path;
    String name;
    String url;
    String byteSize;

    public LogFile(File file) {

        this.path = file.getPath();
        this.name = file.getName();
        this.url = "../../api/log-files/" + this.name;
        this.byteSize = file.length() + " Bytes";
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getByteSize() {
        return byteSize;
    }
}
