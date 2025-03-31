package edu.up.isgc.editor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//Video edition class
public class FFmpegEditor {

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mov", ".mkv", ".flv"};
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
    private static final String OUTPUT_FORMAT = "mp4";
    private static final String VIDEO_CODEC = "libx264";
    private static final String AUDIO_CODEC = "aac";
    private static final int IMAGE_DURATION = 3;

    //Main function - calls all the others to create the video
    public File joinFiles(List<File> inputFiles, String prompt) {
        //Create temp files list
        List<File> tempFiles = new ArrayList<>();
        List<File> tempImages = new ArrayList<>();
        List<File> tempAudios = new ArrayList<>();

        //ChatGPT calls variable
        ChatGPT chatGPT = new ChatGPT();

        //process files directory
        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //final output directory
        File finalDir = new File("final-output-video");
        if (!finalDir.exists()) {
            finalDir.mkdirs();
        }

        //intermediary files
        File outputFile = new File(finalDir,"outputVideo" + System.currentTimeMillis() + "." + OUTPUT_FORMAT);
        File joinedVideos = null;
        File finalAudio = null;
        File finalVideo = new File(dir,"finalVideo" + System.currentTimeMillis() + "." + OUTPUT_FORMAT);

        try {
            System.out.println("Creating temp files for base video.");
            //Reprocess videos and turn pictures into videos
            for (File file : inputFiles) {
                if (isVideo(file)) {
                    File processed = processVideo(file);
                    if (processed != null){
                        tempFiles.add(processed);
                        //Take frame for vision call
                        File frame = convertToImage(processed);
                        tempImages.add(frame);
                    }
                } else if (isImage(file)) {
                    File videoFromImage = convertImageToVideo(file);
                    if (videoFromImage != null){
                        tempFiles.add(videoFromImage);
                        //Take frame for vision call
                        File frame = convertToImage(videoFromImage);
                        tempImages.add(frame);
                    }
                }
            }

            //Joins given videos and pictures
            System.out.println("Joining main video");
            if (!tempFiles.isEmpty()) {
                joinedVideos = new File(dir,"JoinedVideo" + System.currentTimeMillis() + "." + OUTPUT_FORMAT);
                concatenateVideos(tempFiles, joinedVideos);
            }

            //Uses taken frame for vision and video for duration (calculate needed words)
            System.out.println("Creating temp audios.");
            for (int i=0; i<tempFiles.size(); i++) {
                File audio = chatGPT.ImageToAudio(tempImages.get(i), tempFiles.get(i));
                tempAudios.add(audio);
            }

            //Takes all audios and connects them into a single file
            System.out.println("Joining audio");
            if (!tempAudios.isEmpty()) {
                finalAudio = new File(dir,"finalAudio" + System.currentTimeMillis() + ".mp3");
                concatenateAudios(tempAudios, finalAudio);
            }

            //Generating postcards
            System.out.println("Generating postcards and collage");
            File postCardS = new File(dir, "postcardS_" + System.currentTimeMillis() + ".jpg");
            chatGPT.downloadImage(prompt, postCardS.getAbsolutePath());

            File postCardE = new File(dir, "postcardE_" + System.currentTimeMillis() + ".jpg");
            chatGPT.downloadImage(prompt, postCardE.getAbsolutePath());

            //Converts postcads and collage to videos
            File postCard1 = convertImageToVideo(postCardS);
            File collage = createCollage(inputFiles);
            File postCard2 = convertImageToVideo(postCardE);

            //Uses list to concatenate them
            List<File> finalVideos = new ArrayList<>();
            finalVideos.add(postCard1);
            finalVideos.add(joinedVideos);
            finalVideos.add(collage);
            finalVideos.add(postCard2);

            System.out.println("Joining video for output");
            concatenateVideos(finalVideos, finalVideo);

            //Add audio to the final video
            System.out.println("Adding audio to video");
            if(finalAudio != null){
                addAudioVideo(finalAudio, finalVideo, outputFile);
            } else {
                System.err.println("Error adding audio to video.");
            }

        } catch (Exception e) {
            System.err.println("Error joining videos: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Y asi nomás quedó plebes - El pirata de culiacán");
        }

        return outputFile;
    }

