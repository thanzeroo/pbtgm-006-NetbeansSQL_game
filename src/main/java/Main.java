
import com.formdev.flatlaf.FlatLightLaf;

public class Main {
    
    public static void main(String[] args) {
        FlatLightLaf.setup();
        /* Tampilkan Loading Screen dulu, lalu buka Start */
        java.awt.EventQueue.invokeLater(() -> {
            new LoadingScreen().setVisible(true);
        });
    }
}

