/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.ui;

import io.nextweb.Session;
import io.nextweb.common.User;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.results.WithLoadResult;
import textsync.internal.appjangle.AppjangleLogin;

/**
 *
 * @author mroh004
 */
public class MainFrame extends javax.swing.JFrame {

    SyncPanel syncPanel;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("textsync GUI");

        contentPanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(contentPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                final MainFrame mf = new MainFrame();
                mf.setSize(500, 400);
                mf.setLocation(600, 400);
                mf.setVisible(true);

                final AppjangleLogin loginForm = new AppjangleLogin(new AppjangleLogin.WhenLoggedIn() {

                    public void thenDo(final Session session, final Component p_loginForm, final User user) {
                        final JPanel destPanel = mf.contentPanel;
                        
                        destPanel.remove(p_loginForm);
                        destPanel.validate();
                        destPanel.revalidate();
                        
                        session.link(user.userNode()).get();
                        
                        destPanel.add(new SyncPanel(session, user), BorderLayout.CENTER);

                        destPanel.validate();
                        destPanel.revalidate();
                        
                        //session.close().get();
    

                    }
                });

                mf.contentPanel.add(loginForm);

                loginForm.setVisible(true);

            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentPanel;
    // End of variables declaration//GEN-END:variables
}
