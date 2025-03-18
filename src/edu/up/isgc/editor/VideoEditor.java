package edu.up.isgc.editor;

public class VideoEditor {
    public static void main(String[] args) {

        //create new window
        GUI editor = new GUI();
        editor.setVisible(true);

        //set focus on window (avoid text field focus)
        editor.requestFocusInWindow();
    }
}
