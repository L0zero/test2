package io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 默认流输入
 */
public class StreamInputSystem extends BufferedInputSystem {

    public StreamInputSystem() {
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public StreamInputSystem(InputStream in) {
        bufferedReader = new BufferedReader(new InputStreamReader(in));
    }

}
