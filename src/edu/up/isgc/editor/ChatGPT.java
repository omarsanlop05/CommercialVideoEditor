package edu.up.isgc.editor;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ChatGPT {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    //USE HERE YOUR API KEY
    private final String apiKey = "";

    private String textToImage(String description) {
        String prompt = "Generate an image in postcard format that shows" + description + ".";

        System.out.println("Generating command");

        String model = "dall-e-3";
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
            e.printStackTrace();
            return null;
        }
    }

    private File processImagesScript(File imageFiles, File sourceVideo) {

        File dir = new File("output-files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outputFile = new File(dir, "script" + System.currentTimeMillis() + ".txt");

        double duration = getVideoDuration(sourceVideo);
        if(duration < 3){
            duration = 3;
        }
        int targetWords = (int) Math.round(duration * 2.8);
        System.out.println("Target words: " + targetWords);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile.getAbsolutePath()), StandardCharsets.UTF_8)) {
            String description = imageToScript(imageFiles, targetWords);
            if (description != null && !description.isEmpty()) {
                writer.write(description);
                writer.newLine();
            }
            System.out.println("Descriptions saved to: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String imageToScript(File file, int targetWords) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            String prompt = String.format(
                    "Describe in detail what is happening in this image. " +
                            "The description should be exactly %d words (±5 words). " +
                            "Be precise and include all important elements.",
                    targetWords);

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
            e.printStackTrace();
            return null;
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
            e.printStackTrace();
            return "Error parsing response";
        }
    }

    public File ImageToAudio(File imageFiles, File sourceVideo) {
        try {
            File text = processImagesScript(imageFiles, sourceVideo);
            System.out.println("Generating audio file from: " + text.getAbsolutePath());

            File dir = new File("output-files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File audioName = new File(dir, "output_audio" + System.currentTimeMillis() + ".mp3");

            // Leer y limpiar el texto
            String script = Files.readString(text.toPath());
            script = script.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .trim();

            System.out.println("Script: " + script);
            System.out.println("Extracting duration of the video");

            double voiceSpeed = 1.0;

            System.out.println("Voice speed: " + voiceSpeed);

            String jsonBody = String.format("{\"model\":\"tts-1\",\"input\":\"%s\",\"voice\":\"alloy\",\"speed\":%.1f}",
                    script, voiceSpeed);

            // Crear archivo temporal para el JSON
            File jsonTempFile = File.createTempFile("tts-request", ".json");
            Files.writeString(jsonTempFile.toPath(), jsonBody, StandardCharsets.UTF_8);
            jsonTempFile.deleteOnExit();  // Borrar al terminar

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
            command.add("@" + jsonTempFile.getAbsolutePath());  // <-- Clave aquí
            command.add("--output");
            command.add(audioName.getAbsolutePath());

            System.out.println("Executing command: " + command);

            Process curlProcess = new ProcessBuilder(command).start();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(curlProcess.getErrorStream()));

            //cambiar este pedooooooooooooooooooooooooooooooooooooo
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.err.println("cURL Error: " + errorLine);
            }

            curlProcess.waitFor();
            System.out.println("Audio generado: " + audioName.getAbsolutePath());

            jsonTempFile.delete();

            return audioName;

        } catch (IOException | InterruptedException e) {
            System.err.println("Error generating audio: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private double getVideoDuration(File videoFile) {
        try{
            System.out.println("Getting video duration");
            ProcessBuilder pb = new ProcessBuilder("ffprobe", "-i", videoFile.getAbsolutePath(),
                    "-show_entries", "format=duration", "-v", "quiet",
                    "-of", "csv=p=0");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String durationStr = reader.readLine().trim();
            System.out.println("Duration: " + durationStr);
            return Double.parseDouble(durationStr);
        } catch (Exception e) {
            System.err.println("Error parsing video duration: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public void downloadImage(String prompt, String outputPath){

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
            } else {
                System.err.println("Could not download image");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Could not download image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractURL(String curlResponse) {
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
            e.printStackTrace();
            return null;
        }
    }
}
