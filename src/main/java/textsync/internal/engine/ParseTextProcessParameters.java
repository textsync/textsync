package textsync.internal.engine;

import java.util.Set;

import textsync.internal.DataService;
import textsync.internal.LogService;

public interface ParseTextProcessParameters {
    public String text();
    public String extension();
    public DataService dataService();
    public LogService logService();
    public Set<Operation> skippedOperations();
    public WhenTextProcessingComplete callback();
}