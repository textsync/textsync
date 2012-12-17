/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.engine;

import textsync.internal.DataService;
import textsync.internal.LogService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import one.async.joiner.CallbackLatch;

/**
 *
 * @author mroh004
 */
public class ProcessFilesProcess {

    public interface WhenFilesProcessed {

        public void onSuccess();

        public void onFailure(Throwable t);
    }

    public interface ProcessFilesParameters {
        public File inputFile();
            public DataService dataService();
           public LogService logService();
            public FileCache cache();
             public Set<Operation> skippedOperations();
            public WhenFilesProcessed callback();
    }
    
    public static void processFile(final ProcessFilesParameters p) throws Exception {

        final List<String> files = getFilesRecursively(p.inputFile().getAbsoluteFile());

        
        final CallbackLatch latch = new CallbackLatch(files.size()) {

            @Override
            public void onCompleted() {
                p.callback().onSuccess();
            }

            @Override
            public void onFailed(Throwable thrwbl) {
                p.callback().onFailure(thrwbl);
            }
        };


        for (final String filePath : files) {

            // logService.note("  Loading file: " + filePath);

            final FileInputStream fis = new FileInputStream(new File(
                    filePath));
            final Scanner scanner = new Scanner(fis, "UTF-8");

            String file = "";
            while (scanner.hasNextLine()) {
                file += scanner.nextLine() + "\n";
            }
            fis.close();

            final String fileClosed = file;
            //logService.note("  Start processing file: " + filePath);
            
            final boolean skipUpload = (p.cache() != null) && !p.cache().isModified(new File(filePath));

            if (!skipUpload && p.cache() != null) {
                p.cache().updateCache(new File(filePath));
            }

            ParseTextProcessParameters params = new ParseTextProcessParameters() {

                public String text() {
                    return fileClosed;
                }

                public String extension() {
                    return getExtension(filePath);
                }

                public DataService dataService() {
                    return p.dataService();
                }

                public LogService logService() {
                    return p.logService();
                }

                public Set<Operation> skippedOperations() {
                    Set<Operation> skipped = new HashSet<Operation>(p.skippedOperations());
                    if (skipUpload) {
                       skipped.add(Operation.UPLOAD); 
                    }
                    return skipped;
                }


                public WhenTextProcessingComplete callback() {
                    return new WhenTextProcessingComplete() {

                        public void onSuccess(String text) {

                            if (!text.equals(fileClosed)) {

                                //logService.note("  Writing changed file: " + filePath);
                                try {
                                    FileOutputStream fos = new FileOutputStream(new File(filePath));

                                    byte[] data = text.getBytes("UTF-8");
                                    fos.write(data, 0, data.length);

                                    fos.close();
                                    
                                    if (p.cache() != null) {
                                    	p.cache().updateCache(new File(filePath));
                                    }
                                } catch (Exception e) {
                                    latch.registerFail(e);
                                    return;
                                }
                            }
                            // logService.note("  Processing completed for file: " + filePath);
                            latch.registerSuccess();
                        }

                        public void onFailure(Throwable t) {
                            latch.registerFail(t);
                        }
                    };
                }
            };

            
            new NewProcessTextProcess(params).process();
            

        }

    }

    public static List<String> getFilesRecursively(final File dir) {
        final ArrayList<String> list = new ArrayList<String>(100);

        if (dir.isFile()) {
            list.add(dir.getAbsolutePath());
            return list;
        }

        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File f : files) {
                if (f.isDirectory()) {
                    list.addAll(getFilesRecursively(f));
                } else {
                    list.add(f.getAbsolutePath());
                }
            }
        }
        return list;
    }

     public static List<File> getFilesRecursively(final List<File> files) {
         List<String> res = new ArrayList<String>(files.size());
         for (File file: files) {
             res.addAll(getFilesRecursively(file));
         }
         
         List<File> resFiles = new ArrayList<File>(res.size());
         
         for (String path : res) {
             resFiles.add(new File(path));
         }
         
         return resFiles;
     }
    
    private static String getExtension(String path) {
        final int idx = path.lastIndexOf(".");
        return path.substring(idx + 1);
    }
}
