package edu.up.isgc.editor;

public class VideoEditor {
    public static void main(String[] args) {
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        //create new window
        GUI editor = new GUI();
        //editor.setVisible(true);

        //set focus on window (avoid text field focus)
        editor.requestFocusInWindow();

        ChatGPT imagen = new ChatGPT();
        imagen.downloadImage("Generate an image that represents relaxing beach vibe", "testeohermano.jpg");
    }
}