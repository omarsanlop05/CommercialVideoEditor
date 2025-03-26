package edu.up.isgc.editor;

//libraries
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GUI extends JFrame {

    //global variables
    private JPanel panel;
    private JPanel[] rows = new JPanel[3];
    private JButton generateVideoButton;
    private JTextField prompt = new JTextField(20);
    private List<File> inputFiles = new ArrayList<>();

    //constructor
    public GUI() {
        this.setSize(500, 400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        components();
    }

    //component initialization
    private void components() {
        panel();
        label();
        textField();
        button();
        rowInsertion();
    }

    //panels configuration
    private void panel() {
        //Main panel
        panel = new JPanel();
        this.getContentPane().add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Row declaration
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new JPanel(new FlowLayout(FlowLayout.CENTER));
        }
    }

    //labels configuration
    private void label() {
        //title configuration
        JLabel title = new JLabel("Commercial Video Editor", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        //labels configuration and assignation
        JLabel instruction = new JLabel("Selected the files that will create the video. Can be images or videos.");
        JLabel caption = new JLabel("Write a short prompt that describes the mood of the video.");

        rows[0].add(instruction);
        rows[1].add(caption);
    }

    //text field configurations
    private void textField() {

        //activate calculate button once it has received data
        DocumentListener editData = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkGenerateButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkGenerateButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkGenerateButtonState();
            }

        };

        //text field assignation and reconfiguration
        prompt.getDocument().addDocumentListener(editData);

        rows[1].add(prompt);
    }

    //buttons configuration
    private void button() {

        generateVideoButton = new JButton("Generate Video");
        //generateVideo.setBounds(400, 100, 200, 50);
        generateVideoButton.setEnabled(false);

        JButton chooseFile = new JButton("Select files");
        //chooseFile.setBounds(400, 100, 200, 50);
        chooseFile.setEnabled(true);

        //select file
        ActionListener selectFile = _ -> fileChooser();

        ActionListener generateVideoTrigger = e -> {
            if (!inputFiles.isEmpty()) {
                Tools editor = new Tools();
                editor.setInputFiles(inputFiles);
                editor.arrangeFiles();
                System.out.println("Generating video from selected files:");
                for (File file : editor.getInputFiles()) {
                    System.out.println(file.getAbsolutePath());
                }
                editor.joinFiles(editor.getInputFiles());
                System.out.println("The video has been generated");
            } else {
                System.out.println("No files selected.");
            }
        };

        //action assignation
        generateVideoButton.addActionListener(generateVideoTrigger);
        chooseFile.addActionListener(selectFile);

        //insert buttons
        rows[0].add(chooseFile);
        rows[2].add(generateVideoButton);
    }

    //insert different rows in main panel
    private void rowInsertion() {
        for (JPanel row : rows) panel.add(row);
    }

    //fileChooser function
    private void fileChooser() {
        JFileChooser chosenFile = new JFileChooser();
        chosenFile.setDialogTitle("File Selector");
        chosenFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chosenFile.setAcceptAllFileFilterUsed(false);
        chosenFile.setMultiSelectionEnabled(true);

        // File filter
        javax.swing.filechooser.FileNameExtensionFilter fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("Images and Videos (JPG, PNG, GIF, BMP, JPEG, MP4, AVI, MOV, MKV)", "jpg", "jpeg", "png", "gif", "bmp", "mp4", "avi", "mov", "mkv");

        // Add filters
        chosenFile.addChoosableFileFilter(fileFilter);

        int selection = chosenFile.showOpenDialog(this);

        if (selection == JFileChooser.APPROVE_OPTION) {
            inputFiles.clear();

            for(File file : chosenFile.getSelectedFiles()) {
                inputFiles.add(file);
            }

            System.out.println("Files selected correctly: ");
            for(File file : inputFiles) {
                System.out.println(file.getAbsolutePath());
            }

            checkGenerateButtonState();
        }

    }

    private void checkGenerateButtonState() {
        generateVideoButton.setEnabled(!prompt.getText().isEmpty() && !inputFiles.isEmpty());
    }
}
