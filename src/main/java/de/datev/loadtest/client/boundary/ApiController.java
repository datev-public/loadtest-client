package de.datev.loadtest.client.boundary;

import de.datev.loadtest.client.JMeterRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final Logger log = LoggerFactory.getLogger(ApiController.class);

    private JMeterRunner jmeterRunner;

    @Autowired
    public ApiController(JMeterRunner jmeterRunner) {
        this.jmeterRunner = jmeterRunner;
    }

    @GetMapping(path = "/log-files/{fileName}")
    public void downloadLogFile(@PathVariable String fileName, HttpServletResponse response) {

        if (!fileName.endsWith(".csv")) {
            fileName += ".csv";
        }

        log.debug("downloadLogFile {}", fileName);

        try {
            response.setHeader("Content-Type", "text/csv");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            OutputStream out = response.getOutputStream();
            Files.copy(new File(jmeterRunner.getLogDir(), fileName).toPath(), out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping(path = "jmx-files/{fileName}")
    public void downloadJmxFile(@PathVariable String fileName, HttpServletResponse response) {

        log.debug("downloadJmxFile {}", fileName);
        // TODO: fix
        fileName = fileName.replace("..", "/") + ".jmx";

        try {
            response.setHeader("Content-Type", "text/xml");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            OutputStream out = response.getOutputStream();
            Files.copy(jmeterRunner.getFilePath(fileName).toPath(), out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
