package edu.up.isgc.editor;

import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ChatGPT {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey = "";

    public File processImagesToScript(List<File> imageFiles, String outputFilePath) {

        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outputFile = new File(dir, outputFilePath + System.currentTimeMillis() + ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
            for (File imageFile : imageFiles) {
                String description = imageToText(imageFile);
                if (description != null && !description.isEmpty()) {
                    writer.write(description);
                    writer.newLine();
                }
            }
            System.out.println("Descriptions saved to: " + outputFilePath);
            return outputFile;
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        return null;
    }

    public String imageToText(File file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            String prompt = "Describe briefly what is in the image (max 50 words)";

            String jsonRequest = String.format(
                    "{\"model\": \"gpt-4o\", \"max_tokens\": %d, \"messages\": [{\"role\": \"user\", \"content\": ["
                            + "{\"type\": \"text\", \"text\": \"%s\"},"
                            + "{\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64,%s\"}}"
                            + "]}]}",
                    300,
                    prompt,
                    base64Image
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractContentFromJson(response.body());
            } else {
                System.err.println("API error: " + response.statusCode() + " - " + response.body());
                return "Error: " + response.statusCode();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing image: " + e.getMessage());
            return "Error processing image";
        }
    }

    private String extractContentFromJson(String jsonResponse) {
        try {
            int contentStart = jsonResponse.indexOf("\"content\":") + 10;
            if (contentStart < 10) return "No content found";

            int textStart = jsonResponse.indexOf("\"", contentStart) + 1;
            if (textStart < 1) return "No text start found";

            int textEnd = jsonResponse.indexOf("\"", textStart);
            if (textEnd <= textStart) return "No text end found";

            String content = jsonResponse.substring(textStart, textEnd);

            content = content.replace("\\n", "\n").replace("\\\"", "\"");

            return content;
        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            return "Error parsing response";
        }
    }

    public File textToAudio(File text, File sourceVideo){
        try{
            File dir = new File("output-files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File audioName = new File(dir, "output_audio" + System.currentTimeMillis() + ".mp3");
            String script = Files.readString(Path.of(text.getAbsolutePath()));

            ProcessBuilder pb = new ProcessBuilder("ffprobe", "-i", sourceVideo.getAbsolutePath(), "-show_entries", "format=duration", "-v", "quiet", "-of", "csv=p=0");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String durationStr = reader.readLine();
            double duration = Double.parseDouble(durationStr);

            double wordsPerSecond = script.split("\\s+").length / duration;
            double voiceSpeed = Math.min(2.0, Math.max(0.5, wordsPerSecond / 2.5)); // Ajustar dentro del rango permitido

            String jsonBody = "{"
                    + "\"model\": \"gpt-4o-mini-tts\","
                    + "\"input\": \"" + script + "\","
                    + "\"voice\": \"alloy\","
                    + "\"speed\": " + voiceSpeed
                    + "}";

            List<String> command = new ArrayList<>();
            command.add("curl");
            command.add("-X");
            command.add("POST");
            command.add("https://api.openai.com/v1/audio/speech");
            command.add("-H");
            command.add("Content-Type: application/json");
            command.add("-H");
            command.add("Authorization: Bearer " + apiKey);
            command.add("-d");
            command.add(jsonBody);
            command.add("--output");
            command.add(audioName.getAbsolutePath());

            Process curlProcess = new ProcessBuilder(command).start();
            curlProcess.waitFor();
            System.out.println("Audio generado: " + audioName);
            return audioName;
        } catch (IOException|InterruptedException e){
            System.err.println("Error generating audio: " + e.getMessage());
        }
        return null;
    }

    public String textToImage(String description) {
        String prompt = "Generate a postcard that represents" + description + ".";

        System.out.println("Generating command");

        String model = "dall-e-2";
        String size = "1024x1024";

        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String data = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"n\":1,\"size\":\"%s\",\"response_format\":\"url\"}",
                model, escapedPrompt, size
        );

        List<String> command = new ArrayList<>();
        command.add("curl");
        command.add("-X");
        command.add("POST");
        command.add("https://api.openai.com/v1/images/generations");
        command.add("-H");
        command.add("Content-Type: application/json");
        command.add("-H");
        command.add("Authorization: Bearer " + apiKey);
        command.add("-d");
        command.add("\"" + data.replace("\"", "\\\"") + "\"");

        System.out.println("Complete curl command: " + String.join(" ", command));

        try {
            System.out.println("Generating an image of " + description);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder response = new StringBuilder();

            try (InputStream inputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Curl error (code " + exitCode + "): " + response);
            }

            System.out.println("Image generated, extracting url");

            return extractURL(response.toString());

        } catch (IOException | InterruptedException e) {
            System.err.println("Error generating image: " + e.getMessage());
            return null;
        }
    }

    public File downloadImage(String prompt, String outputPath){

        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File outputFile = new File(dir, outputPath + System.currentTimeMillis() + ".mp4");

        String[] command = {
                "curl",
                "-o",
                outputPath,
                "-L",
                textToImage(prompt)
        };

        System.out.println("Downloading image");

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if(exitCode == 0){
                System.out.println("Image downloaded");
                return outputFile;
            } else {
                System.err.println("Could not download image");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String extractURL(String curlResponse) {
        try {
            System.out.println("Full response: " + curlResponse);
            String jsonStr = curlResponse.substring(curlResponse.indexOf("{"));

            int urlStart = jsonStr.indexOf("\"url\"");
            if (urlStart == -1) return null;

            urlStart = jsonStr.indexOf(":", urlStart) + 1;

            while (urlStart < jsonStr.length() &&
                    (Character.isWhitespace(jsonStr.charAt(urlStart)) ||
                            jsonStr.charAt(urlStart) == '"' ||
                            jsonStr.charAt(urlStart) == '\'')) {
                urlStart++;
            }

            if (urlStart >= jsonStr.length()) return null;

            int urlEnd = urlStart;
            while (urlEnd < jsonStr.length() &&
                    jsonStr.charAt(urlEnd) != '"' &&
                    jsonStr.charAt(urlEnd) != '\'') {
                urlEnd++;
            }

            return jsonStr.substring(urlStart, urlEnd);
        } catch (Exception e) {
            System.err.println("Error extracting url: " + e.getMessage());
            return null;
        }
    }
}
