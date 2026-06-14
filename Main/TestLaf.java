import javax.swing.UIManager;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

public class TestLaf {
    public static void main(String[] args) {
        boolean success = FlatMacDarkLaf.setup();
        System.out.println("Setup returned: " + success);
        System.out.println("Current LAF: " + UIManager.getLookAndFeel().getName());
        System.out.println("Class loaded: " + UIManager.getLookAndFeel().getClass().getName());
    }
}
