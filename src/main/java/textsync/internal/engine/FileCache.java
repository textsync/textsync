/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.engine;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mroh004
 */
public class FileCache {

    private final Map<String, Long> lastModified;

    private void logLastModified(File file) {
        lastModified.put(file.getAbsolutePath(), file.lastModified());
    }

    public boolean hasCache(File file) {
        return lastModified.containsKey(file.getAbsolutePath());
    }
    
    public void updateCache(File file) {
        logLastModified(file);
    }
    
    public boolean isModified(File file) {

        if (!lastModified.containsKey(file.getAbsolutePath())) {
            
            return true;
        }

        long lastValue = lastModified.get(file.getAbsolutePath());

        if (file.lastModified() == lastValue) {
            return false;
        }

        return true;

    }

    public FileCache() {
        this.lastModified = new HashMap<String, Long>();
    }
}
