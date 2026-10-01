
import com.formdev.flatlaf.FlatLightLaf;

public class Start extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Start.class.getName());

    public Start() {
        FlatLightLaf.setup();
        initComponents();
        setLocationRelativeTo(null);

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        Startbtn = new javax.swing.JButton();
        Quitbtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1200, 700));

        jPanel1.setBackground(new java.awt.Color(0, 51, 51));

        Startbtn.setBackground(new java.awt.Color(0, 102, 0));
        Startbtn.setFont(new java.awt.Font("JetBrains Mono", 0, 24)); // NOI18N
        Startbtn.setForeground(new java.awt.Color(255, 255, 255));
        Startbtn.setText("Start");
        Startbtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        Startbtn.addActionListener(this::StartbtnActionPerformed);

        Quitbtn.setBackground(new java.awt.Color(0, 102, 0));
        Quitbtn.setFont(new java.awt.Font("JetBrains Mono", 0, 24)); // NOI18N
        Quitbtn.setForeground(new java.awt.Color(255, 255, 255));
        Quitbtn.setText("Quit");
        Quitbtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        Quitbtn.addActionListener(this::QuitbtnActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(111, 111, 111)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Quitbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Startbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(824, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(89, 89, 89)
                .addComponent(Startbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Quitbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(491, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void StartbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartbtnActionPerformed
        Save saveFrame = new Save();
        saveFrame.setVisible(true);

        this.dispose();
    }//GEN-LAST:event_StartbtnActionPerformed

    private void QuitbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitbtnActionPerformed
        System.exit(0);
    }//GEN-LAST:event_QuitbtnActionPerformed

    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new Start().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Quitbtn;
    private javax.swing.JButton Startbtn;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
