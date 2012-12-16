package textsync.internal.engine;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseSectionsProcess {

	final static Pattern startPattern = Pattern
			.compile(ParseConstants.commentStartRegex);
	final static Pattern endPattern = Pattern
			.compile(ParseConstants.commentEndRegex);
	final String source;
	
	private static final boolean ENABLE_LOG = false;

	Matcher commentStartMatcher;
	Matcher commentEndMatcher;

	public class SectionMatch {
		Operation operation;
		String parameter;
		int sectionStart;
		int sectionEnd;
		int startCommentEnd;
		int endCommentStart;
		
		@Override
		public String toString() {
			return "SectionMatch [operation=" + operation + ", parameter="
					+ parameter + ", sectionStart=" + sectionStart
					+ ", sectionEnd=" + sectionEnd + ", startCommentEnd="
					+ startCommentEnd + ", endCommentStart=" + endCommentStart
					+ "]";
		}
		
		
		
		
		
	}

	public List<SectionMatch> parse() {
		List<SectionMatch> matches = new LinkedList<SectionMatch>();
		Stack<SectionMatch> parents = new Stack<ParseSectionsProcess.SectionMatch>();
		
		tick(source, matches, null, parents);
		
		return matches;
		
	}
	
	private void tick(String source, List<SectionMatch> matches,
			SectionMatch thisMatch, Stack<SectionMatch> parentMatches) {

		if (!(commentStartMatcher.find())) {
			return;
		}

		int commentStart = commentStartMatcher.start();

		if (!commentEndMatcher.find(commentStart)) {
			return;
		}

		final int commentEnd = commentEndMatcher.end();
		final int commentContentStart;

		if (source.substring(commentStartMatcher.start()).startsWith("// ")
				|| source.substring(commentStartMatcher.start()).startsWith(
						"/* ") || source.substring(commentStartMatcher.start()).startsWith(
								"*/ ")) {
			commentContentStart = commentStartMatcher.start() + 7;
		} else {
			commentContentStart = commentStartMatcher.start() + 4;
			
		}

		final int commentContentEnd;
		if (source.substring(commentEndMatcher.end() - 3).startsWith(" */")) {
			commentContentEnd =  commentEndMatcher.end() - 7;
		} else {
			commentContentEnd= commentEndMatcher.end() - 4;
		}

		final String commentContent = source.substring(commentContentStart,
				commentContentEnd);

		final String endMarker = "one.end";
		final String uploadNew = "one.create";
		final String uploadPublic = "one.createPublic";
		final String upload = "one.upload";
		final String download = "one.download";
		final String ignore = "one.ignoreNext";

		final String content;
		if (commentContent.length() > 2) {
			content = source.substring(commentContentStart + 1,
					commentContentEnd);
		} else {
			content = "";
		}

		if (ENABLE_LOG) {
			System.out.println("  Comment content: ["
					+ content.substring(0, Math.min(110, content.length()))+"]");
		}

		if (content.startsWith(ignore)) {
			commentStartMatcher.find();
			tick(source, matches, thisMatch, parentMatches);
			return;
		}
		
		if (content.startsWith(endMarker)) {
			
			if (thisMatch == null) {
				tick(source, matches, thisMatch, parentMatches);
				return;
			}

			thisMatch.endCommentStart = commentStart;
			thisMatch.sectionEnd = commentEnd;
			
			if (ENABLE_LOG) {
				System.out.println("Found match: "+thisMatch);
			}
			
			matches.add(thisMatch);
			
			if (parentMatches.empty()) {
				tick(source, matches, null, parentMatches);
			}else {
			
			tick(source, matches, parentMatches.pop(), parentMatches);
			}
			return;
		}

		SectionMatch m = new SectionMatch();
		m.sectionStart = commentStart;
		m.startCommentEnd = commentEnd;
		m.operation = Operation.NONE;
		
		
		if (content.startsWith(uploadPublic)) {
			m.operation = Operation.UPLOADPUBLIC;
			m.parameter = source.substring(commentContentStart + uploadPublic.length() + 2,
                     commentContentEnd);
			
		} else if (content.startsWith(uploadNew)) {
			m.operation = Operation.UPLOADNEW;
			m.parameter = source.substring(commentContentStart + uploadNew.length() + 2,
                    commentContentEnd);
		} else if (content.startsWith(upload)) {
			
			m.operation = Operation.UPLOAD;
			m.parameter = source.substring(commentContentStart + upload.length() + 2,
                    commentContentEnd);
		} else if (content.startsWith(download)) {
			m.operation = Operation.DOWNLOAD;
			m.parameter = source.substring(commentContentStart + download.length() + 2,
                    commentContentEnd);
		}

		if (m.operation != Operation.NONE) {
			if (thisMatch != null) {
				parentMatches.push(thisMatch);
			}
			tick(source, matches, m, parentMatches);
			return;
		}
		
		tick(source, matches, thisMatch, parentMatches);

	}

	

	public ParseSectionsProcess(String source) {
		super();

		commentStartMatcher = startPattern.matcher(source);

		commentEndMatcher = endPattern.matcher(source);
		
		this.source = source;
	}

}
