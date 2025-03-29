package edu.up.isgc.editor;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChatGPT {
    private HttpServer server;
    private final String apiKey = "";

    public void startServer(String filePath){
        try {
            //http://localhost:8000/images
            server = HttpServer.create(new InetSocketAddress(8000), 0);

            server.createContext("/images", exchange -> {
                File file = new File(filePath);

                if (!file.exists()) {
                    String response = "File not found";
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }

                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                FileInputStream fs = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fs.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fs.close();
                os.close();
            });

            server.setExecutor(null); // Usa el executor por defecto
            server.start();

            System.out.println("Server started at http://localhost:8000/images");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server stopped.");
        }
    }

    /*
    public String imageToText(File file){

        curl --location 'https://api.openai.com/v1/chat/completions' \
        --header 'Content-Type: application/json' \
        --header 'Authorization: Bearer $OPENAI_API_KEY' \
        --data '{
            "model": "gpt-4o",
            "messages": [
                {
                "role": "user",
                "content": [
                    {
                    "type": "text",
                    "text": "What is in this image?"
                    },
                    {
                    "type": "image_url",
                    "image_url": {
                        "url": "https://twoplaidaprons.com/wp-content/uploads/2021/01/Korean-cheese-corn-dogs-angled-view-of-fried-corn-dogs-on-a-plate-with-ketcup-and-mustard.jpg"
                    }
                    }
                ]
                }
            ],
            "max_tokens": 300
        }
        '

    }

    /*
    public File textToAudio(String text){
        curl --location 'https://api.openai.com/v1/audio/speech' \
        --header 'Content-Type: application/json' \
        --header 'Authorization: Bearer $OPENAI_API_KEY' \
        --data '{
        "model": "gpt-4o-mini-tts",
                "input": "The quick brown fox jumped over the lazy dog.",
                "voice": "alloy",
                "response_format": "mp3"
    }'
            --output audio.mp3
    }
    */

    public String textToImage(String prompt) {

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
            System.out.println("Generating an image of " + prompt);
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

    public void downloadImage(String prompt, String outputPath){
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
            e.printStackTrace();
        }
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
