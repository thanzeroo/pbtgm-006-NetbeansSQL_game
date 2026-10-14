import com.formdev.flatlaf.FlatLightLaf;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class Save2 extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Save2.class.getName());

    public Save2() {
        initComponents();
        setLocationRelativeTo(null); 
        getContentPane().setBackground(new java.awt.Color(0, 51, 51));
        loadDataPlayer(); 
    }

    public void loadDataPlayer() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        
        model.addColumn("ID");
        model.addColumn("Nama Player");
        model.addColumn("Damage (Strength)");
        model.addColumn("Total Klik");
        model.addColumn("Monster ID");
        model.addColumn("Sisa HP Monster");
        model.addColumn("Terakhir Disimpan");

        String sql = "SELECT id, player_name, strength, click_count, current_monster_id, monster_current_hp, updated_at FROM players ORDER BY id DESC";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("player_name"),
                    rs.getInt("strength"),
                    rs.getInt("click_count"),
                    rs.getInt("current_monster_id"),
                    rs.getInt("monster_current_hp"),
                    rs.getTimestamp("updated_at")
                });
            }
            jTable1.setModel(model);
        } catch (Exception e) {
            System.err.println("Gagal memuat save data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnPlay = new javax.swing.JButton();
        btnNewGame = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Menu Save & Karakter");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        btnPlay.setBackground(new java.awt.Color(0, 102, 0));
        btnPlay.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnPlay.setForeground(new java.awt.Color(255, 255, 255));
        btnPlay.setText("▶ PLAY / LANJUTKAN");
        btnPlay.addActionListener(this::btnPlayActionPerformed);

        btnNewGame.setBackground(new java.awt.Color(0, 102, 0));
        btnNewGame.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnNewGame.setForeground(new java.awt.Color(255, 255, 255));
        btnNewGame.setText("✚ NEW GAME");
        btnNewGame.addActionListener(this::btnNewGameActionPerformed);

        btnDelete.setBackground(new java.awt.Color(0, 102, 0));
        btnDelete.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnDelete.setText("🗑 HAPUS SAVE");
        btnDelete.addActionListener(this::btnDeleteActionPerformed);

        btnBack.setBackground(new java.awt.Color(0, 102, 0));
        btnBack.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnBack.setForeground(new java.awt.Color(255, 255, 255));
        btnBack.setText("⬅ KEMBALI KE MENU");
        btnBack.addActionListener(this::btnBackActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(btnPlay, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnNewGame, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                        .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPlay, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnNewGame, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * PLAY: Memilih data Save2 dari tabel dan langsung bermain game
     */
    private void btnPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        int row = jTable1.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, 
                "Silakan klik salah satu baris save data pada tabel terlebih dahulu!", 
                "Peringatan", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerId = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
        
        // Buka Game dengan memuat data player terpilih
        Game game = new Game(playerId);
        game.setVisible(true);
        
        // Tutup menu Save2
        this.dispose();
    }//GEN-LAST:event_btnPlayActionPerformed

    /**
     * CREATE: Membuat Save2 data karakter baru (New Game)
     */
    private void btnNewGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewGameActionPerformed
        String name = JOptionPane.showInputDialog(this, 
            "Masukkan Nama Player / Karakter Baru:", 
            "New Game", 
            JOptionPane.PLAIN_MESSAGE);
            
        if (name == null || name.trim().isEmpty()) {
            return; // Dibatalkan oleh player
        }

        String sql = "INSERT INTO players (player_name, strength, stat_points, click_count, current_monster_id, monster_current_hp) VALUES (?, 1, 0, 0, 1, 25)";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, name.trim());
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newPlayerId = rs.getInt(1);
                    JOptionPane.showMessageDialog(this, 
                        "Karakter '" + name.trim() + "' berhasil dibuat! Selamat bermain!", 
                        "Sukses", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Langsung jalankan game untuk karakter baru
                    Game game = new Game(newPlayerId);
                    game.setVisible(true);
                    this.dispose();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Gagal membuat save data baru: " + e.getMessage(), 
                "Error Database", 
                JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnNewGameActionPerformed

    /**
     * DELETE: Menghapus Save2 data yang dipilih
     */
    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        int row = jTable1.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, 
                "Pilih baris save data yang ingin dihapus!", 
                "Peringatan", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
        String pName = jTable1.getValueAt(row, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Apakah Anda yakin ingin menghapus save data milik '" + pName + "' (ID: " + id + ")?", 
            "Konfirmasi Hapus", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM players WHERE id = ?";
            try (Connection conn = dbconnection.configDB();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, id);
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Save data berhasil dihapus!");
                loadDataPlayer(); // Segarkan kembali tabel
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    /**
     * BACK: Kembali ke menu Start
     */
    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new Start().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new Save2().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnNewGame;
    private javax.swing.JButton btnPlay;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
