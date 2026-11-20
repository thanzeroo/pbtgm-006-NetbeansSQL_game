
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 1. HARUS MERUPAKAN JPANEL, BUKAN JFRAME
public class SaveCard extends javax.swing.JPanel {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SaveCard.class.getName());

    private final String name;
    private final int id, strength;

    public SaveCard(int id, String name, int strength, Save mainFrame) {
        initComponents();

        this.id = id;
        this.name = name;
        this.strength = strength;
        
        setPreferredSize(new Dimension(245, 500));
        setMinimumSize(new Dimension(245, 500));
        setMaximumSize(new Dimension(245, 500));

        setBackground(new java.awt.Color(0, 40, 40));
        setCardSelected(false);
        loadData();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.setSelectedPlayer(id, SaveCard.this);
            }
        });
    }

    private void loadData() {
        saveLabel.setText("SAVE " + id);
        nameLabel.setText("NAME: " + name);
        strengthLabel.setText("STRENGTH: " + strength);
    }

    public void setCardSelected(boolean isSelected) {
        if (isSelected) {
            super.putClientProperty(FlatClientProperties.STYLE,
                    "background: rgb(0, 65, 65); "
                    + "arc: 15; "
                    + "border: 3,3,3,3,rgb(0, 204, 102),1,15"
            );
        } else {
            super.putClientProperty(FlatClientProperties.STYLE,
                    "background: rgb(0, 40, 40); "
                    + "arc: 15; "
                    + "border: 2,2,2,2,rgb(0, 102, 0),1,15"
            );
        }
        this.revalidate();
        this.repaint();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        saveLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        strengthLabel = new javax.swing.JLabel();

        saveLabel.setFont(new java.awt.Font("JetBrains Mono", 1, 20)); // NOI18N
        saveLabel.setForeground(new java.awt.Color(255, 255, 255));
        saveLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        saveLabel.setText("SAVE 1");

        nameLabel.setFont(new java.awt.Font("JetBrains Mono", 0, 14)); // NOI18N
        nameLabel.setForeground(new java.awt.Color(220, 255, 220));
        nameLabel.setText("NAME:");

        strengthLabel.setFont(new java.awt.Font("JetBrains Mono", 0, 14)); // NOI18N
        strengthLabel.setForeground(new java.awt.Color(220, 255, 220));
        strengthLabel.setText("STRENGTH: ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(saveLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                                        .addComponent(nameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(strengthLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(saveLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(nameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(strengthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(46, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel saveLabel;
    private javax.swing.JLabel strengthLabel;
    // End of variables declaration//GEN-END:variables
}
