package textsync.internal.engine;

public class ParseConstants {

	static final String matchCStyleComment = "(// )?";
	static final String matchCSSStyleCommentStart = "(/\\* )?";
	static final String matchCSSStyleCommentEnd = "( \\*/)?";
	static final String matchCommentStart = "\\<![ \\r\\n\\t]*--";
	static final String matchCommentEnd = "--[ \\r\\n\\t]*\\>";
	static final String commentRegex = "(// )?\\<![ \\r\\n\\t]*(--([^\\-]|[\\r\\n]|-[^\\-])*--[ \\r\\n\\t]*)\\>";
	static final String commentRegex2 = "(--([^\\-]|[\\r\\n]|-[^\\-])*";
	static final String commentStartRegex = matchCStyleComment+matchCSSStyleCommentStart+matchCommentStart;
	static final String commentEndRegex = matchCommentEnd+matchCSSStyleCommentEnd;

}
