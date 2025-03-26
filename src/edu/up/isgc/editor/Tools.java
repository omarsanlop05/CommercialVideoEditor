package edu.up.isgc.editor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Tools {
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

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mov", ".mkv", ".flv"};
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
    private static final String OUTPUT_FORMAT = "mp4";
    private static final String VIDEO_CODEC = "libx264";
    private static final String AUDIO_CODEC = "aac";
    private static final int IMAGE_DURATION = 3;

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
                    "cmd.exe", "/c",
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
            }
        }
        System.err.println("Could not parse date: " + dateStr);
        return null;
    }

    public File joinFiles(List<File> inputFiles) {
        List<File> tempFiles = new ArrayList<>();
        File outputFile = null;

        try {
            for (File file : inputFiles) {
                if (isVideo(file)) {
                    File processed = processVideo(file);
                    if (processed != null) tempFiles.add(processed);
                } else if (isImage(file)) {
                    File videoFromImage = convertImageToVideo(file);
                    if (videoFromImage != null) tempFiles.add(videoFromImage);
                }
            }

            File dir = new File("output-files");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            if (!tempFiles.isEmpty()) {
                outputFile = new File(dir,"outputVideo." + OUTPUT_FORMAT);
                concatenateVideos(tempFiles, outputFile);
            }

        } catch (Exception e) {
            System.err.println("Error al unir archivos: " + e.getMessage());
        } finally {
            tempFiles.forEach(temp -> {
                if (temp.exists()) temp.delete();
            });
        }

        return outputFile;
    }

    private boolean isVideo(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private File processVideo(File input) {
        try {
            System.out.println("Processing video: " + input.getName());
            File output = File.createTempFile("video_", "." + OUTPUT_FORMAT);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", input.getAbsolutePath(),
                    "-c:v", VIDEO_CODEC,
                    "-c:a", AUDIO_CODEC,
                    "-preset", "fast",
                    "-movflags", "+faststart",
                    output.getAbsolutePath()
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while (reader.readLine() != null) {
                System.out.println(reader.readLine());
            }

            System.out.println("Video processing completed.");
            return output;

        } catch (Exception e) {
            System.err.println("Error procesando video: " + input.getName());
            return null;
        }
    }

    private File convertImageToVideo(File image) {
        try {
            System.out.println("Converting image: " + image.getName());
            File output = File.createTempFile("img_", "." + OUTPUT_FORMAT);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-loop", "1",
                    "-i", image.getAbsolutePath(),
                    "-c:v", VIDEO_CODEC,
                    "-t", String.valueOf(IMAGE_DURATION),
                    "-vf", "scale='iw-mod(iw,2)':'ih-mod(ih,2)',format=yuv420p",
                    "-preset", "fast",
                    output.getAbsolutePath()
            );

            pb.redirectErrorStream(true); // Añade esto antes de iniciar el proceso
            Process process = pb.start();
            System.out.println("Process started");
            //BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            System.out.println("reader started.");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line); // Muestra la salida de FFmpeg
                }
            }
            System.out.println("reader finished.");

            System.out.println("Waiting for process to finish...");
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg falló con código: " + exitCode);
            }

            System.out.println("Image processing completed.");
            return output;

        } catch (Exception e) {
            System.err.println("Error convirtiendo imagen " + image.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static void concatenateVideos(List<File> videos, File output) {
        try {
            System.out.println("Concatenating videos into " + output.getName());
            File listFile = File.createTempFile("list_", ".txt");
            Files.write(listFile.toPath(),
                    videos.stream()
                            .map(f -> String.format("file '%s'", f.getAbsolutePath()))
                            .collect(java.util.stream.Collectors.toList())
            );

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", listFile.getAbsolutePath(),
                    "-c", "copy",
                    output.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg falló con código: " + exitCode);
            }

            Files.delete(listFile.toPath());
            System.out.println("Videos concatenated into " + output.getName());

        } catch (Exception e) {
            System.err.println("Error al concatenar videos: " + e.getMessage());
        }
    }
}