package Main;

import services.POSTerminal;

/**
 * Entry point for the Garahe Ni Mateicla POS system.
 * This class is intentionally thin — all logic lives in service classes
 * so the UI can be swapped (e.g., from console to GUI) without changing this file.
 */
public class Main {

    public static void main(String[] args) {
        new POSTerminal().run();
    }
}
