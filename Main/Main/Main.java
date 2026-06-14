package Main;

import ui.RoleSelectionFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point for the Garahe Ni Mateicla Online Food Ordering System.
 */
public class Main {
    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.themes.FlatMacDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        
        SwingUtilities.invokeLater(() -> {
            new RoleSelectionFrame().setVisible(true);
        });
    }
}
