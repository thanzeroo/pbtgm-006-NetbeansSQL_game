import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 * Game.java - Monster Clicker Game
 * Versi lengkap dengan:
 *  - Loading monster dari DB (ASCII baru dari monster.txt)
 *  - Sidebar: klik otomatis +2/+4, damage gacha, skill chain/thunder/income
 *  - Gacha sistem dengan koin
 *  - Damage chain (5x beruntun)
 *  - Income koin naik
 *  - Skill petir (setiap 10 klik)
 */
public class Game extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Game.class.getName());

    // ==========================================
    // VARIABEL STATE PLAYER & GAME
    // ==========================================
    private int currentPlayerId = 1;
    private String currentPlayerName = "Player";
    private int strength = 1;           // Damage per klik
    private int coins = 0;              // Koin
    private int clickCount = 0;         // Jumlah klik

    // ==========================================
    // SKILL & UPGRADE STATE
    // ==========================================
    private int autoClickLevel = 0;         // Level auto-klik (+2 per level)
    private int chainDamageLevel = 0;       // Level chain damage (5x beruntun)
    private double coinIncomeMultiplier = 1.0; // Multiplier income koin
    private int coinIncomeLevel = 0;        // Level income koin
    private int thunderLevel = 0;           // Level skill petir
    private int thunderCounter = 0;         // Hitung klik untuk petir
    private int gachaDamageBonus = 0;       // Bonus damage dari gacha
    private int gachaClickBonus = 0;        // Bonus klik otomatis per serang dari gacha

    // Auto-click timer
    private Timer autoClickTimer = null;

    // Chain damage state
    private boolean chainActive = false;
    private int chainCount = 0;
    private static final int CHAIN_MAX = 5;

    // ==========================================
    // VARIABEL STATE MONSTER
    // ==========================================
    private int monsterId = 1;
    private String monsterName = "Goblin";
    private int maxHp = 80;
    private int currentHp = 80;
    private String asciiNormal = "";
    private String asciiHit = "";
    private String effectDescription = "";

    // Kunci anti race-condition
    private boolean isMonsterDefeated = false;

    private final Random random = new Random();

    // ==========================================
    // KONSTRUKTOR
    // ==========================================
    public Game() { this(1); }

    public static Font getBrailleFont(int size) {
        String[] preferredFonts = {
            "Adwaita Mono",
            "Noto Sans Symbols 2",
            "Symbola",
            "DejaVu Sans Mono",
            "Segoe UI Symbol",
            "MS Gothic",
            Font.MONOSPACED,
            Font.DIALOG
        };
        for (String name : preferredFonts) {
            Font f = new Font(name, Font.PLAIN, size);
            if (f.canDisplay('\u2801')) {
                return f;
            }
        }
        try {
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (Font f : ge.getAllFonts()) {
                if (f.canDisplay('\u2801')) {
                    return f.deriveFont(Font.PLAIN, (float) size);
                }
            }
        } catch (Exception ignored) {}
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    public Game(int pId) {
        initComponents();
        setLocationRelativeTo(null);
        getContentPane().setBackground(new java.awt.Color(0, 51, 51));

        Font brailleFont = getBrailleFont(11);
        txtAsciiDisplay.setFont(brailleFont);
        txtAsciiDisplay.setLineWrap(false);
        txtAsciiDisplay.setEditable(false);
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        txtAsciiDisplay.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (asciiNormal != null && !asciiNormal.isEmpty() && currentHp > 0 && !isMonsterDefeated) {
                    renderAsciiToDisplay(asciiNormal);
                }
            }
        });

        loadPlayerData(pId);
    }

    // ==========================================
    // LOAD DATA PLAYER DARI DB
    // ==========================================
    public void loadPlayerData(int pId) {
        this.currentPlayerId = pId;
        // Coba baca kolom baru, fallback jika belum ada
        String sql = "SELECT id, player_name, strength, stat_points, click_count, current_monster_id, " +
                     "monster_current_hp FROM players WHERE id = ?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.currentPlayerName = rs.getString("player_name");
                    this.strength = Math.max(1, rs.getInt("strength"));
                    this.coins = Math.max(0, rs.getInt("stat_points"));
                    this.clickCount = rs.getInt("click_count");
                    int savedMonsterId = rs.getInt("current_monster_id");
                    int savedHp = rs.getInt("monster_current_hp");

                    // Coba baca kolom extra skill (mungkin belum ada)
                    try { this.autoClickLevel = rs.getInt("auto_click_level"); } catch (Exception ignored) {}
                    try { this.chainDamageLevel = rs.getInt("chain_damage_level"); } catch (Exception ignored) {}
                    try { this.coinIncomeLevel = rs.getInt("coin_income_level"); } catch (Exception ignored) {}
                    try { this.thunderLevel = rs.getInt("thunder_level"); } catch (Exception ignored) {}
                    try { this.thunderCounter = rs.getInt("thunder_counter"); } catch (Exception ignored) {}
                    try { this.gachaDamageBonus = rs.getInt("gacha_damage"); } catch (Exception ignored) {}
                    try { this.gachaClickBonus = rs.getInt("gacha_clicks"); } catch (Exception ignored) {}

                    // Hitung income multiplier dari level
                    this.coinIncomeMultiplier = 1.0 + (coinIncomeLevel * 0.2);

                    boolean loaded = loadMonsterFromDB(savedMonsterId);
                    if (loaded && savedHp > 0 && savedHp <= maxHp) {
                        this.currentHp = savedHp;
                    }
                    updateStatusDisplay();
                    updateShopDisplay();

                    // Aktifkan auto-click jika ada
                    applyAutoClickTimer();
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat player: " + e.getMessage());
        }
        loadMonsterFromDB(1);
        updateStatusDisplay();
        updateShopDisplay();
    }

    // ==========================================
    // RENDER ASCII DISPLAY (Otomatis Scaler Font, Anti Terpotong & Presisi di Tengah)
    // ==========================================
    public void renderAsciiToDisplay(String text) {
        if (text == null || text.trim().isEmpty()) {
            txtAsciiDisplay.setText("");
            return;
        }

        text = text.replace("\r", "");
        String[] lines = text.split("\n");
        int lineCount = lines.length;
        int maxLen = 0;
        for (String line : lines) {
            if (line.length() > maxLen) maxLen = line.length();
        }

        // Ukuran area tampilan aktual (viewport)
        int areaWidth = txtAsciiDisplay.getWidth();
        int areaHeight = txtAsciiDisplay.getHeight();
        if (areaWidth <= 0 || areaHeight <= 0) {
            if (jScrollPane1 != null && jScrollPane1.getViewport() != null) {
                java.awt.Dimension viewSize = jScrollPane1.getViewport().getSize();
                if (viewSize.width > 0) areaWidth = viewSize.width;
                if (viewSize.height > 0) areaHeight = viewSize.height;
            }
        }
        if (areaWidth <= 0) areaWidth = 950;
        if (areaHeight <= 0) areaHeight = 420;

        // Berikan margin pengaman agar tidak mepet atau terpotong oleh border
        int targetWidth = areaWidth - 24;
        int targetHeight = areaHeight - 16;

        // Cari ukuran font dari 14pt turun ke 5pt yang 100% muat vertikal & horizontal
        Font bestFont = getBrailleFont(5);
        int bestPadTop = 0;
        int bestPadLeft = 0;

        for (int sz = 14; sz >= 5; sz--) {
            Font f = getBrailleFont(sz);
            java.awt.FontMetrics fm = txtAsciiDisplay.getFontMetrics(f);
            int totalH = fm.getHeight() * lineCount;
            int totalW = fm.charWidth(' ') * maxLen;

            if (totalH <= targetHeight && totalW <= targetWidth) {
                bestFont = f;
                bestPadTop = Math.max(0, (targetHeight - totalH) / (2 * fm.getHeight()));
                bestPadLeft = Math.max(0, (targetWidth - totalW) / (2 * fm.charWidth(' ')));
                break;
            }
        }

        txtAsciiDisplay.setFont(bestFont);

        String padding = " ".repeat(bestPadLeft);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bestPadTop; i++) {
            sb.append("\n");
        }
        for (String line : lines) {
            sb.append(padding).append(line).append("\n");
        }

        txtAsciiDisplay.setText(sb.toString());
        txtAsciiDisplay.setCaretPosition(0);
    }

    private String centerAscii(String text) {
        renderAsciiToDisplay(text);
        return txtAsciiDisplay.getText();
    }

    // ==========================================
    // LOAD MONSTER DARI DB
    // ==========================================
    public boolean loadMonsterFromDB(int id) {
        String sql = "SELECT * FROM monsters WHERE id = ?";
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
                    String mEffect = "";
                    try { mEffect = rs.getString("effect_description"); } catch (Exception ignored) {}
                    loadMonsterData(mId, mName, mHp, mNormal, mHit, mEffect);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal membaca monster dari DB: " + e.getMessage());
        }
        return false;
    }

    public void loadMonsterData(int id, String name, int hp, String normal, String hit, String effect) {
        this.monsterId = id;
        this.monsterName = name;
        this.maxHp = hp;
        this.currentHp = hp;
        this.asciiNormal = normal;
        this.asciiHit = hit;
        this.effectDescription = (effect != null) ? effect : "";
        this.isMonsterDefeated = false;
        this.thunderCounter = 0;

        updateStatusDisplay();
        renderAsciiToDisplay(asciiNormal);
        btnAttack.setEnabled(true);

        // Tampilkan efek monster di label info
        if (lblEffectInfo != null && !effectDescription.isEmpty()) {
            lblEffectInfo.setText("<html><center><i>Efek: " + effectDescription + "</i></center></html>");
        }
    }

    // ==========================================
    // UPDATE DISPLAY
    // ==========================================
    private void updateStatusDisplay() {
        lblMonsterName.setText("[MONSTER] " + monsterName.toUpperCase() + "  (#" + monsterId + ")  |  Pemain: " + currentPlayerName);
        pbMonsterHp.setMaximum(maxHp);
        pbMonsterHp.setValue(currentHp);

        int totalDmg = strength + gachaDamageBonus;
        int clicksPerPress = 1 + (autoClickLevel * 2) + gachaClickBonus;
        String chainStr = chainDamageLevel > 0 ? " | Chain Lv." + chainDamageLevel : "";
        String thunderStr = thunderLevel > 0 ? " | Petir Lv." + thunderLevel + " (" + thunderCounter + "/10)" : "";
        lblHpInfo.setText("<html><center>HP: " + currentHp + " / " + maxHp +
                "  |  DMG: " + totalDmg +
                "  |  Klik: x" + clicksPerPress +
                "  |  Koin: " + coins +
                chainStr + thunderStr +
                "</center></html>");
    }

    private void updateShopDisplay() {
        if (lblCoins != null) lblCoins.setText("Koin: " + coins);
        if (lblCurrentDmg != null) {
            int totalDmg = strength + gachaDamageBonus;
            lblCurrentDmg.setText("DMG: " + totalDmg + (gachaDamageBonus > 0 ? " (+" + gachaDamageBonus + " gacha)" : ""));
        }
        // Update multi/auto click label
        if (lblAutoClick != null) {
            int clicksPerPress = 1 + (autoClickLevel * 2) + gachaClickBonus;
            lblAutoClick.setText("Multi-Klik: x" + clicksPerPress + " per serang");
        }
    }

    // ==========================================
    // BELI UPGRADE DAMAGE (FIXED, bukan +5/7/9)
    // ==========================================
    private void buyUpgrade(String upgradeName, int damageBonus, int cost) {
        if (coins >= cost) {
            coins -= cost;
            strength += damageBonus;
            updateStatusDisplay();
            updateShopDisplay();
            JOptionPane.showMessageDialog(this,
                "✅ Berhasil membeli " + upgradeName + "!\n\n" +
                "• Bonus Damage: +" + damageBonus + "\n" +
                "• Total Damage: " + (strength + gachaDamageBonus) + "\n" +
                "• Sisa Koin: " + coins,
                "Upgrade Sukses", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup untuk " + upgradeName + "!\n\n" +
                "• Biaya: " + cost + " Koin\n" +
                "• Koin kamu: " + coins + " Koin\n" +
                "• Kurang: " + (cost - coins) + " Koin",
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ==========================================
    // GACHA SYSTEM (KOIN -> DAMAGE 1 3 5, OTOMATIS PENCET 2 4 6, BISA ZONK)
    // ==========================================
    private void doGacha(int gachaCost, int tier) {
        if (coins < gachaCost) {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup untuk Gacha!\n\n• Biaya Gacha: " + gachaCost + " Koin\n• Koin kamu: " + coins,
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        coins -= gachaCost;

        // Roll Zonk atau Hadiah
        // Tier 1: 35% Zonk
        // Tier 2: 25% Zonk
        // Tier 3: 15% Zonk
        int zonkThreshold = (tier == 1) ? 35 : ((tier == 2) ? 25 : 15);
        int roll = random.nextInt(100);

        if (roll < zonkThreshold) {
            // ZONK!
            JOptionPane.showMessageDialog(this,
                "💨 ZONK!\n\nSayang sekali, kamu tidak beruntung kali ini...\nCoba keberuntunganmu lagi!\n\nSisa Koin: " + coins,
                "🎰 Hasil Gacha - Zonk", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Hadiah: pilih antara Damage (1, 3, 5) atau Otomatis Klik (2, 4, 6)
            boolean isDamageReward = random.nextBoolean();
            if (isDamageReward) {
                // Damage: tier 1 (1 atau 3), tier 2 (3 atau 5), tier 3 (5)
                int dmgGain;
                if (tier == 1) {
                    dmgGain = random.nextBoolean() ? 1 : 3;
                } else if (tier == 2) {
                    dmgGain = random.nextBoolean() ? 3 : 5;
                } else {
                    dmgGain = 5;
                }
                gachaDamageBonus += dmgGain;
                JOptionPane.showMessageDialog(this,
                    "🎉 JACKPOT DAMAGE! 🎉\n\n" +
                    "Kamu mendapatkan bonus permanen: +" + dmgGain + " Damage!\n" +
                    "• Total Bonus Gacha Damage: +" + gachaDamageBonus + "\n" +
                    "• Total Damage Karakter: " + (strength + gachaDamageBonus) + "\n\n" +
                    "Sisa Koin: " + coins,
                    "🎰 Hasil Gacha - Sukses!", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Otomatis Klik per Serang: tier 1 (2), tier 2 (2 atau 4), tier 3 (4 atau 6)
                int clickGain;
                if (tier == 1) {
                    clickGain = 2;
                } else if (tier == 2) {
                    clickGain = random.nextBoolean() ? 2 : 4;
                } else {
                    clickGain = random.nextBoolean() ? 4 : 6;
                }
                gachaClickBonus += clickGain;
                int totalPerPress = 1 + (autoClickLevel * 2) + gachaClickBonus;
                JOptionPane.showMessageDialog(this,
                    "⚡ JACKPOT OTOMATIS KLIK! ⚡\n\n" +
                    "Kamu mendapatkan bonus otomatis: +" + clickGain + " Klik per serang!\n" +
                    "• Total Bonus Gacha Klik: +" + gachaClickBonus + "\n" +
                    "• Sekarang 1x klik tombol SERANG memicu " + totalPerPress + " pukulan!\n\n" +
                    "Sisa Koin: " + coins,
                    "🎰 Hasil Gacha - Sukses!", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        updateStatusDisplay();
        updateShopDisplay();
    }

    // ==========================================
    // MULTI-KLIK SYSTEM (+2, +4 KLIK TAMBAHAN PER KLIK)
    // ==========================================
    private void buyAutoClick(int bonusPerClick, int cost, int level) {
        if (autoClickLevel >= level) {
            JOptionPane.showMessageDialog(this, "Kamu sudah memiliki upgrade klik ini!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (coins < cost) {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup!\n• Biaya: " + cost + " Koin\n• Koin kamu: " + coins,
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        coins -= cost;
        autoClickLevel = level;
        applyAutoClickTimer();
        updateStatusDisplay();
        updateShopDisplay();
        int totalPerPress = 1 + (autoClickLevel * 2) + gachaClickBonus;
        JOptionPane.showMessageDialog(this,
            "✅ Klik Tambahan +" + (level * 2) + " Aktif!\n" +
            "Setiap 1 kali klik tombol SERANG sekarang memicu " + totalPerPress + " pukulan!\n" +
            "Sisa Koin: " + coins,
            "Upgrade Klik Berhasil", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applyAutoClickTimer() {
        if (autoClickTimer != null) {
            autoClickTimer.stop();
            autoClickTimer = null;
        }
        if (autoClickLevel > 0) {
            int interval = 1000; // 1 detik
            autoClickTimer = new Timer(interval, e -> {
                if (!isMonsterDefeated && currentHp > 0) {
                    // Auto-klik sebanyak autoClickLevel * 2 kali efeknya
                    int autoDmg = (strength + gachaDamageBonus) * (autoClickLevel * 2);
                    doAttackLogic(autoDmg, autoClickLevel * 2, false);
                }
            });
            autoClickTimer.setRepeats(true);
            autoClickTimer.start();
        }
    }

    // ==========================================
    // CHAIN DAMAGE SYSTEM
    // ==========================================
    private void buyChainDamage(int cost, int level) {
        if (chainDamageLevel >= level) {
            JOptionPane.showMessageDialog(this, "Level ini sudah dimiliki!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (coins < cost) {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup!\n• Biaya: " + cost + " Koin\n• Koin kamu: " + coins,
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        coins -= cost;
        chainDamageLevel = level;
        updateStatusDisplay();
        updateShopDisplay();
        JOptionPane.showMessageDialog(this,
            "✅ Chain Damage Lv." + level + " aktif!\nSetiap serangan menghantam " + CHAIN_MAX + " kali berturut-turut!\nDamage chain naik tiap level!\nSisa Koin: " + coins,
            "Chain Damage Aktif", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // COIN INCOME SYSTEM
    // ==========================================
    private void buyCoinIncome(int cost, int level) {
        if (coinIncomeLevel >= level) {
            JOptionPane.showMessageDialog(this, "Level ini sudah dimiliki!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (coins < cost) {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup!\n• Biaya: " + cost + " Koin\n• Koin kamu: " + coins,
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        coins -= cost;
        coinIncomeLevel = level;
        coinIncomeMultiplier = 1.0 + (level * 0.2);
        updateStatusDisplay();
        updateShopDisplay();
        JOptionPane.showMessageDialog(this,
            "✅ Income Koin Lv." + level + " aktif!\nSetiap serangan dapat x" + String.format("%.1f", coinIncomeMultiplier) + " koin!\nSisa Koin: " + coins,
            "Income Koin Naik", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // THUNDER SKILL SYSTEM
    // ==========================================
    private void buyThunder(int cost, int level) {
        if (thunderLevel >= level) {
            JOptionPane.showMessageDialog(this, "Level ini sudah dimiliki!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (coins < cost) {
            JOptionPane.showMessageDialog(this,
                "❌ Koin tidak cukup!\n• Biaya: " + cost + " Koin\n• Koin kamu: " + coins,
                "Koin Tidak Cukup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        coins -= cost;
        thunderLevel = level;
        updateStatusDisplay();
        updateShopDisplay();
        int thunderDmg = strength * 5 * level;
        JOptionPane.showMessageDialog(this,
            "✅ Skill Petir Lv." + level + " aktif!\nSetiap 10 serangan, petir menghantam monster!\nDamage petir: " + thunderDmg + " (=" + strength + " x 5 x lv." + level + ")\nSisa Koin: " + coins,
            "⚡ Skill Petir Aktif", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // LOGIKA SERANGAN INTI (dipanggil baik oleh tombol maupun auto-klik)
    // ==========================================
    private void doAttackLogic(int dmg, int clicksToAdd, boolean showHitEffect) {
        if (isMonsterDefeated || currentHp <= 0) return;

        // Thunder check (hitung dari manual click saja)
        if (clicksToAdd > 0) {
            thunderCounter += clicksToAdd;
        }

        // Hitung damage: chain jika aktif
        int totalDmg = 0;
        if (chainDamageLevel > 0) {
            // Chain: 5x berturut-turut, masing-masing damage naik +chainDamageLevel*10%
            for (int i = 0; i < CHAIN_MAX; i++) {
                double multiplier = 1.0 + (i * chainDamageLevel * 0.1);
                totalDmg += (int)(dmg * multiplier);
            }
        } else {
            totalDmg = dmg;
        }

        // --- EFEK KHUSUS MONSTER ---
        String monsterEffectNote = "";
        if (monsterId == 3 && currentHp < (maxHp / 2)) {
            // Skeleton: Tulang retak saat HP < 50%, damage x1.5!
            totalDmg = (int)(totalDmg * 1.5);
            monsterEffectNote = "💥 [Kutukan Tulang: DMG x1.5!] ";
        } else if (monsterId == 4 && (clickCount % 5 == 0)) {
            // Ghost: Menghindar setiap 5 serangan
            totalDmg = 0;
            monsterEffectNote = "👻 [Ghost Menghindar! Serangan Miss!] ";
        } else if (monsterId == 9) {
            // Golem: Kulit batu kurangi damage 20%
            totalDmg = Math.max(1, (int)(totalDmg * 0.8));
            monsterEffectNote = "🛡 [Armor Batu Golem Menahan Damage!] ";
        }

        // Thunder damage jika counter >= 10
        int thunderDmgExtra = 0;
        if (thunderLevel > 0 && thunderCounter >= 10) {
            thunderDmgExtra = strength * 5 * thunderLevel;
            thunderCounter = 0;
        }

        totalDmg += thunderDmgExtra;

        // Kurangi HP
        currentHp -= totalDmg;
        if (currentHp < 0) currentHp = 0;
        clickCount += Math.max(1, clicksToAdd);

        // Regenerasi Vine Eater (Monster 8)
        if (monsterId == 8 && (clickCount % 4 == 0) && currentHp > 0 && currentHp < maxHp) {
            int heal = Math.max(1, (int)(maxHp * 0.03));
            currentHp = Math.min(maxHp, currentHp + heal);
            monsterEffectNote = "🌿 [Vine Eater Menyerap Darah: +" + heal + " HP!] ";
        }

        // Hitung koin yang didapat
        int baseCoins = 1;
        int coinGained = (int)(baseCoins * coinIncomeMultiplier);
        if (coinGained < 1) coinGained = 1;
        coins += coinGained;

        pbMonsterHp.setValue(currentHp);
        updateStatusDisplay();
        updateShopDisplay();

        if (showHitEffect) {
            // Tampilkan ASCII hit + thunder efek + efek monster
            String displayHit = asciiHit;
            if (thunderDmgExtra > 0) {
                displayHit = "[PETIR MENYAMBAR!] Damage Petir: +" + thunderDmgExtra + "\n\n" + asciiHit;
            }
            if (chainDamageLevel > 0) {
                displayHit = "[CHAIN x" + CHAIN_MAX + "!] " + displayHit;
            }
            if (!monsterEffectNote.isEmpty()) {
                displayHit = monsterEffectNote + "\n\n" + displayHit;
            }
            final String finalDisplayHit = displayHit;
            renderAsciiToDisplay(finalDisplayHit);
        }

        // Cek kematian monster
        if (currentHp <= 0) {
            isMonsterDefeated = true;
            btnAttack.setEnabled(false);
            if (autoClickTimer != null) autoClickTimer.stop();

            int bonusKemenangan = 20 + (monsterId * 5);
            // Goblin (id 1): 10% bonus koin ekstra
            if (monsterId == 1) {
                bonusKemenangan += (int)(bonusKemenangan * 0.1);
            }
            final int finalBonusKemenangan = bonusKemenangan;
            coins += bonusKemenangan;
            updateShopDisplay();

            Timer hitTimer = new Timer(300, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    renderAsciiToDisplay(
                        "===  " + monsterName.toUpperCase() + " DIKALAHKAN!  ===\n\n" +
                        "[+] HADIAH KEMENANGAN: +" + finalBonusKemenangan + " KOIN!\n\n" +
                        "Lanjut ke monster berikutnya..."
                    );
                    Timer nextTimer = new Timer(1200, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            monsterId++;
                            boolean adaLagi = loadMonsterFromDB(monsterId);
                            if (!adaLagi) {
                                renderAsciiToDisplay(
                                    "===  SELAMAT! SEMUA MONSTER TELAH DIKALAHKAN!  ===\n\n" +
                                    "Kamu adalah pemenangnya!");
                                lblMonsterName.setText("[VICTORY] SEMUA MONSTER TELAH DIKALAHKAN!");
                                btnAttack.setEnabled(false);
                            } else {
                                // Aktifkan kembali auto-click untuk monster baru
                                applyAutoClickTimer();
                            }
                            saveGameProgress(false);
                        }
                    });
                    nextTimer.setRepeats(false);
                    nextTimer.start();
                }
            });
            hitTimer.setRepeats(false);
            hitTimer.start();
        } else if (showHitEffect) {
            Timer hitTimer = new Timer(300, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentHp > 0 && !isMonsterDefeated) {
                        txtAsciiDisplay.setText(centerAscii(asciiNormal));
                        txtAsciiDisplay.setCaretPosition(0);
                    }
                }
            });
            hitTimer.setRepeats(false);
            hitTimer.start();
        }
    }

    // ==========================================
    // SAVE GAME PROGRESS
    // ==========================================
    public void saveGameProgress(boolean showNotification) {
        // Coba save dengan kolom baru dulu, fallback ke kolom lama
        String sql = "UPDATE players SET current_monster_id=?, monster_current_hp=?, strength=?, stat_points=?, " +
                     "click_count=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = dbconnection.configDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, monsterId);
            ps.setInt(2, currentHp);
            ps.setInt(3, strength);
            ps.setInt(4, coins);
            ps.setInt(5, clickCount);
            ps.setInt(6, currentPlayerId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Gagal save (kolom standar): " + e.getMessage());
        }

        // Coba save kolom extra skill (aman jika gagal)
        try (Connection conn = dbconnection.configDB()) {
            String sqlExtra = "UPDATE players SET auto_click_level=?, chain_damage_level=?, coin_income_level=?, " +
                              "thunder_level=?, thunder_counter=?, gacha_damage=?, gacha_clicks=? WHERE id=?";
            try (PreparedStatement ps2 = conn.prepareStatement(sqlExtra)) {
                ps2.setInt(1, autoClickLevel);
                ps2.setInt(2, chainDamageLevel);
                ps2.setInt(3, coinIncomeLevel);
                ps2.setInt(4, thunderLevel);
                ps2.setInt(5, thunderCounter);
                ps2.setInt(6, gachaDamageBonus);
                ps2.setInt(7, gachaClickBonus);
                ps2.setInt(8, currentPlayerId);
                ps2.executeUpdate();
            } catch (Exception exFallback) {
                // Fallback jika belum ada kolom gacha_clicks
                String sqlOld = "UPDATE players SET auto_click_level=?, chain_damage_level=?, coin_income_level=?, " +
                                "thunder_level=?, thunder_counter=?, gacha_damage=? WHERE id=?";
                try (PreparedStatement ps3 = conn.prepareStatement(sqlOld)) {
                    ps3.setInt(1, autoClickLevel);
                    ps3.setInt(2, chainDamageLevel);
                    ps3.setInt(3, coinIncomeLevel);
                    ps3.setInt(4, thunderLevel);
                    ps3.setInt(5, thunderCounter);
                    ps3.setInt(6, gachaDamageBonus);
                    ps3.setInt(7, currentPlayerId);
                    ps3.executeUpdate();
                }
            }
        } catch (Exception ignored) {
            // Kolom skill belum ada di DB lama - tidak masalah
        }

        if (showNotification) {
            JOptionPane.showMessageDialog(this,
                "💾 Progres Game Berhasil Disimpan!\n\n" +
                "• Pemain: " + currentPlayerName + "\n" +
                "• Monster: " + monsterName + " (#" + monsterId + ")\n" +
                "• HP Monster: " + currentHp + " / " + maxHp + "\n" +
                "• Damage: " + (strength + gachaDamageBonus) + "\n" +
                "• Koin: " + coins + "\n" +
                "• Total Klik: " + clickCount,
                "Simpan Berhasil", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==========================================
    // INIT COMPONENTS (Generated-style, aman untuk Netbeans)
    // ==========================================
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        panelSidebar = new javax.swing.JPanel();
        lblShopTitle = new javax.swing.JLabel();
        lblCoins = new javax.swing.JLabel();
        lblCurrentDmg = new javax.swing.JLabel();
        lblAutoClick = new javax.swing.JLabel();

        // === DAMAGE UPGRADE BUTTONS ===
        btnUpgrade1 = new javax.swing.JButton();
        btnUpgrade2 = new javax.swing.JButton();
        btnUpgrade3 = new javax.swing.JButton();

        // === AUTO CLICK BUTTONS ===
        btnAutoClick2 = new javax.swing.JButton();
        btnAutoClick4 = new javax.swing.JButton();

        // === SKILL BUTTONS ===
        btnChainDamage = new javax.swing.JButton();
        btnCoinIncome = new javax.swing.JButton();
        btnThunder = new javax.swing.JButton();

        // === GACHA SECTION ===
        lblGachaTitle = new javax.swing.JLabel();
        btnGacha1 = new javax.swing.JButton();
        btnGacha2 = new javax.swing.JButton();
        btnGacha3 = new javax.swing.JButton();

        lblSidebarTip = new javax.swing.JLabel();

        // === MAIN AREA ===
        lblMonsterName = new javax.swing.JLabel();
        lblEffectInfo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAsciiDisplay = new javax.swing.JTextArea();
        pbMonsterHp = new javax.swing.JProgressBar();
        lblHpInfo = new javax.swing.JLabel();
        btnAttack = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Monster Clicker Game");
        setPreferredSize(new java.awt.Dimension(1250, 720));

        // ---- SIDEBAR ----
        panelSidebar.setBackground(new java.awt.Color(0, 30, 40));

        lblShopTitle.setFont(new java.awt.Font("JetBrains Mono", 1, 14));
        lblShopTitle.setForeground(new java.awt.Color(255, 220, 50));
        lblShopTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblShopTitle.setText("[ UPGRADE SHOP ]");

        lblCoins.setFont(new java.awt.Font("JetBrains Mono", 1, 13));
        lblCoins.setForeground(new java.awt.Color(255, 215, 0));
        lblCoins.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCoins.setText("Koin: 0");

        lblCurrentDmg.setFont(new java.awt.Font("JetBrains Mono", 1, 11));
        lblCurrentDmg.setForeground(new java.awt.Color(200, 255, 200));
        lblCurrentDmg.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurrentDmg.setText("DMG: 1");

        lblAutoClick.setFont(new java.awt.Font("JetBrains Mono", 0, 10));
        lblAutoClick.setForeground(new java.awt.Color(180, 230, 255));
        lblAutoClick.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAutoClick.setText("Multi-Klik: x1 per serang");

        // --- Damage Upgrades ---
        btnUpgrade1.setBackground(new java.awt.Color(0, 100, 50));
        btnUpgrade1.setFont(new java.awt.Font("JetBrains Mono", 1, 11));
        btnUpgrade1.setForeground(java.awt.Color.WHITE);
        btnUpgrade1.setText("Pemula (+1) | 10 Koin");
        btnUpgrade1.addActionListener(this::btnUpgrade1ActionPerformed);

        btnUpgrade2.setBackground(new java.awt.Color(0, 100, 50));
        btnUpgrade2.setFont(new java.awt.Font("JetBrains Mono", 1, 11));
        btnUpgrade2.setForeground(java.awt.Color.WHITE);
        btnUpgrade2.setText("Prajurit (+3) | 25 Koin");
        btnUpgrade2.addActionListener(this::btnUpgrade2ActionPerformed);

        btnUpgrade3.setBackground(new java.awt.Color(0, 100, 50));
        btnUpgrade3.setFont(new java.awt.Font("JetBrains Mono", 1, 11));
        btnUpgrade3.setForeground(java.awt.Color.WHITE);
        btnUpgrade3.setText("Ksatria (+6) | 60 Koin");
        btnUpgrade3.addActionListener(this::btnUpgrade3ActionPerformed);

        // --- Multi Click (+2, +4) ---
        btnAutoClick2.setBackground(new java.awt.Color(20, 80, 130));
        btnAutoClick2.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnAutoClick2.setForeground(java.awt.Color.WHITE);
        btnAutoClick2.setText("+2 Klik/Serang | 80 Koin");
        btnAutoClick2.addActionListener(this::btnAutoClick2ActionPerformed);

        btnAutoClick4.setBackground(new java.awt.Color(10, 60, 120));
        btnAutoClick4.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnAutoClick4.setForeground(java.awt.Color.WHITE);
        btnAutoClick4.setText("+4 Klik/Serang | 200 Koin");
        btnAutoClick4.addActionListener(this::btnAutoClick4ActionPerformed);

        // --- Skill Buttons ---
        btnChainDamage.setBackground(new java.awt.Color(100, 20, 120));
        btnChainDamage.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnChainDamage.setForeground(java.awt.Color.WHITE);
        btnChainDamage.setText("Chain Lv1 | 150 Koin");
        btnChainDamage.addActionListener(this::btnChainDamageActionPerformed);

        btnCoinIncome.setBackground(new java.awt.Color(120, 80, 0));
        btnCoinIncome.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnCoinIncome.setForeground(java.awt.Color.WHITE);
        btnCoinIncome.setText("Income x1.2 | 100 Koin");
        btnCoinIncome.addActionListener(this::btnCoinIncomeActionPerformed);

        btnThunder.setBackground(new java.awt.Color(50, 50, 150));
        btnThunder.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnThunder.setForeground(java.awt.Color.WHITE);
        btnThunder.setText("Petir Lv1 | 300 Koin");
        btnThunder.addActionListener(this::btnThunderActionPerformed);

        // --- GACHA SECTION ---
        lblGachaTitle.setFont(new java.awt.Font("JetBrains Mono", 1, 12));
        lblGachaTitle.setForeground(new java.awt.Color(255, 150, 50));
        lblGachaTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblGachaTitle.setText("[ GACHA KOIN ]");

        btnGacha1.setBackground(new java.awt.Color(150, 80, 20));
        btnGacha1.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnGacha1.setForeground(java.awt.Color.WHITE);
        btnGacha1.setText("Gacha Basic | 30 Koin");
        btnGacha1.addActionListener(this::btnGacha1ActionPerformed);

        btnGacha2.setBackground(new java.awt.Color(140, 60, 10));
        btnGacha2.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnGacha2.setForeground(java.awt.Color.WHITE);
        btnGacha2.setText("Gacha Super | 75 Koin");
        btnGacha2.addActionListener(this::btnGacha2ActionPerformed);

        btnGacha3.setBackground(new java.awt.Color(130, 40, 0));
        btnGacha3.setFont(new java.awt.Font("JetBrains Mono", 1, 10));
        btnGacha3.setForeground(java.awt.Color.WHITE);
        btnGacha3.setText("Gacha Sultan | 150 Koin");
        btnGacha3.addActionListener(this::btnGacha3ActionPerformed);

        lblSidebarTip.setFont(new java.awt.Font("JetBrains Mono", 0, 10));
        lblSidebarTip.setForeground(new java.awt.Color(140, 200, 180));
        lblSidebarTip.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSidebarTip.setText("<html><center>💡 Tips: Pukul monster<br>untuk dapat Koin!</center></html>");

        // ---- Layout SIDEBAR ----
        javax.swing.GroupLayout panelSidebarLayout = new javax.swing.GroupLayout(panelSidebar);
        panelSidebar.setLayout(panelSidebarLayout);
        panelSidebarLayout.setHorizontalGroup(
            panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSidebarLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblShopTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblCoins, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblCurrentDmg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAutoClick, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpgrade3, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                    .addComponent(btnAutoClick2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAutoClick4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnChainDamage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCoinIncome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnThunder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblGachaTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGacha1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGacha2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGacha3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSidebarTip))
                .addGap(8, 8, 8))
        );
        panelSidebarLayout.setVerticalGroup(
            panelSidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSidebarLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lblShopTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(lblCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(lblCurrentDmg, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(lblAutoClick, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnUpgrade1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnUpgrade2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnUpgrade3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnAutoClick2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnAutoClick4, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnChainDamage, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnCoinIncome, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnThunder, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(lblGachaTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(btnGacha1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(btnGacha2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(btnGacha3, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(lblSidebarTip, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        // ---- MAIN AREA ----
        lblMonsterName.setFont(new java.awt.Font("JetBrains Mono", 1, 16));
        lblMonsterName.setForeground(new java.awt.Color(255, 255, 255));
        lblMonsterName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMonsterName.setText("★ Monster Name");

        lblEffectInfo.setFont(new java.awt.Font("JetBrains Mono", 0, 10));
        lblEffectInfo.setForeground(new java.awt.Color(200, 200, 100));
        lblEffectInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblEffectInfo.setText("<html><center><i>Efek monster...</i></center></html>");

        jScrollPane1.setBackground(new java.awt.Color(0, 30, 30));
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        txtAsciiDisplay.setBackground(new java.awt.Color(0, 30, 30));
        txtAsciiDisplay.setColumns(20);
        txtAsciiDisplay.setFont(getBrailleFont(10));
        txtAsciiDisplay.setForeground(new java.awt.Color(0, 255, 128));
        txtAsciiDisplay.setLineWrap(false);
        txtAsciiDisplay.setEditable(false);
        jScrollPane1.setViewportView(txtAsciiDisplay);

        pbMonsterHp.setBackground(new java.awt.Color(0, 30, 30));
        pbMonsterHp.setFont(new java.awt.Font("JetBrains Mono", 1, 12));
        pbMonsterHp.setForeground(new java.awt.Color(0, 200, 80));
        pbMonsterHp.setStringPainted(true);

        lblHpInfo.setFont(new java.awt.Font("JetBrains Mono", 0, 11));
        lblHpInfo.setForeground(new java.awt.Color(255, 255, 255));
        lblHpInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHpInfo.setText("<html><center>HP: - / -</center></html>");

        btnAttack.setBackground(new java.awt.Color(180, 30, 30));
        btnAttack.setFont(new java.awt.Font("JetBrains Mono", 1, 18));
        btnAttack.setForeground(java.awt.Color.WHITE);
        btnAttack.setText(">> SERANG! <<");
        btnAttack.addActionListener(this::btnAttackActionPerformed);

        btnSave.setBackground(new java.awt.Color(0, 100, 0));
        btnSave.setFont(new java.awt.Font("JetBrains Mono", 1, 13));
        btnSave.setForeground(java.awt.Color.WHITE);
        btnSave.setText("[ SAVE GAME ]");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        btnBack.setBackground(new java.awt.Color(0, 80, 80));
        btnBack.setFont(new java.awt.Font("JetBrains Mono", 1, 13));
        btnBack.setForeground(java.awt.Color.WHITE);
        btnBack.setText("[ KEMBALI KE MENU ]");
        btnBack.addActionListener(this::btnBackActionPerformed);

        // ---- MAIN LAYOUT ----
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(panelSidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMonsterName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblEffectInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
                    .addComponent(pbMonsterHp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblHpInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAttack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(15, 15, 15)
                        .addComponent(btnBack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelSidebar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMonsterName, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addComponent(lblEffectInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                        .addGap(8, 8, 8)
                        .addComponent(pbMonsterHp, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(lblHpInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnAttack, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(12, 12, 12))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>

    // ==========================================
    // EVENT HANDLERS - DAMAGE UPGRADES
    // ==========================================
    private void btnUpgrade1ActionPerformed(java.awt.event.ActionEvent evt) {
        buyUpgrade("Pemula", 1, 10);
    }

    private void btnUpgrade2ActionPerformed(java.awt.event.ActionEvent evt) {
        buyUpgrade("Prajurit", 3, 25);
    }

    private void btnUpgrade3ActionPerformed(java.awt.event.ActionEvent evt) {
        buyUpgrade("Ksatria", 6, 60);
    }

    // ==========================================
    // EVENT HANDLERS - AUTO CLICK
    // ==========================================
    private void btnAutoClick2ActionPerformed(java.awt.event.ActionEvent evt) {
        buyAutoClick(2, 80, 1);
    }

    private void btnAutoClick4ActionPerformed(java.awt.event.ActionEvent evt) {
        buyAutoClick(4, 200, 2);
    }

    // ==========================================
    // EVENT HANDLERS - SKILLS
    // ==========================================
    private void btnChainDamageActionPerformed(java.awt.event.ActionEvent evt) {
        if (chainDamageLevel == 0) {
            buyChainDamage(150, 1);
        } else {
            buyChainDamage(400, chainDamageLevel + 1);
        }
    }

    private void btnCoinIncomeActionPerformed(java.awt.event.ActionEvent evt) {
        if (coinIncomeLevel == 0) {
            buyCoinIncome(100, 1);
        } else {
            buyCoinIncome(coinIncomeLevel * 200, coinIncomeLevel + 1);
        }
    }

    private void btnThunderActionPerformed(java.awt.event.ActionEvent evt) {
        if (thunderLevel == 0) {
            buyThunder(300, 1);
        } else {
            buyThunder(thunderLevel * 600, thunderLevel + 1);
        }
    }

    // ==========================================
    // EVENT HANDLERS - GACHA
    // ==========================================
    private void btnGacha1ActionPerformed(java.awt.event.ActionEvent evt) {
        doGacha(30, 1);
    }

    private void btnGacha2ActionPerformed(java.awt.event.ActionEvent evt) {
        doGacha(75, 2);
    }

    private void btnGacha3ActionPerformed(java.awt.event.ActionEvent evt) {
        doGacha(150, 3);
    }

    // ==========================================
    // EVENT HANDLER - TOMBOL SERANG
    // ==========================================
    private void btnAttackActionPerformed(java.awt.event.ActionEvent evt) {
        if (isMonsterDefeated || currentHp <= 0) return;
        int totalHits = 1 + (autoClickLevel * 2) + gachaClickBonus;
        int dmg = strength + gachaDamageBonus;
        for (int i = 0; i < totalHits; i++) {
            if (isMonsterDefeated || currentHp <= 0) break;
            doAttackLogic(dmg, 1, (i == totalHits - 1));
        }
    }

    // ==========================================
    // EVENT HANDLER - SAVE & BACK
    // ==========================================
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        saveGameProgress(true);
    }

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {
        int pilihan = JOptionPane.showConfirmDialog(this,
            "Simpan progres sebelum kembali ke menu save?",
            "Simpan & Kembali",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (pilihan == JOptionPane.YES_OPTION) {
            saveGameProgress(true);
            if (autoClickTimer != null) autoClickTimer.stop();
            new Save().setVisible(true);
            this.dispose();
        } else if (pilihan == JOptionPane.NO_OPTION) {
            if (autoClickTimer != null) autoClickTimer.stop();
            new Save().setVisible(true);
            this.dispose();
        }
    }

    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new Game(1).setVisible(true));
    }

    // Variables declaration
    private javax.swing.JButton btnAttack;
    private javax.swing.JButton btnAutoClick2;
    private javax.swing.JButton btnAutoClick4;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnChainDamage;
    private javax.swing.JButton btnCoinIncome;
    private javax.swing.JButton btnGacha1;
    private javax.swing.JButton btnGacha2;
    private javax.swing.JButton btnGacha3;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnThunder;
    private javax.swing.JButton btnUpgrade1;
    private javax.swing.JButton btnUpgrade2;
    private javax.swing.JButton btnUpgrade3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblAutoClick;
    private javax.swing.JLabel lblCoins;
    private javax.swing.JLabel lblCurrentDmg;
    private javax.swing.JLabel lblEffectInfo;
    private javax.swing.JLabel lblGachaTitle;
    private javax.swing.JLabel lblHpInfo;
    private javax.swing.JLabel lblMonsterName;
    private javax.swing.JLabel lblShopTitle;
    private javax.swing.JLabel lblSidebarTip;
    private javax.swing.JPanel panelSidebar;
    private javax.swing.JProgressBar pbMonsterHp;
    private javax.swing.JTextArea txtAsciiDisplay;
    // End of variables declaration
}
