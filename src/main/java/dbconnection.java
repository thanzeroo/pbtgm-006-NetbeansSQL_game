import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Kelas Koneksi Database MySQL
 * Menghubungkan game ke database 'aplikasi_game_clicker'
 */
public class dbconnection {
    
    public static Connection configDB() throws SQLException {
        Connection mysqlconfig = null;
        try {
            // URL Database, Username, dan Password MySQL
            String url = "jdbc:mysql://localhost:3306/aplikasi_game_clicker?useSSL=false&allowPublicKeyRetrieval=true";
            String user = "root"; 
            String pass = ""; // Default XAMPP / MariaDB lokal tidak memakai password
            
            mysqlconfig = DriverManager.getConnection(url, user, pass); 
        } catch (SQLException e) {
            System.err.println("Koneksi Database Gagal: " + e.getMessage());
            throw e;
        }
        return mysqlconfig;
    }
}
