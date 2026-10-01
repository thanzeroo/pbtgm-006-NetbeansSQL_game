import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class Game extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Game.class.getName());

    // ==========================================
    // VARIABEL STATE PLAYER & GAME
    // ==========================================
    private int currentPlayerId = 1;
    private String currentPlayerName = "Player";
    private int strength = 1;       // Damage per klik (berlipat ganda *2 tiap monster mati)
    private int clickCount = 0;     // Jumlah klik pemain

    // ==========================================
    // VARIABEL STATE MONSTER
    // ==========================================
    private int monsterId = 1;
    private String monsterName = "Slime Ijo";
    private int maxHp = 25;
    private int currentHp = 25;
    private String asciiNormal = "";
    private String asciiHit = "";
    
    // Kunci keamanan agar klik cepat tidak memicu lompatan ID monster (1, 3, 5)
    private boolean isMonsterDefeated = false;

    /**
     * Konstruktor default
     */
    public Game() {
        this(1);
    }

    /**
     * Konstruktor dengan ID Player yang dipilih dari menu Save
     */
    public Game(int pId) {
        initComponents();
        setLocationRelativeTo(null); // Tampil tepat di tengah monitor

        // Format font monospaced tebal untuk tampilan ASCII art
        txtAsciiDisplay.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        txtAsciiDisplay.setEditable(false);

        // Muat data Save player dari database
        loadPlayerData(pId);
    }

    /**
     * Memuat data player yang tersimpan dari database 'aplikasi_game_clicker'
     */
    public void loadPlayerData(int pId) {
        this.currentPlayerId = pId;
        String sql = "SELECT id, player_name, strength, click_count, current_monster_id, monster_current_hp FROM players WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, pId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.currentPlayerName = rs.getString("player_name");
                    this.strength = rs.getInt("strength");
                    if (this.strength < 1) this.strength = 1;
                    this.clickCount = rs.getInt("click_count");
                    int savedMonsterId = rs.getInt("current_monster_id");
                    int savedHp = rs.getInt("monster_current_hp");

                    // Muat data monster
                    boolean loaded = loadMonsterFromDB(savedMonsterId);
                    if (loaded && savedHp > 0 && savedHp <= maxHp) {
                        this.currentHp = savedHp;
                    }
                    updateStatusDisplay();
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat player: " + e.getMessage());
        }

        // Fallback jika player belum ada
        loadMonsterFromDB(1);
        updateStatusDisplay();
    }

    /**
     * Helper untuk membuat teks seni ASCII berada persis di tengah JTextArea
     * (Pusat Horizontal & Vertikal)
     */
    private String centerAscii(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        text = text.replace("\r", "");
        String[] lines = text.split("\n");
        
        int maxLen = 0;
        for (String line : lines) {
            if (line.length() > maxLen) {
                maxLen = line.length();
            }
        }
        
        int totalCols = 110;
        int padLeft = Math.max(0, (totalCols - maxLen) / 2);
        String padding = " ".repeat(padLeft);
        
        int totalRows = 22;
        int padTop = Math.max(0, (totalRows - lines.length) / 2);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padTop; i++) {
            sb.append("\n");
        }
        for (String line : lines) {
            sb.append(padding).append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Mengambil data monster dari tabel 'monsters'
     */
    public boolean loadMonsterFromDB(int id) {
        String sql = "SELECT id, name, max_hp, ascii_normal, ascii_hit FROM monsters WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int mId = rs.getInt("id");
                    String mName = rs.getString("name");
                    int mHp = rs.getInt("max_hp");
                    String mNormal = rs.getString("ascii_normal");
                    String mHit = rs.getString("ascii_hit");
                    
                    loadMonsterData(mId, mName, mHp, mNormal, mHit);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal membaca monster dari DB: " + e.getMessage());
        }
        return false;
    }

    /**
     * Memperbarui data monster dan mereset komponen UI
     */
    public void loadMonsterData(int id, String name, int hp, String normal, String hit) {
        this.monsterId = id;
        this.monsterName = name;
        this.maxHp = hp;
        this.currentHp = hp;
        this.asciiNormal = normal;
        this.asciiHit = hit;

        // Reset status kunci kematian monster
        this.isMonsterDefeated = false;
        
        updateStatusDisplay();
        txtAsciiDisplay.setText(centerAscii(asciiNormal));
        btnAttack.setEnabled(true);
    }

    /**
     * Memperbarui teks info HP, Damage, dan nama player
     */
    private void updateStatusDisplay() {
        lblMonsterName.setText(monsterName + "  (Monster #" + monsterId + ")  -  Pemain: " + currentPlayerName);
        pbMonsterHp.setMaximum(maxHp);
        pbMonsterHp.setValue(currentHp);
        lblHpInfo.setText("HP: " + currentHp + " / " + maxHp + "    |    Damage: " + strength + " (x2 Tiap Menang)    |    Total Klik: " + clickCount);
    }

    /**
     * UPDATE: Menyimpan progres game ke tabel 'players' di database
     */
    public void saveGameProgress(boolean showNotification) {
        String sql = "UPDATE players SET current_monster_id = ?, monster_current_hp = ?, strength = ?, click_count = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, monsterId);
            ps.setInt(2, currentHp);
            ps.setInt(3, strength);
            ps.setInt(4, clickCount);
            ps.setInt(5, currentPlayerId);
            
            int updated = ps.executeUpdate();
            if (updated > 0 && showNotification) {
                JOptionPane.showMessageDialog(this, 
                    "Progres Game Berhasil Disimpan!\n\n" +
                    "• Pemain: " + currentPlayerName + "\n" +
                    "• Monster Terakhir: " + monsterName + " (ID: " + monsterId + ")\n" +
                    "• Sisa HP: " + currentHp + " / " + maxHp + "\n" +
                    "• Damage (Strength): " + strength + "\n" +
                    "• Total Klik: " + clickCount, 
                    "Simpan Berhasil", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            if (showNotification) {
                JOptionPane.showMessageDialog(this, "Gagal menyimpan progres: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblMonsterName = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAsciiDisplay = new javax.swing.JTextArea();
        pbMonsterHp = new javax.swing.JProgressBar();
        lblHpInfo = new javax.swing.JLabel();
        btnAttack = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Game Monster Clicker");
        setPreferredSize(new java.awt.Dimension(1200, 700));

        lblMonsterName.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblMonsterName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMonsterName.setText("Monster Name");

        txtAsciiDisplay.setColumns(20);
        jScrollPane1.setViewportView(txtAsciiDisplay);

        pbMonsterHp.setStringPainted(true);

        lblHpInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHpInfo.setText("HP: 25 / 25");

        btnAttack.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAttack.setText("SERANG!");
        btnAttack.addActionListener(this::btnAttackActionPerformed);

        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnSave.setText("💾 SAVE GAME");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        btnBack.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnBack.setText("⬅ KEMBALI KE MENU SAVE");
        btnBack.addActionListener(this::btnBackActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMonsterName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                    .addComponent(pbMonsterHp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblHpInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAttack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnSave, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBack, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)))
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblMonsterName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pbMonsterHp, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblHpInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnAttack, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Event Handler untuk Tombol Serang / Hit
     */
    private void btnAttackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAttackActionPerformed
        // Cegah klik berlebih jika monster sudah kalah / dalam transisi pergantian
        if (isMonsterDefeated || currentHp <= 0) {
            return;
        }

        // 1. Kurangi HP monster sesuai damage (strength) saat ini
        currentHp -= strength;
        if (currentHp < 0) currentHp = 0;
        clickCount++;

        // 2. Perbarui status visual HP dan klik
        pbMonsterHp.setValue(currentHp);
        lblHpInfo.setText("HP: " + currentHp + " / " + maxHp + "    |    Damage: " + strength + " (x2 Tiap Menang)    |    Total Klik: " + clickCount);

        // 3. Tampilkan Visual Efek Kena Serang di tengah
        txtAsciiDisplay.setText(centerAscii(asciiHit));

        // 4. Jika monster kalah karena serangan ini
        if (currentHp <= 0) {
            // KUNCI LANGSUNG! Mencegah race condition yang membuat monster loncat (1, 3, 5)
            isMonsterDefeated = true;
            btnAttack.setEnabled(false);

            // Berikan efek buff damage *2 untuk monster berikutnya
            strength *= 2;

            Timer hitTimer = new Timer(250, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    txtAsciiDisplay.setText(centerAscii(
                        "=== " + monsterName.toUpperCase() + " DIKALAHKAN! ===\n\n" +
                        "★ EFEK BONUS DIAKTIFKAN: DAMAGE BERIKUTNYA MENJADI " + strength + " (2X LIPAT)! ★"
                    ));

                    // Beri jeda 1 detik lalu muat monster berikutnya secara berurutan
                    Timer nextTimer = new Timer(1000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            monsterId++; // Maju 1 langkah teratur: 1 -> 2 -> 3 -> 4 dst
                            boolean adaLagi = loadMonsterFromDB(monsterId);
                            if (!adaLagi) {
                                txtAsciiDisplay.setText(centerAscii("=== SELAMAT! ANDA TELAH MENGALAHKAN SEMUA MONSTER! ==="));
                                lblMonsterName.setText("VICTORY! SELURUH MONSTER TELAH DIKALAHKAN");
                                btnAttack.setEnabled(false);
                            }
                            // Simpan progres secara otomatis di background
                            saveGameProgress(false);
                        }
                    });
                    nextTimer.setRepeats(false);
                    nextTimer.start();
                }
            });
            hitTimer.setRepeats(false);
            hitTimer.start();
        } else {
            // Jika monster masih hidup, kembalikan tampilan normal setelah 250ms
            Timer hitTimer = new Timer(250, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentHp > 0 && !isMonsterDefeated) {
                        txtAsciiDisplay.setText(centerAscii(asciiNormal));
                    }
                }
            });
            hitTimer.setRepeats(false);
            hitTimer.start();
        }
    }//GEN-LAST:event_btnAttackActionPerformed

    /**
     * Tombol Save Game: Memperbarui Save data player ke database
     */
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveGameProgress(true);
    }//GEN-LAST:event_btnSaveActionPerformed

    /**
     * Tombol Back: Konfirmasi simpan data lalu kembali ke menu Save
     */
    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        int pilihan = JOptionPane.showConfirmDialog(this, 
            "Apakah Anda ingin menyimpan progres game terlebih dahulu sebelum kembali ke menu save?", 
            "Simpan & Kembali", 
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (pilihan == JOptionPane.YES_OPTION) {
            saveGameProgress(true);
            new Save().setVisible(true);
            this.dispose();
        } else if (pilihan == JOptionPane.NO_OPTION) {
            new Save().setVisible(true);
            this.dispose();
        }
    }//GEN-LAST:event_btnBackActionPerformed

    /**
     * Method main untuk menjalankan form Game secara langsung
     * @param args argumen baris perintah
     */
    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new Game(1).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAttack;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnSave;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblHpInfo;
    private javax.swing.JLabel lblMonsterName;
    private javax.swing.JProgressBar pbMonsterHp;
    private javax.swing.JTextArea txtAsciiDisplay;
    // End of variables declaration//GEN-END:variables
}
