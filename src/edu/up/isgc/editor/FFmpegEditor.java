package edu.up.isgc.editor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class FFmpegEditor {

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mov", ".mkv", ".flv"};
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
    private static final String OUTPUT_FORMAT = "mp4";
    private static final String VIDEO_CODEC = "libx264";
    private static final String AUDIO_CODEC = "aac";
    private static final int IMAGE_DURATION = 5;

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
                outputFile = new File(dir,"outputVideo" + System.currentTimeMillis() + "." + OUTPUT_FORMAT);
                concatenateVideos(tempFiles, outputFile);
            }

        } catch (Exception e) {
            System.err.println("Error joining videos: " + e.getMessage());
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
            String videoName = "video_" + System.currentTimeMillis();
            System.out.println("Processing video: " + input.getName());

            File dir = new File("output-files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File output = new File(dir,videoName + "_reprocessedVideo." + OUTPUT_FORMAT);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", input.getAbsolutePath(),
                    "-c:v", VIDEO_CODEC,
                    "-an",
                    "-pix_fmt", "yuv420p",
                    "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(1920-iw)/2:(1080-ih)/2",
                    "-r", "30",
                    output.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg: " + line);
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
            String imageName = "video_" + System.currentTimeMillis();

            System.out.println("Converting image: " + image.getName());

            File dir = new File("output-files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File output = new File(dir,imageName + "_videoImg." + OUTPUT_FORMAT);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-loop", "1",
                    "-i", image.getAbsolutePath(),
                    "-c:v", VIDEO_CODEC,
                    "-t", String.valueOf(IMAGE_DURATION),
                    "-pix_fmt", "yuv420p",
                    "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(1920-iw)/2:(1080-ih)/2",
                    "-r", "30",
                    output.getAbsolutePath()
            );

            System.out.println("Output file path: " + output.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();


            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg failed with code: " + exitCode);
            }

            return output;

        } catch (Exception e) {
            System.err.println("Error converting image " + image.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void concatenateVideos(List<File> videos, File output) {
        try {
            System.out.println("Concatenating videos into " + output.getName());
            File dir = new File("output-files");

            if (!dir.exists()) {
                dir.mkdirs();
            }

            File collage = createCollage(videos);
            videos.add(collage);

            File listFile = new File(dir, "list" + System.currentTimeMillis() + ".txt");
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
                throw new IOException("FFmpeg failed with code: " + exitCode);
            }

            Files.delete(listFile.toPath());
            System.out.println("Videos concatenated into " + output.getName());

        } catch (Exception e) {
            System.err.println("Error concatenating videos: " + e.getMessage());
        }
    }

    public File createCollage(List<File> inputFiles) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            System.out.println("No hay archivos para el collage");
            return null;
        }

        File dir = new File("output-files");
        if (!dir.exists()){
            dir.mkdirs();
        }

        List<File> processedFiles = new ArrayList<>();
        try {
            for (int i = 0; i < inputFiles.size(); i++) {
                File input = inputFiles.get(i);
                File processed = processFileForCollage(input, i);
                if (processed != null) {
                    processedFiles.add(processed);
                }
            }

            if (processedFiles.isEmpty()) {
                System.out.println("Files couldn't be processed");
                return null;
            }

            File collage = new File(dir, "collage" + System.currentTimeMillis() + ".mp4");
            createXStackCollage(processedFiles, collage);

            System.out.println("WARNING: ERASING COLLAGE TEMP FILES");
            for (File file : processedFiles) {
                file.delete();
            }

            return collage;

        } catch (Exception e) {
            System.err.println("Error al crear el collage: " + e.getMessage());
            return null;
        }
    }

    private File processFileForCollage(File input, int index) {
        try {
            File output = new File("output-files", "temp" + index + ".mp4");

            // FFmpeg command to adjust duration and format
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(input.getAbsolutePath());

            // Si es video, cortarlo o hacer loop
            if (isVideo(input)) {
                command.add("-t");
                command.add("5");
            }
            // Si es imagen, convertirla a video de 5 segundos
            else if (isImage(input)) {
                command.add("-loop");
                command.add("1");
                command.add("-t");
                command.add("5");
            }

            // Configuración para mantener aspecto y rellenar con negro
            command.add("-vf");
            command.add("scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(1920-iw)/2:(1080-ih)/2");
            command.add("-c:v");
            command.add(VIDEO_CODEC);
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-r");
            command.add("30");
            command.add(output.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Mostrar salida de FFmpeg para depuración
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg failed with code: " + exitCode);
                return null;
            }

            return output;
        } catch (Exception e) {
            System.err.println("Error processing file for collage: " + e.getMessage());
        }
        return null;
    }

    private void createXStackCollage(List<File> files, File output) {
        try {
            System.out.println("Building XStack command");
            int numFiles = files.size();

            int rows = (int) Math.ceil(Math.sqrt(numFiles));
            int cols = (int) Math.ceil((double) numFiles / rows);

            int totalWidth = 1920;
            int totalHeight = 1080;

            int cellWidth = totalWidth / cols;
            int cellHeight = totalHeight / rows;

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");

            for (File file : files) {
                command.add("-i");
                command.add(file.getAbsolutePath());
            }

            StringBuilder filter = new StringBuilder();

            for (int i = 0; i < numFiles; i++) {
                filter.append(String.format(
                        "[%d:v]scale='if(gt(a,%d/%d),%d,-1)':'if(gt(a,%d/%d),-1,%d)',",
                        i, cellWidth, cellHeight, cellWidth, cellWidth, cellHeight, cellHeight
                ));
                filter.append(String.format(
                        "pad=%d:%d:(ow-iw)/2:(oh-ih)/2[x%d];",
                        cellWidth, cellHeight, i
                ));
            }

            for (int i = 0; i < numFiles; i++) {
                filter.append(String.format("[x%d]", i));
            }

            filter.append("xstack=inputs=").append(numFiles).append(":layout=");

            for (int i = 0; i < numFiles; i++) {
                int row = i / cols;
                int col = i % cols;
                filter.append(String.format("%d_%d", col * cellWidth, row * cellHeight));
                if (i < numFiles - 1) {
                    filter.append("|");
                }
            }

            filter.append("[out]");

            StringBuilder inputLabels = new StringBuilder();
            for (int i = 0; i < numFiles; i++) {
                inputLabels.append("[v").append(i).append("]");
            }

            command.add("-filter_complex");
            command.add(filter.toString());
            command.add("-map");
            command.add("[out]");
            command.add("-c:v");
            command.add("libx264");
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-r");
            command.add("30");
            command.add("-t");
            command.add("5");
            command.add("-crf");
            command.add("23");
            command.add("-preset");
            command.add("fast");
            command.add(output.getAbsolutePath());

            System.out.println("FFmpeg command:");
            System.out.println(String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer la salida de FFmpeg para depuración
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg falló con código: " + exitCode);
            }

            System.out.println("Collage creado exitosamente: " + output.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("FFmpeg falló al crear el collage: " + e.getMessage());
        }
    }


    private String imageBase64(File image) {
        try {
            Path path = Path.of(image.getAbsolutePath());
            byte[] fileContent = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
