package textsync.internal.engine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import one.core.nodes.OneNode;

import textsync.internal.DataService;
import textsync.internal.appjangle.AppjangleDataService;
import textsync.internal.engine.ParseSectionsProcess.SectionMatch;
import textsync.internal.engine.ParseTextProcess.Replace;

public class NewProcessTextProcess {

	private final static boolean ENABLE_LOG = false;
	
	private final ParseTextProcessParameters params;

	private final String source;

	List<Replace> replacements;

	List<SectionMatch> sections;

	int idx;

	public void process() {

		sections = new ParseSectionsProcess(params.text()).parse();

		Collections.reverse(sections);

		idx = -1;

		nextSection();
	}

	private void nextSection() {
		idx++;
		if (idx >= sections.size()) {
			applyReplacements();
			return;
		}
		
		processMatch(sections.get(idx));
	}

	private void applyReplacements() {
		
		String result = source;

		int lastFrom = -1;
        for (Replace r : replacements) {
        	
        	if (lastFrom != -1 && r.to > lastFrom) {
        		// don't apply this replacement, since its accurancy cannot be guaranteed.
        		continue;
        	}
        	
            result = result.substring(0, r.from) + r.with + result.substring(r.to);
            lastFrom = r.from;
        }
       
		params.callback().onSuccess(result);
	}

	
	
	private void processMatch(SectionMatch m) {
		if (params.skippedOperations().contains(m.operation)) {
			nextSection();
			return;
		}

		final String enclosedWithinComments = source.substring(
				m.startCommentEnd, m.endCommentStart);

		if (m.operation == Operation.UPLOADNEW
				|| m.operation == Operation.UPLOADPUBLIC) {
			processUploadNew(m, enclosedWithinComments);
			return;
		}
		
		if (m.operation == Operation.UPLOAD) {
			processUpload(m, enclosedWithinComments);
			return;
		}
		
		if (m.operation == Operation.DOWNLOAD) {
			processDownload(m, enclosedWithinComments);
			return;
		}

	}

	private void processDownload(final SectionMatch m, String enclosedWithinComments) {
		params.dataService().downloadChanges(enclosedWithinComments, m.parameter, new DataService.WhenChangesDownloaded() {

            public void onUnchanged() {
                
                nextSection();
            }

            public void onChanged(String newValue) {
                Replace replace = new Replace(m.startCommentEnd, m.endCommentStart, newValue);
                replacements.add(replace);

                nextSection();

            }

            public void onFailure(Throwable t) {
                params.logService().note("  Exception occured: " + t.getMessage());
                
                nextSection();
            }
        });
	}

	private void processUpload(final SectionMatch m, String enclosedWithinComments) {
		 if (ENABLE_LOG) {
			 System.out.println("Performing upload to "+m.parameter);
		 }
		 params.dataService().uploadChanges(enclosedWithinComments, m.parameter, new DataService.WhenChangesUploaded() {

             public void thenDo(boolean changed) {
                 if (changed) {
                     params.logService().note("  Updated node for: " + m.parameter);
                 }
                
                 nextSection();
             }

             public void onFailure(Throwable t) {
                 params.logService().note("Exception occured: " + t.getMessage());
                 
                 nextSection();
             }
         });

		
	}

	private void processUploadNew(final SectionMatch m,
			String enclosedWithinComments) {

		params.dataService().createNewNode(enclosedWithinComments, m.parameter,
				params.extension(), m.operation == Operation.UPLOADPUBLIC,
				new AppjangleDataService.WhenNewNodeCreated() {

					public void thenDo(OneNode newNode) {

						String replacement = "<!-- one.upload "
								+ newNode.getId() + " -->";
						if (source.substring(m.sectionStart).startsWith("// ")) {
							replacement = "// " + replacement;
						}
						replacements.add(new Replace(m.sectionStart,
								m.startCommentEnd, replacement));
						params.logService().note(
								"  Create new document: " + newNode.getId());
						nextSection();
					}

					public void onFailure(Throwable t) {
						params.logService().note("  Exception reported: "
								+ t.getMessage());
						

						nextSection();
					}
				});
	}

	public NewProcessTextProcess(ParseTextProcessParameters params) {
		super();
		this.params = params;
		this.source= params.text();
		this.replacements = new LinkedList<ParseTextProcess.Replace>() ;
	}

}
