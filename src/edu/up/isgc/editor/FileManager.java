package edu.up.isgc.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private List<File> inputFiles;

    public FileManager(List<File> inputFiles) {
        setInputFiles(inputFiles);
    }

    public List<File> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(List<File> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public void ArrangeFiles() {
        
    }
}
