/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import one.core.nodes.OneNode;
import textsync.internal.DataService;
import textsync.internal.LogService;
import textsync.internal.appjangle.AppjangleDataService;

/**
 *
 * @author Max
 */
public class ParseTextProcess {

    static final boolean ENABLE_LOG = false;

    public static void processText(final ParseTextProcessParameters params) {
      
        new DoOperationsProcess(params).start();
    }

    

    public static interface OperationCallback {

        public void onSuccess(String newFile);

        public void onFailure(Throwable t);
    }

    public static class DoOperationsProcess {

        final Matcher commentStartMatcher;
        final Matcher commentEndMatcher;
        String file;
       // boolean skipUpload;
        WhenTextProcessingComplete callback;
        List<Replace> replacements;
        int lastCommentEnd = -1;
        int lastCommentStart = -1;
        String parameter = "";
        Operation operation = Operation.NONE;
        private final DataService dataService;
        private final LogService logService;
        private final String extension;
        private final Set<Operation> skipOperations;

        public void start() {
            next();
        }

        protected void next() {

            if (ENABLE_LOG) {
                System.out.println("Seek next match.");
                System.out.println("  Current Operation: " + operation);
            }
            
            if (!commentStartMatcher.find()) {
                callback.onSuccess(performReplacements(file, replacements));
                return;
            }

             if (ENABLE_LOG) {
                System.out.println("  Match found: "+commentStartMatcher.start()+" to "+commentStartMatcher.end());
 
            }
            
            final int commentStart = commentStartMatcher.start();
           
            if (!this.commentEndMatcher.find(commentStart)) {
                callback.onSuccess(performReplacements(file, replacements));
                return;
            }
            
            final int commentEnd = commentEndMatcher.end();
            final int commentContentStart;


            if (!file.substring(commentStartMatcher.start()).startsWith("// ") || !file.substring(commentStartMatcher.start()).startsWith("/* ")) {
                commentContentStart = commentStartMatcher.start() + 4;
            } else {
                commentContentStart = commentStartMatcher.start() + 7;
            }
            
            
            
            final int commentContentEnd = commentEndMatcher.end() - 4;

            if (commentContentEnd < 0) {
                operation = Operation.NONE;
                next();
                return;
            }
            
            final String commentContent = file.substring(commentContentStart,
                    commentContentEnd);

            final String endMarker = "one.end";

            final String uploadNew = "one.create";
            final String uploadPublic = "one.createPublic";
            final String upload = "one.upload";
            final String download = "one.download";
            final String ignore = "one.ignoreNext";

            final String content;
            if (commentContent.length() > 2) {
                content = file.substring(
                        commentContentStart + 1, commentContentEnd);
            } else {
                content = "";
            }

            if (ENABLE_LOG) {
                System.out.println("  Comment content: " + content.substring(0, Math.min(50, content.length())));
          
            }

            if (content.startsWith(ignore)) {
                commentStartMatcher.find();
                next();
                return;
            }

            if (content.startsWith(endMarker)) {
                if (ENABLE_LOG) {
                    System.out.println("  Hit end marker.");
                }
                
                if (operation == Operation.UPLOADNEW || operation == Operation.UPLOADPUBLIC) {

                    
                    if (skipOperations.contains(Operation.UPLOADNEW) || skipOperations.contains(Operation.UPLOADPUBLIC)) {
                        operation = Operation.NONE;
                        next();
                        return;
                    }
                    
                    final String enclosedWithinComments = file.substring(
                            lastCommentEnd, commentStart);

                    dataService.createNewNode(enclosedWithinComments,
                            parameter,
                            extension,
                            operation == Operation.UPLOADPUBLIC,
                            new AppjangleDataService.WhenNewNodeCreated() {

                                public void thenDo(OneNode newNode) {
                                    operation = Operation.NONE;
                                    String replacement = "<!-- one.upload " + newNode.getId() + " -->";
                                    if (file.substring(lastCommentStart).startsWith("// ")) {
                                        replacement = "// " + replacement;
                                    }
                                    replacements.add(new Replace(lastCommentStart, lastCommentEnd, replacement));
                                    logService.note("  Create new document: " + newNode.getId());
                                    next();
                                }

                                public void onFailure(Throwable t) {
                                    logService.note("  Exception reported: " + t.getMessage());
                                    operation = Operation.NONE;

                                    next();
                                }
                            });



                    return;

                }

                if (operation == Operation.UPLOAD) {

                    if (skipOperations.contains(Operation.UPLOAD)) {
                        operation = Operation.NONE;
                        next();
                        return;
                    }

                    final String enclosedWithinComments = file.substring(
                            lastCommentEnd, commentStart);

                    dataService.uploadChanges(enclosedWithinComments, parameter, new DataService.WhenChangesUploaded() {

                        public void thenDo(boolean changed) {
                            if (changed) {
                                logService.note("  Updated node for: " + parameter);
                            }
                            operation = Operation.NONE;

                            next();
                        }

                        public void onFailure(Throwable t) {
                            logService.note("Exception occured: " + t.getMessage());
                            operation = Operation.NONE;

                            next();
                        }
                    });

                    return;

                }

                if (operation == Operation.DOWNLOAD) {
                    
                    if (skipOperations.contains(Operation.DOWNLOAD)) {
                        operation = Operation.NONE;
                        next();
                        return;
                    }
                    
                    final String localValue = file.substring(
                            lastCommentEnd, commentStart);
                    dataService.downloadChanges(localValue, parameter, new DataService.WhenChangesDownloaded() {

                        public void onUnchanged() {
                            operation = Operation.NONE;

                            next();
                        }

                        public void onChanged(String newValue) {
                            Replace replace = new Replace(lastCommentEnd, commentStart, newValue);
                            replacements.add(replace);

                            operation = Operation.NONE;
                            next();

                        }

                        public void onFailure(Throwable t) {
                            logService.note("  Exception occured: " + t.getMessage());
                            operation = Operation.NONE;

                            next();
                        }
                    });
                    return;

                }



            }

            if (operation == Operation.NONE) {

                if (content.startsWith(uploadPublic)) {
                    operation = Operation.UPLOADPUBLIC;
                    lastCommentEnd = commentEnd;
                    lastCommentStart = commentStart;
                    parameter = file.substring(commentContentStart + uploadPublic.length() + 2,
                            commentContentEnd);
                    next();
                    return;
                }

                if (content.startsWith(uploadNew)) {
                    operation = Operation.UPLOADNEW;
                    lastCommentEnd = commentEnd;
                    lastCommentStart = commentStart;
                    parameter = file.substring(commentContentStart + uploadNew.length() + 2,
                            commentContentEnd);
                    next();
                    return;
                }

                if (content.startsWith(upload)) {
                    operation = Operation.UPLOAD;
                    lastCommentEnd = commentEnd;
                    lastCommentStart = commentStart;
                    parameter = file.substring(commentContentStart + upload.length() + 2,
                            commentContentEnd);
                    next();
                    return;
                }

                if (content.startsWith(download)) {
                    operation = Operation.DOWNLOAD;
                    lastCommentEnd = commentEnd;
                    lastCommentStart = commentStart;
                    parameter = file.substring(commentContentStart + download.length() + 2,
                            commentContentEnd);
                    next();
                    return;
                }

                next();
                return;
            } else {
                next();
                return;
            }

            //throw new IllegalStateException("Case not handeled: "+content);

        }

        public DoOperationsProcess(final ParseTextProcessParameters params) {
            super();
            this.dataService = params.dataService();
            this.logService = params.logService();
            this.file = params.text();
            this.extension = params.extension();
            
            final Pattern p = Pattern.compile(ParseConstants.commentStartRegex);
            this.commentStartMatcher = p.matcher(file);
            
            final Pattern p2 = Pattern.compile(ParseConstants.commentEndRegex);
           this.commentEndMatcher = p2.matcher(file);
            
            this.skipOperations = params.skippedOperations();
            this.callback = params.callback();
            this.replacements = new ArrayList<Replace>();
        }
    }

    public static String performReplacements(String input, List<Replace> replacements) {

        String result = input;

        for (Replace r : replacements) {

            result = result.substring(0, r.from) + r.with + result.substring(r.to);

        }
        return result;
    }

    public static class Replace {

        public int from;
        public int to;
        public String with;

        public Replace(int from, int to, String with) {
            this.from = from;
            this.to = to;
            this.with = with;
        }
    }

    
}
