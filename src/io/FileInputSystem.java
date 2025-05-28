package io;

import java.io.*;

public class FileInputSystem extends BufferedInputSystem {

    public FileInputSystem(File file) {
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
