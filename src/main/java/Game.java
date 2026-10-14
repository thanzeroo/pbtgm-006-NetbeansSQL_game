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
    private int strength = 1;       // Damage per klik
    private int coins = 0;          // Koin untuk membeli upgrade damage
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
        getContentPane().setBackground(new java.awt.Color(0, 51, 51));

        // Format font monospaced tebal untuk tampilan ASCII art
        txtAsciiDisplay.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        txtAsciiDisplay.setEditable(false);

        // Muat data Save player dari database
        loadPlayerData(pId);
    }

    /**
     * Memuat data player yang tersimpan dari database 'aplikasi_game_clicker'
     */
    public void loadPlayerData(int pId) {
        this.currentPlayerId = pId;
        String sql = "SELECT id, player_name, strength, stat_points, click_count, current_monster_id, monster_current_hp FROM players WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, pId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.currentPlayerName = rs.getString("player_name");
                    this.strength = rs.getInt("strength");
                    if (this.strength < 1) this.strength = 1;
                    this.coins = rs.getInt("stat_points");
                    if (this.coins < 0) this.coins = 0;
                    this.clickCount = rs.getInt("click_count");
                    int savedMonsterId = rs.getInt("current_monster_id");
                    int savedHp = rs.getInt("monster_current_hp");

                    // Muat data monster
                    boolean loaded = loadMonsterFromDB(savedMonsterId);
                    if (loaded && savedHp > 0 && savedHp <= maxHp) {
                        this.currentHp = savedHp;
                    }
                    updateStatusDisplay();
                    updateShopDisplay();
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat player: " + e.getMessage());
        }

        // Fallback jika player belum ada
        loadMonsterFromDB(1);
        updateStatusDisplay();
        updateShopDisplay();
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
        
        int totalCols = 100;
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
     * Memperbarui teks info HP, Damage, Koin, dan nama player
     */
    private void updateStatusDisplay() {
        lblMonsterName.setText(monsterName + "  (Monster #" + monsterId + ")  -  Pemain: " + currentPlayerName);
        pbMonsterHp.setMaximum(maxHp);
        pbMonsterHp.setValue(currentHp);
        lblHpInfo.setText("HP: " + currentHp + " / " + maxHp + "    |    Damage: " + strength + "    |    Koin: " + coins + "    |    Total Klik: " + clickCount);
    }

    /**
     * Memperbarui tampilan widget di sidebar shop
     */
    private void updateShopDisplay() {
        if (lblCoins != null) {
            lblCoins.setText("🪙 Koin: " + coins);
        }
        if (lblCurrentDmg != null) {
            lblCurrentDmg.setText("🗡️ Damage: " + strength);
        }
    }

    /**
     * Logika Pembelian Peningkatan Damage menggunakan Koin
     */
    private void buyUpgrade(String upgradeName, int damageBonus, int cost) {
        if (coins >= cost) {
            coins -= cost;
            strength += damageBonus;
            updateStatusDisplay();
            updateShopDisplay();
            JOptionPane.showMessageDialog(this, 
                "Berhasil membeli " + upgradeName + "!\n\n" +
                "• Bonus Damage: +" + damageBonus + "\n" +
                "• Total Damage Sekarang: " + strength + "\n" +
                "• Sisa Koin: " + coins,
                "Upgrade Sukses", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Koin Anda tidak cukup untuk " + upgradeName + "!\n\n" +
                "• Biaya Upgrade: " + cost + " Koin\n" +
                "• Koin Anda Saat Ini: " + coins + " Koin\n" +
                "• Kurang: " + (cost - coins) + " Koin lagi\n\n" +
                "Serang monster untuk mengumpulkan koin!", 
                "Koin Tidak Cukup", 
                JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * UPDATE: Menyimpan progres game ke tabel 'players' di database
     */
    public void saveGameProgress(boolean showNotification) {
        String sql = "UPDATE players SET current_monster_id = ?, monster_current_hp = ?, strength = ?, stat_points = ?, click_count = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, monsterId);
            ps.setInt(2, currentHp);
            ps.setInt(3, strength);
            ps.setInt(4, coins);
            ps.setInt(5, clickCount);
            ps.setInt(6, currentPlayerId);
            
            int updated = ps.executeUpdate();
            if (updated > 0 && showNotification) {
                JOptionPane.showMessageDialog(this, 
                    "Progres Game Berhasil Disimpan!\n\n" +
                    "• Pemain: " + currentPlayerName + "\n" +
                    "• Monster Terakhir: " + monsterName + " (ID: " + monsterId + ")\n" +
                    "• Sisa HP: " + currentHp + " / " + maxHp + "\n" +
                    "• Damage (Strength): " + strength + "\n" +
                    "• Koin: " + coins + "\n" +
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

        panelSidebar = new javax.swing.JPanel();
        lblShopTitle = new javax.swing.JLabel();
        lblCoins = new javax.swing.JLabel();
        lblCurrentDmg = new javax.swing.JLabel();
        btnUpgrade1 = new javax.swing.JButton();
        btnUpgrade2 = new javax.swing.JButton();
        btnUpgrade3 = new javax.swing.JButton();
        btnUpgrade4 = new javax.swing.JButton();
        btnUpgrade5 = new javax.swing.JButton();
        lblSidebarTip = new javax.swing.JLabel();
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

        panelSidebar.setBackground(new java.awt.Color(0, 40, 40));

        lblShopTitle.setFont(new java.awt.Font("JetBrains Mono", 1, 16)); // NOI18N
        lblShopTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblShopTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblShopTitle.setText("UPGRADE DAMAGE");

        lblCoins.setFont(new java.awt.Font("JetBrains Mono", 1, 15)); // NOI18N
        lblCoins.setForeground(new java.awt.Color(255, 215, 0));
        lblCoins.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCoins.setText("🦙 Koin: 0");

        lblCurrentDmg.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        lblCurrentDmg.setForeground(new java.awt.Color(200, 255, 200));
        lblCurrentDmg.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurrentDmg.setText("🗡️ Damage: 1");

        btnUpgrade1.setBackground(new java.awt.Color(0, 102, 0));
        btnUpgrade1.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        btnUpgrade1.setForeground(new java.awt.Color(255, 255, 255));
        btnUpgrade1.setText("Awam (+1) | 10 Koin");
        btnUpgrade1.addActionListener(this::btnUpgrade1ActionPerformed);

        btnUpgrade2.setBackground(new java.awt.Color(0, 102, 0));
        btnUpgrade2.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        btnUpgrade2.setForeground(new java.awt.Color(255, 255, 255));
        btnUpgrade2.setText("Pemula (+3) | 25 Koin");
        btnUpgrade2.addActionListener(this::btnUpgrade2ActionPerformed);

        btnUpgrade3.setBackground(new java.awt.Color(0, 102, 0));
        btnUpgrade3.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        btnUpgrade3.setForeground(new java.awt.Color(255, 255, 255));
        btnUpgrade3.setText("Standar (+5) | 50 Koin");
        btnUpgrade3.addActionListener(this::btnUpgrade3ActionPerformed);

        btnUpgrade4.setBackground(new java.awt.Color(0, 102, 0));
        btnUpgrade4.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        btnUpgrade4.setForeground(new java.awt.Color(255, 255, 255));
        btnUpgrade4.setText("Luar Biasa (+7) | 100 Koin");
        btnUpgrade4.addActionListener(this::btnUpgrade4ActionPerformed);

        btnUpgrade5.setBackground(new java.awt.Color(0, 102, 0));
        btnUpgrade5.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        btnUpgrade5.setForeground(new java.awt.Color(255, 255, 255));
        btnUpgrade5.setText("Dewa (+9) | 200 Koin");
        btnUpgrade5.addActionListener(this::btnUpgrade5ActionPerformed);

        lblSidebarTip.setFont(new java.awt.Font("JetBrains Mono", 0, 11)); // NOI18N
        lblSidebarTip.setForeground(new java.awt.Color(160, 210, 190));
        lblSidebarTip.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSidebarTip.setText("<html><center>Tips: Pukul monster<br>dapat +1 Koin!</center></html>");

        javax.swing.GroupLayout panelSidebarLayout = new javax.swing.GroupLayout(panelSidebar);
        panelSidebar.setLayout(panelSidebarLayout);
        panelSidebarLayout.setHorizontalGroup(
            panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSidebarLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblShopTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblCoins, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblCurrentDmg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade4, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                    .addComponent(btnUpgrade5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSidebarTip))
                .addGap(14, 14, 14))
        );
        panelSidebarLayout.setVerticalGroup(
            panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSidebarLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblShopTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(lblCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(lblCurrentDmg, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(btnUpgrade1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnUpgrade2, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnUpgrade3, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnUpgrade4, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(btnUpgrade5, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(lblSidebarTip, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(130, Short.MAX_VALUE))
        );

        lblMonsterName.setFont(new java.awt.Font("JetBrains Mono", 1, 18)); // NOI18N
        lblMonsterName.setForeground(new java.awt.Color(255, 255, 255));
        lblMonsterName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMonsterName.setText("Monster Name");

        jScrollPane1.setBackground(new java.awt.Color(0, 30, 30));

        txtAsciiDisplay.setBackground(new java.awt.Color(0, 30, 30));
        txtAsciiDisplay.setColumns(20);
        txtAsciiDisplay.setFont(new java.awt.Font("JetBrains Mono", 1, 13)); // NOI18N
        txtAsciiDisplay.setForeground(new java.awt.Color(0, 255, 128));
        jScrollPane1.setViewportView(txtAsciiDisplay);

        pbMonsterHp.setBackground(new java.awt.Color(0, 30, 30));
        pbMonsterHp.setFont(new java.awt.Font("JetBrains Mono", 1, 12)); // NOI18N
        pbMonsterHp.setForeground(new java.awt.Color(0, 153, 76));
        pbMonsterHp.setStringPainted(true);

        lblHpInfo.setFont(new java.awt.Font("JetBrains Mono", 1, 13)); // NOI18N
        lblHpInfo.setForeground(new java.awt.Color(255, 255, 255));
        lblHpInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHpInfo.setText("HP: 25 / 25");

        btnAttack.setBackground(new java.awt.Color(0, 102, 0));
        btnAttack.setFont(new java.awt.Font("JetBrains Mono", 1, 16)); // NOI18N
        btnAttack.setForeground(new java.awt.Color(255, 255, 255));
        btnAttack.setText("⚔️ SERANG!");
        btnAttack.addActionListener(this::btnAttackActionPerformed);

        btnSave.setBackground(new java.awt.Color(0, 102, 0));
        btnSave.setFont(new java.awt.Font("JetBrains Mono", 1, 13)); // NOI18N
        btnSave.setForeground(new java.awt.Color(255, 255, 255));
        btnSave.setText("💾 SAVE GAME");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        btnBack.setBackground(new java.awt.Color(0, 102, 0));
        btnBack.setFont(new java.awt.Font("JetBrains Mono", 1, 13)); // NOI18N
        btnBack.setForeground(new java.awt.Color(255, 255, 255));
        btnBack.setText("⬅ KEMBALI KE MENU SAVE");
        btnBack.addActionListener(this::btnBackActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(panelSidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMonsterName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
                    .addComponent(pbMonsterHp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblHpInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAttack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnSave, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(btnBack, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)))
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelSidebar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
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
                            .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpgrade1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpgrade1ActionPerformed
        buyUpgrade("Peningkatan Awam", 1, 10);
    }//GEN-LAST:event_btnUpgrade1ActionPerformed

    private void btnUpgrade2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpgrade2ActionPerformed
        buyUpgrade("Peningkatan Pemula", 3, 25);
    }//GEN-LAST:event_btnUpgrade2ActionPerformed

    private void btnUpgrade3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpgrade3ActionPerformed
        buyUpgrade("Peningkatan Standar", 5, 50);
    }//GEN-LAST:event_btnUpgrade3ActionPerformed

    private void btnUpgrade4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpgrade4ActionPerformed
        buyUpgrade("Peningkatan Luar Biasa", 7, 100);
    }//GEN-LAST:event_btnUpgrade4ActionPerformed

    private void btnUpgrade5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpgrade5ActionPerformed
        buyUpgrade("Peningkatan Dewa", 9, 200);
    }//GEN-LAST:event_btnUpgrade5ActionPerformed

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

        // 2. Setiap pukulan memberikan koin (+1 Koin)
        coins += 1;

        // 3. Perbarui status visual HP, koin, damage, dan total klik
        pbMonsterHp.setValue(currentHp);
        updateStatusDisplay();
        updateShopDisplay();

        // 4. Tampilkan Visual Efek Kena Serang di tengah
        txtAsciiDisplay.setText(centerAscii(asciiHit));

        // 5. Jika monster kalah karena serangan ini
        if (currentHp <= 0) {
            // KUNCI LANGSUNG! Mencegah race condition yang membuat monster loncat
            isMonsterDefeated = true;
            btnAttack.setEnabled(false);

            // Ganti efek multiple: beri hadiah koin kemenangan untuk monster mati
            int bonusKemenangan = 20;
            coins += bonusKemenangan;
            updateShopDisplay();

            Timer hitTimer = new Timer(250, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    txtAsciiDisplay.setText(centerAscii(
                        "=== " + monsterName.toUpperCase() + " DIKALAHKAN! ===\n\n" +
                        "★ HADIAH KEMENANGAN: +" + bonusKemenangan + " KOIN DIDAPATKAN! ★\n\n" +
                        "Gunakan koin di sidebar kiri untuk meningkatkan damage!"
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
    private javax.swing.JButton btnUpgrade1;
    private javax.swing.JButton btnUpgrade2;
    private javax.swing.JButton btnUpgrade3;
    private javax.swing.JButton btnUpgrade4;
    private javax.swing.JButton btnUpgrade5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCoins;
    private javax.swing.JLabel lblCurrentDmg;
    private javax.swing.JLabel lblHpInfo;
    private javax.swing.JLabel lblMonsterName;
    private javax.swing.JLabel lblShopTitle;
    private javax.swing.JLabel lblSidebarTip;
    private javax.swing.JPanel panelSidebar;
    private javax.swing.JProgressBar pbMonsterHp;
    private javax.swing.JTextArea txtAsciiDisplay;
    // End of variables declaration//GEN-END:variables
}