    //Check if file is video
    private boolean isVideo(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    //Check if file is image
    private boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    //Reprocess videos to have same format
    private File processVideo(File input) {
        try {
            //Creates video duplicate to work on (original file is unaffected)
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

            System.out.println("Video processing completed: " + input.getName());
            return output;

        } catch (Exception e) {
            System.err.println("Error processing video: " + input.getName());
            e.printStackTrace();
            return null;
        }
    }

    //Converts image to 3seconds video
    private File convertImageToVideo(File image) {
        try {
            //Uses same format as reprocessed videos to create the video out of images
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
                System.err.println("Error converting image: " + image.getName() + ": " + exitCode);
            }

            System.out.println("Image processed completed: " + image.getName());
            return output;

        } catch (Exception e) {
            System.err.println("Error converting image " + image.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //Concatenate videos
    private void concatenateVideos(List<File> videos, File output) {
        try {
            System.out.println("Concatenating videos into " + output.getName());
            File dir = new File("output-files");

            if (!dir.exists()) {
                dir.mkdirs();
            }

            //Build list of the files that will be concatenated
            File listFile = new File(dir, "list" + System.currentTimeMillis() + ".txt");
            Files.write(listFile.toPath(),
                    videos.stream()
                            .map(f -> String.format("file '%s'", f.getAbsolutePath()))
                            .collect(java.util.stream.Collectors.toList())
            );

            //Command
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", listFile.getAbsolutePath(),
                    "-c:v", VIDEO_CODEC,
                    "-c:a", AUDIO_CODEC,
                    "-strict", "experimental",
                    output.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg failed with code: " + exitCode);
            }

            //Once concatenated, deletes list file
            Files.delete(listFile.toPath());
            System.out.println("Videos concatenated into " + output.getName());

        } catch (Exception e) {
            System.err.println("Error concatenating videos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Calls functions necessary for collage
    private File createCollage(List<File> inputFiles) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            System.out.println("No hay archivos para el collage");
            return null;
        }

        File dir = new File("output-files");
        if (!dir.exists()){
            dir.mkdirs();
        }

        //Reprocess files to 5s videos
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

            //Crates xStack collage with reprocessed videos
            File collage = new File(dir, "collage" + System.currentTimeMillis() + ".mp4");
            createXStackCollage(processedFiles, collage);

            System.out.println("WARNING: ERASING COLLAGE TEMP FILES");
            for (File file : processedFiles) {
                file.delete();
            }

            System.out.println("Collage completed: " + collage.getName());
            return collage;

        } catch (Exception e) {
            System.err.println("Error creating collage: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //Cuts videos on the first 5 seconds to add to the collage
    private File processFileForCollage(File input, int index) {
        try {
            //Process files with same format, keeping their aspect ratio
            File output = new File("output-files", "temp" + index + ".mp4");

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(input.getAbsolutePath());

            if (isVideo(input)) {
                command.add("-t");
                command.add("5");
            }
            else if (isImage(input)) {
                command.add("-loop");
                command.add("1");
                command.add("-t");
                command.add("5");
            }

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
            e.printStackTrace();
        }
        return null;
    }

    //Uses xStack to create the collage given the cut videos
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

            //Calculate size of video to fit in the grid (depends on resolution and # of files)
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

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg failed with code: " + exitCode);
            } else{
                System.out.println("Collage created succesfully: " + output.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("FFmpeg failed creating collage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Call extractFirstFrame function (if it's a video)
    private File convertToImage(File file){
        System.out.println("Converting file: " + file.getAbsolutePath() + " to image");
        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outputFile = new File(dir, file.getName() + "_frame.jpg");

        //Extracts first frame (if it's a video)
        if (isVideo(file)) {
            extractFirstFrame(file, outputFile.getAbsolutePath());
            System.out.println("Video converted correctly: " + outputFile.getName());
            return outputFile;
        } else if (isImage(file)) {
            System.out.println("File was already image: " + file.getName());
            return file;
        }
        System.err.println("Could not convert file: " + file.getName() + " to image");
        return null;
    }

    //Extract first frame from video to make vision OpenAI call
    public void extractFirstFrame(File video, String outputFileName) {
        System.out.println("Extracting first frame from: " + video.getName());
        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //Uses thumbnail to avoid dark frames
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(video.getAbsolutePath());
            command.add("-vf");
            command.add("thumbnail");
            command.add("-q:v");
            command.add("2");
            command.add("-frames:v");
            command.add("1");
            command.add(outputFileName);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg failed with code: " + exitCode);
            } else{
                System.out.println("First frame extracted succesfully:  " + outputFileName);
            }

            System.out.println("Extracted frame: " + outputFileName);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error al extraer frame: " + video.getName());
            e.printStackTrace();
        }
    }

    //Concatenates separate audio files to create the whole one
    private void concatenateAudios(List<File> audios, File output){
        try{
            System.out.println("Concatenating audio files");

            //Create list file of audios to concatenate
            Path listFile = Paths.get("lista.txt");
            List<String> fileLines = new ArrayList<>();
            for (File file : audios) {
                fileLines.add("file '" + file.getAbsolutePath() + "'");
            }
            Files.write(listFile, fileLines);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", listFile.toAbsolutePath().toString(),
                    "-c", "copy",
                    output.getAbsolutePath()
            );

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();

            //Deletes list file
            Files.deleteIfExists(listFile);

            if (exitCode != 0) {
                System.err.println("Error with ffmpeg: " + exitCode);
            } else {
                System.out.println("Audios concatenated successfully");
            }
        } catch (InterruptedException|IOException e) {
            System.err.println("Error joining files: " + e.getMessage());
        }
    }

    //Adds the audio to the final outputVideo
    public void addAudioVideo(File audio, File video, File output) {
        int delayMs = 3000;

        //Adds audio after the 3 seconds the first postcard shows.
        System.out.println("Adding audio file: " + audio.getName() + " to video: " + video.getName());
        String videoPath = video.getAbsolutePath();
        String audioPath = audio.getAbsolutePath();
        String outputPath = output.getAbsolutePath();

        String delayFilter = String.format("[1:a]adelay=%d|%d[delayed_audio]", delayMs, delayMs);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoPath, "-i", audioPath,
                "-filter_complex", delayFilter,
                "-map", "0:v", "-map", "[delayed_audio]",
                "-c:v", "copy", "-c:a", "aac", outputPath
        );

        System.out.println(pb);

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Audio added successfully");
            } else {
                System.err.println("Failed to add audio to video. FFmpeg exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            System.err.println("Operation interrupted");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error adding audio to video: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
