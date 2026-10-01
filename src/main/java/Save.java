
import com.formdev.flatlaf.FlatDarkLaf;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Save extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Save.class.getName());

    public int id;
    public int selectedPlayerId = -1;
    public SaveCard selectedVisualCard = null;

    public Save() {
        initComponents();
        setLocationRelativeTo(null);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

        scrollPanel.setViewportView(contentPanel);

        loadDataPlayer(contentPanel);
    }

    public void setSelectedPlayer(int id, SaveCard clickedCard) {
        if (selectedVisualCard != null) {
            selectedVisualCard.setCardSelected(false);
        }

        this.selectedPlayerId = id;
        this.selectedVisualCard = clickedCard;
        this.selectedVisualCard.setCardSelected(true);
    }

    public void loadDataPlayer(JPanel container) {
        String sql = "SELECT id, player_name, strength, click_count, current_monster_id, monster_current_hp, updated_at FROM players ORDER BY id";
        try (Connection conn = dbconnection.configDB(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            container.removeAll();
            container.add(javax.swing.Box.createHorizontalStrut(30));

            while (rs.next()) {
                int currentId = rs.getInt("id");
                String name = rs.getString("player_name");
                int strength = rs.getInt("strength");

                SaveCard card = new SaveCard(currentId, name, strength, this);
                container.add(card);
                container.add(javax.swing.Box.createHorizontalStrut(30));
            }

            container.revalidate();
            container.repaint();
        } catch (Exception e) {
            System.err.println("Gagal memuat save data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnPlay = new javax.swing.JButton();
        btnNewGame = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        scrollPanel = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Menu Save & Karakter");

        btnPlay.setBackground(new java.awt.Color(153, 0, 102));
        btnPlay.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnPlay.setForeground(new java.awt.Color(255, 255, 255));
        btnPlay.setText("PLAY / LANJUTKAN");
        btnPlay.addActionListener(this::btnPlayActionPerformed);

        btnNewGame.setBackground(new java.awt.Color(153, 0, 102));
        btnNewGame.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnNewGame.setForeground(new java.awt.Color(255, 255, 255));
        btnNewGame.setText("NEW GAME");
        btnNewGame.addActionListener(this::btnNewGameActionPerformed);

        btnDelete.setBackground(new java.awt.Color(153, 0, 102));
        btnDelete.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnDelete.setText("HAPUS SAVE");
        btnDelete.addActionListener(this::btnDeleteActionPerformed);

        btnBack.setBackground(new java.awt.Color(153, 0, 102));
        btnBack.setFont(new java.awt.Font("JetBrains Mono", 1, 14)); // NOI18N
        btnBack.setForeground(new java.awt.Color(255, 255, 255));
        btnBack.setText("KEMBALI KE MENU");
        btnBack.addActionListener(this::btnBackActionPerformed);

        scrollPanel.setBackground(new java.awt.Color(102, 0, 102));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scrollPanel)
                    .addGroup(layout.createSequentialGroup()
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
                .addComponent(scrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 573, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
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

    private void btnPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        if (selectedPlayerId == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Silakan klik/pilih salah satu kartu save data terlebih dahulu!",
                    "Peringatan",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        Game game = new Game(selectedPlayerId);
        game.setVisible(true);

        this.dispose();
    }//GEN-LAST:event_btnPlayActionPerformed

    private void btnNewGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewGameActionPerformed
        String name = JOptionPane.showInputDialog(this,
                "Masukkan Nama Save",
                "New Game",
                JOptionPane.PLAIN_MESSAGE);

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO players (player_name, strength, stat_points, click_count, current_monster_id, monster_current_hp) VALUES (?, 1, 0, 0, 1, 25)";
        try (Connection conn = dbconnection.configDB(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name.trim());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newPlayerId = rs.getInt(1);
                    JOptionPane.showMessageDialog(this,
                            "Karakter '" + name.trim() + "' berhasil dibuat! Selamat bermain!",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE);

                    Game game = new Game(newPlayerId);
                    game.setVisible(true);
                    this.dispose();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal membuat karakter baru: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnNewGameActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (selectedPlayerId == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih karakter yang ingin dihapus terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus save data ini secara permanen?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM players WHERE id = ?";
            try (Connection conn = dbconnection.configDB(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, selectedPlayerId);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Save data berhasil dihapus!");

                // Reset seleksi dan muat ulang list kartu
                selectedPlayerId = -1;
                selectedVisualCard = null;
                // Mengambil kembali kontainer dari viewport scrollPanel
                JPanel contentPanel = (JPanel) scrollPanel.getViewport().getView();
                loadDataPlayer(contentPanel);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus data: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed


    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new Start().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FlatDarkLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new Save2().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnNewGame;
    private javax.swing.JButton btnPlay;
    private javax.swing.JScrollPane scrollPanel;
    // End of variables declaration//GEN-END:variables
}
