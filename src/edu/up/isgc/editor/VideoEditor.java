package edu.up.isgc.editor;

import java.io.File;
import java.util.List;

public class VideoEditor {
    public static void main(String[] args) {
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        //create new window
        GUI editor = new GUI();
        //editor.setVisible(true);

        //set focus on window (avoid text field focus)
        editor.requestFocusInWindow();

        ChatGPT imagen = new ChatGPT();
        //imagen.downloadImage("Generate an image that represents relaxing beach vibe", "testeohermano.jpg");
        List<File> imageFiles = List.of(
                new File("testeohermano.jpg")
        );
        //imagen.processImagesToScript(imageFiles, "script.txt");
        File script = new File("script.txt");
        File video = new File("script.txt");
        System.out.println(imagen.textToAudio(script, video).getAbsolutePath());
    }
}