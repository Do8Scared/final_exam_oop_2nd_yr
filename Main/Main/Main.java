package Main;

import services.CustomerTerminal;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the Garahe Ni Mateicla Online Food Ordering System.
 * This class is intentionally thin — all logic lives in service classes
 * so the UI can be swapped (e.g., from console to GUI) without changing this file.
 */
public class Main {

    public static void main(String[] args) {
        // Force the console output to UTF-8
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        new CustomerTerminal().run();
    }
}
