package services;

import java.util.Scanner;

/**
 * Centralized input validation utilities for the POS terminal.
 * Extracts all input-reading and validation logic from the Main class
 * so it can be shared across services and replaced by GUI inputs later.
 */
public class InputHelper {

    private final Scanner scanner;

    /**
     * Constructs an InputHelper using the given Scanner instance.
     *
     * @param scanner the Scanner to read console input from
     */
    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Prompts the user for an integer input and repeats until a valid integer is provided.
     *
     * @param prompt the message to display to the user
     * @return the parsed integer value
     */
    public int getValidIntegerInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> [SYSTEM ERROR] Invalid input. Please type a number.");
            }
        }
    }

    /**
     * Prompts the user for a floating-point monetary input and repeats until valid.
     *
     * @param prompt the message to display to the user
     * @return the parsed double value
     */
    public double getValidDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> [SYSTEM ERROR] Invalid monetary value. Please type a number.");
            }
        }
    }

    /**
     * Prompts the user for non-empty text input and repeats until provided.
     *
     * @param prompt the message to display to the user
     * @return the trimmed string input
     */
    public String getNonEmptyInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] This field cannot be blank.");
        }
    }

    /**
     * Prompts the user for a positive double value (must be > 0).
     *
     * @param prompt the message to display to the user
     * @return the positive double value
     */
    public double getPositiveDoubleInput(String prompt) {
        while (true) {
            double value = getValidDoubleInput(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] Value must be greater than zero.");
        }
    }

    /**
     * Prompts the user for a non-negative integer value (must be >= 0).
     *
     * @param prompt the message to display to the user
     * @return the non-negative integer value
     */
    public int getNonNegativeIntegerInput(String prompt) {
        while (true) {
            int value = getValidIntegerInput(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println(">> [SYSTEM ERROR] Value cannot be negative.");
        }
    }

    /**
     * Reads a line of text input from the user (no validation other than trimming).
     *
     * @param prompt the message to display to the user
     * @return the trimmed user input (may be empty)
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Closes the underlying Scanner resource.
     */
    public void close() {
        scanner.close();
    }
}
