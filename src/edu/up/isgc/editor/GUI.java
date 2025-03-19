package edu.up.isgc.editor;

//libraries
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GUI extends JFrame {

    //global variables
    private JPanel panel;
    private JPanel[] rows = new JPanel[3];
    private JButton generateVideo;
    private JTextField prompt;

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
        textfield();
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

    private void textfield() {
        prompt = new JTextField(20);
        prompt.setEditable(true);

        rows[1].add(prompt);
    }

    //buttons configuration
    private void button() {

        generateVideo = new JButton("Generate Video");
        //generateVideo.setBounds(400, 100, 200, 50);
        generateVideo.setEnabled(false);

        JButton chooseFile = new JButton("Select files");
        //chooseFile.setBounds(400, 100, 200, 50);
        chooseFile.setEnabled(true);

        //select file
        ActionListener selectFile = _ -> fileChooser();

        ActionListener generateVideoTrigger = e -> {

        };

        //action assignation
        generateVideo.addActionListener(generateVideoTrigger);
        chooseFile.addActionListener(selectFile);

        //insert buttons
        rows[0].add(chooseFile);
        rows[2].add(generateVideo);
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
            generateVideo.setEnabled(true);
        }
    }

}
