package edu.up.isgc.editor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FileManager {
    private List<File> inputFiles;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    };

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^(Create|Date|Modif|File).*Date.*?: (\\d{4}[-/:]\\d{2}[-/:]\\d{2} \\d{2}:\\d{2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    public List<File> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(List<File> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public void arrangeFiles() {
        if (inputFiles == null || inputFiles.isEmpty()) {
            System.out.println("No files to arrange");
            return;
        }

        System.out.println("=== Before arrange ===");
        inputFiles.forEach(file ->
                System.out.println(file.getName() + " - " + getCreationDateTime(file))
        );

        // Sort in descending order (most recent first)
        inputFiles.sort(Comparator.comparing(
                this::getCreationDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        System.out.println("\n=== After arrange ===");
        inputFiles.forEach(file ->
                System.out.println(file.getName() + " - " + getCreationDateTime(file))
        );
    }

    private LocalDateTime getCreationDateTime(File file) {
        try {
            String exifToolPath = new File("exiftool-13.25_64/exiftool.exe").getAbsolutePath();

            ProcessBuilder builder = new ProcessBuilder(
                    exifToolPath,
                    "-time:all",
                    file.getAbsolutePath()
            );

            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            LocalDateTime oldestDate = null;
            String line;

            while ((line = reader.readLine()) != null) {
                var matcher = DATE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String dateStr = matcher.group(2);
                    LocalDateTime date = parseDateTime(dateStr);
                    if (date != null && (oldestDate == null || date.isBefore(oldestDate))) {
                        oldestDate = date;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Exiftool exited with code: " + exitCode);
            }

            return oldestDate;

        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing file: " + file.getName());
            e.printStackTrace();
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(dateStr, format);
            } catch (DateTimeParseException e) {
                if(!Objects.equals(dateStr, "0000:00:00 00:00:00"))
                    System.err.println("Could not parse date: " + dateStr);
            }
        }
        return null;
    }


}