
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
        strengthLabel.setText("STRENGTH:" + strength);
   }

    public void setCardSelected(boolean isSelected) {
        if (isSelected) {
            super.putClientProperty(FlatClientProperties.STYLE,
                    "background: $Component.focusedBackground; "
                    + "arc: 15; "
                    + "border: 2,2,2,2,$Component.focusColor,1,15"
            );
        } else {
            super.putClientProperty(FlatClientProperties.STYLE,
                    "background: $Component.borderColor; "
                    + "arc: 15; "
                    + "border: 1,1,1,1,$Component.borderColor,1,15"
            );
        }
        this.revalidate();
        this.repaint();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        saveLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        strengthLabel = new javax.swing.JLabel();

        saveLabel.setFont(new java.awt.Font("JetBrains Mono", 1, 20)); // NOI18N
        saveLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        saveLabel.setText("SAVE 1");

        nameLabel.setFont(new java.awt.Font("JetBrains Mono", 0, 14)); // NOI18N
        nameLabel.setText("NAME:");

        strengthLabel.setFont(new java.awt.Font("JetBrains Mono", 0, 14)); // NOI18N
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
    }// </editor-fold>                        

    // Variables declaration - do not modify                     
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel saveLabel;
    private javax.swing.JLabel strengthLabel;
    // End of variables declaration                   
}
