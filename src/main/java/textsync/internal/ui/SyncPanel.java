/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.DefaultListModel;
import javax.swing.TransferHandler;
import one.core.domain.OneClient;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.results.WithLoadResult;
import one.core.dsl.callbacks.results.WithUserRegisteredResult;
import textsync.TextSync;
import textsync.internal.DataServiceFactory;
import textsync.internal.LogService;
import textsync.internal.appjangle.AppjangleDataServiceFactory;
import textsync.internal.appjangle.AppjangleLogin;
import textsync.internal.engine.Operation;
import textsync.internal.engine.SyncManager;
import textsync.internal.engine.SyncManager.FileProvider;

/**
 *
 * @author mroh004
 */
public class SyncPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    
    SyncManager syncManager;
    WithUserRegisteredResult registrationInfos;
    DataServiceFactory dataServiceFactory;
    FileProvider fileProvider;
    LogService logService = new LogService() {

        public void note(String text) {

            if (messages.getText().equals("")) {
                messages.setText(text);
            } else {
                messages.setText(messages.getText() + "\n" + text);
            }

            if (messages.getText().length() > 10000) {
                messages.setText(messages.getText().substring(messages.getText().length() - 9999, messages.getText().length()));
            }
            
            if (messages.getText().length() > 1) {
            
                messages.setCaretPosition(messages.getText().length() - 1);
            }
        }
    };

    public class PanelSyncEvents implements SyncManager.SyncEvents {

        public void synchronizationCompleted() {
            progressBar.setValue(0);
            forceSyncButton.setEnabled(true);
        }

        
        
        public void initSynchronization(int maxValue) {
            forceSyncButton.setEnabled(false);
            progressBar.setMaximum(maxValue);
            progressBar.setValue(0); 
        }

        public void updateProgressBar(int delta) {
            progressBar.setValue(progressBar.getValue() + delta);
        }

        public void onFailure(Throwable t) {
            progressBar.setValue(0);
            forceSyncButton.setEnabled(true);
        }

        public void reportStatus(String status) {
           logService.note(status);
        }
    }

    /**
     * Creates new form SyncPanel
     */
    public SyncPanel(CoreDsl dsl, WithUserRegisteredResult wurr) {
        this.registrationInfos = wurr;
        initComponents();

        restoreSelectedDirsFromPrefs();

        dataServiceFactory = new AppjangleDataServiceFactory(dsl, wurr);

        this.directories.setDragEnabled(true);

        TransferHandler handler = new TransferHandler() {

            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import FileList
                if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport info) {
                if (!info.isDrop()) {
                    return false;
                }

                // Check for FileList flavor
                if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }

                // Get the fileList that is being dropped.
                Transferable t = info.getTransferable();
                final List<File> data;
                try {
                    data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                } catch (Exception e) {
                    return false;
                }
                DefaultListModel model = (DefaultListModel) directories.getModel();
                for (File file : data) {
                    //System.out.println("File added: "+file);
                    model.addElement(file.getAbsolutePath());
                }
                directories.repaint();
                logService.note("Processs newly added files: "+data);
                syncManager.doPrioritySync(data, new HashSet<Operation>(), new SyncManager.SyncEvents() {

                    public void synchronizationCompleted() {
                        logService.note("Successfully processed newly added files: "+data);
                    }

                    public void initSynchronization(int maxValue) {
                        
                    }

                    public void updateProgressBar(int newValue) {
                        
                    }

                    public void onFailure(Throwable t) {
                        logService.note("Unexpected exception while processing files: "+t.getMessage());
                        throw new RuntimeException(t);
                    }

                    public void reportStatus(String status) {
                        
                    }
                });
                saveSelectedDirsToPrefs();

                return true;
            }
        };
        this.directories.setTransferHandler(handler);

         fileProvider = new SyncManager.FileProvider() {

            public List<File> getFiles() {
                final DefaultListModel model = (DefaultListModel) (directories.getModel());

                final List<File> res = new ArrayList<File>(model.getSize());

                for (int i = 0; i <= model.getSize() - 1; i++) {

                    final String elem = model.get(i).toString();
                    res.add(new File(elem));
                }
                return res;
            }
        };

        this.syncManager = new SyncManager(dataServiceFactory, logService, fileProvider, new PanelSyncEvents());



    }

    private void restoreSelectedDirsFromPrefs() {
        Preferences prefs = Preferences.userNodeForPackage(TextSync.class);
        String dirs = prefs.get("dirs", null);
        if (dirs != null) {

            for (String dir : dirs.split(";;;")) {
                if (!dir.equals("")) {
                    ((DefaultListModel) directories.getModel()).addElement(dir);
                }
            }

        }

    }

    private void saveSelectedDirsToPrefs() {
        Preferences prefs = Preferences.userNodeForPackage(TextSync.class);

        DefaultListModel model = (DefaultListModel) (directories.getModel());

        String dirs = "";
        for (int i = 0; i <= model.getSize() - 1; i++) {
            dirs = dirs + model.get(i).toString() + ";;;";
        }

        prefs.put("dirs", dirs);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        directories = new javax.swing.JList();
        removeButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        messages = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        forceSyncButton = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        uploadButton = new javax.swing.JButton();
        downloadButton = new javax.swing.JButton();
        monitorFilesCheckbox = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        checkFilesButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();

        jLabel1.setText("Monitored Files and Directories:");

        directories.setModel(new DefaultListModel());
        jScrollPane1.setViewportView(directories);

        removeButton.setText("Remove File");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Messages");

        messages.setColumns(20);
        messages.setRows(5);
        jScrollPane2.setViewportView(messages);

        jLabel3.setText("Add files and directories via drag and drop to box above!");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        forceSyncButton.setText("Synchronize");
        forceSyncButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forceSyncButtonActionPerformed(evt);
            }
        });

        jCheckBox1.setText("Synchronize in Background");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        uploadButton.setText("Upload");
        uploadButton.setEnabled(false);
        uploadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uploadButtonActionPerformed(evt);
            }
        });

        downloadButton.setText("Download");
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        monitorFilesCheckbox.setText("Upload Local Changes");
        monitorFilesCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monitorFilesCheckboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(forceSyncButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(uploadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downloadButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(monitorFilesCheckbox)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(forceSyncButton)
                    .addComponent(uploadButton)
                    .addComponent(downloadButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBox1)
                    .addComponent(monitorFilesCheckbox))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jButton1.setText("Logout");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        checkFilesButton.setText("Check Files");
        checkFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkFilesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2)
                            .addComponent(jScrollPane1)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(checkFilesButton)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(removeButton))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1))
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeButton)
                    .addComponent(jLabel3)
                    .addComponent(checkFilesButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void forceSyncButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forceSyncButtonActionPerformed

        forceSyncButton.setEnabled(false);
        syncManager.doFullSynchronization();
    }//GEN-LAST:event_forceSyncButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        DefaultListModel model = (DefaultListModel) (directories.getModel());
        model.remove(directories.getSelectedIndex());
        saveSelectedDirsToPrefs();
    }//GEN-LAST:event_removeButtonActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed

        if (jCheckBox1.isSelected()) {
            syncManager.startPeriodicFullSynchronization();



        } else {
            syncManager.stopPeriodicFullSynchronization();
        }



    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Preferences prefs = Preferences.userNodeForPackage(TextSync.class);
        prefs.remove("sessionId");

        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
        final Container parent = this.getParent();

        parent.remove(this);

        final Container destPanel = parent;
        AppjangleLogin login = new AppjangleLogin(new AppjangleLogin.WhenLoggedIn() {

            public void thenDo(final OneClient client, final Component p_loginForm, final WithUserRegisteredResult wurr) {

                destPanel.remove(p_loginForm);
                destPanel.validate();
                //destPanel.revalidate();

                client.one().load(wurr.userNodeUri()).withSecret(wurr.userNodeSecret()).in(client).and(new WhenLoaded() {

                    @Override
                    public void thenDo(WithLoadResult<Object> wlr) {


                        destPanel.add(new SyncPanel(client.one(), wurr), BorderLayout.CENTER);

                        destPanel.validate();
                        // destPanel.revalidate();

                        client.one().shutdown(client).and(WhenShutdown.DO_NOTHING);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        super.onFailure(t);
                    }
                });
            }
        });
        parent.add(login, BorderLayout.CENTER);
        parent.validate();

    }//GEN-LAST:event_jButton1ActionPerformed

    private void doCheckFiles() {
        logService.note("Checking registered files (" + new Date() + ")");

        final DefaultListModel model = (DefaultListModel) (directories.getModel());
        {
            final List<Integer> toClear = new LinkedList<Integer>();
            for (int i = 0; i <= model.getSize() - 1; i++) {
                final String filePath = model.get(i).toString();

                if (!new File(filePath).exists()) {
                    toClear.add(i);
                }
            }
            Collections.reverse(toClear);
            for (Integer idxToDelete : toClear) {
                logService.note("  Removed non existing file: " + model.get(idxToDelete).toString());
                model.remove(idxToDelete);

            }
        }

        logService.note("Checking for duplicate files.");

        {
            final Set<String> visited = new HashSet<String>(model.getSize());
            final List<Integer> toClear = new LinkedList<Integer>();
            for (int i = 0; i <= model.getSize() - 1; i++) {
                final String filePath = model.get(i).toString();

                if (visited.contains(filePath)) {
                    toClear.add(i);
                }

                visited.add(filePath);


            }
            Collections.reverse(toClear);
            for (Integer idxToDelete : toClear) {
                logService.note("  Removed duplicate file: " + model.get(idxToDelete).toString());
                model.remove(idxToDelete);

            }
        }

        directories.revalidate();
        saveSelectedDirsToPrefs();
    }

    private void checkFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkFilesButtonActionPerformed
        doCheckFiles();
    }//GEN-LAST:event_checkFilesButtonActionPerformed

    private void uploadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uploadButtonActionPerformed
       
    }//GEN-LAST:event_uploadButtonActionPerformed

    private void monitorFilesCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monitorFilesCheckboxActionPerformed
       
        if (monitorFilesCheckbox.isSelected()) {
            syncManager.startAutomaticUploadOfChangedFiles();

        } else {
            syncManager.stopAutomaticUploadOfChangedFiles();
        }

        
    }//GEN-LAST:event_monitorFilesCheckboxActionPerformed

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
        syncManager.doPrioritySync(fileProvider.getFiles(), new HashSet<Operation>(){{add(Operation.UPLOAD); add(Operation.UPLOADNEW); add(Operation.UPLOADPUBLIC);}}, new SyncManager.SyncEvents() {

                    public void synchronizationCompleted() {
                        logService.note("Successfully completed downloads operations.");
                    }

                    public void initSynchronization(int maxValue) {
                        
                    }

                    public void updateProgressBar(int newValue) {
                        
                    }

                    public void onFailure(Throwable t) {
                        logService.note("Unexpected exception while processing files: "+t.getMessage());
                        throw new RuntimeException(t);
                    }

                    public void reportStatus(String status) {
                        
                    }
                });
    }//GEN-LAST:event_downloadButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton checkFilesButton;
    private javax.swing.JList directories;
    private javax.swing.JButton downloadButton;
    private javax.swing.JButton forceSyncButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea messages;
    private javax.swing.JCheckBox monitorFilesCheckbox;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton uploadButton;
    // End of variables declaration//GEN-END:variables
}
