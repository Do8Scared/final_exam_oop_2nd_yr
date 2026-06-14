package Main;

import ui.RoleSelectionFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point for the Garahe Ni Mateicla Online Food Ordering System.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RoleSelectionFrame().setVisible(true);
        });
    }
}
