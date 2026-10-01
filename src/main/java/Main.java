
import com.formdev.flatlaf.FlatLightLaf;

public class Main {
    
    public static void main(String[] args) {
        /* Jalankan jendela menu Start */
        java.awt.EventQueue.invokeLater(() -> {
            new Start().setVisible(true);
        });
        FlatLightLaf.setup();
    }
}

