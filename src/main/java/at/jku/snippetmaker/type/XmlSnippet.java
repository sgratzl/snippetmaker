/*$Id$*/
package at.jku.snippetmaker.type;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.util.LineTokenizer;

import at.jku.snippetmaker.Snippet;
import at.jku.snippetmaker.Snippet.Action;
import at.jku.snippetmaker.SnippetParser;
import at.jku.snippetmaker.Snippets;

public final class XmlSnippet extends BaseFilterReader implements SnippetParser {

	private final int step;
	private final boolean createMarkers;
	private final LineTokenizer tokenizer = new LineTokenizer();

	private String _line = null;
	private int _linePos = 0;

	public static BaseFilterReader createFilterReader(final Reader in, final int step, final boolean createMarkers) {
		return new XmlSnippet(in, step, createMarkers);
	}

	public static SnippetParser createParser(final Reader in) {
		return new XmlSnippet(in);
	}

	private XmlSnippet(final Reader in, final int step, final boolean createMarkers) {
		super(in);
		this.step = step;
		this.createMarkers = createMarkers;
	}

	private XmlSnippet(final Reader in) {
		super(in);
		this.step = Integer.MAX_VALUE;
		this.createMarkers = false;
	}

	@Override
	public int read() throws IOException {
		if (!this.readLine_())
			return -1;

		final int ch = this._line.charAt(this._linePos);
		this._linePos++;
		if (this._linePos == this._line.length()) {
			this._line = null;
		}
		return ch;
	}

	private boolean readLine_() throws IOException {
		while (this._line == null || this._line.length() == 0) {
			this._line = this.tokenizer.getToken(this.in);
			if (this._line == null) {
				return false;
			}
			this._line = this.transformLine(this._line);
			this._linePos = 0;
			if (this._line != null && this.tokenizer.getPostToken().length() != 0) {
				this._line = this._line + this.tokenizer.getPostToken();
			}
		}
		return true;
	}

	@Override
	public Snippets parse() throws IOException {
		String line = null;
		final Snippets snippets = new Snippets();

		final Stack<SnippetBuilder> snippetTree = new Stack<SnippetBuilder>();

		for (;;) {
			line = this.tokenizer.getToken(this.in);
			if (line == null) {
				break;
			}
			{
				final Token token = this.next(line);
				switch (token) {
				case SNIPPET_DEFINE:
					break;
				case SNIPPET_BEGIN:
					final SnippetBuilder current = this.parseSnippet(line.trim());
					if (!snippetTree.empty()) {
						current.setParent(snippetTree.peek());
					}
					snippetTree.push(current);
					break;
				case SNIPPET_FROM:
					assert (!snippetTree.empty());
					snippetTree.peek().switchToFrom();
					break;
				case SNIPPET_TO:
					assert (!snippetTree.empty());
					snippetTree.peek().switchToTo();
					break;
				case SNIPPET_END_FROMTO:
					break; // ignore
				case SNIPPET_END:
					assert (!snippetTree.empty());
					// close the snippet
					final SnippetBuilder sb = snippetTree.pop();
					snippets.add(sb.step, sb.asSnippet());
					break;
				default:
					if (!snippetTree.empty())
						snippetTree.peek().addCode(line);
					break;
				}
			}
		}
		return snippets;
	}

	private enum Token {
		LINE, SNIPPET_DEFINE, SNIPPET_BEGIN, SNIPPET_END, SNIPPET_FROM, SNIPPET_TO, SNIPPET_END_FROMTO
	}

	private Token next(final String line) {
		final String tline = line.trim();
		if (tline.isEmpty())
			return Token.LINE;
		if (tline.matches("<.* xmlns:snippet=\".*\".*>")) // snippet xml namespace define
			return Token.SNIPPET_DEFINE;
		if (tline.matches("<snippet:(insert|remove|fromto).*>"))
			return Token.SNIPPET_BEGIN;
		if (tline.matches("<snippet:from\\s*>"))
			return Token.SNIPPET_FROM;
		if (tline.matches("<snippet:to\\s*>"))
			return Token.SNIPPET_TO;
		if (tline.matches("</snippet:(to|from)\\s*>"))
			return Token.SNIPPET_END_FROMTO;
		if (tline.matches("</snippet:\\w+\\s*>"))
			return Token.SNIPPET_END;
		return Token.LINE;
	}

	private final Stack<SnippetTransformer> stepStack = new Stack<SnippetTransformer>();

	private String transformLine(final String line) {
		final Token token = this.next(line);
		switch (token) {
		case SNIPPET_DEFINE:
			return line.replaceFirst("xmlns:snippet=\".*\"", ""); // cut out namespace definition
		case SNIPPET_BEGIN:
			this.stepStack.push(this.extractSnippetTransformer(line.trim()));
			return this.stepStack.peek().begin(line);
		case SNIPPET_FROM:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippetTransformer);
			return ((FromToSnippetTransformer) this.stepStack.peek()).from(line);
		case SNIPPET_TO:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippetTransformer);
			return ((FromToSnippetTransformer) this.stepStack.peek()).to(line);
		case SNIPPET_END_FROMTO:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippetTransformer);
			return null;
		case SNIPPET_END:
			assert (!this.stepStack.isEmpty());
			return this.stepStack.pop().end(line);
		default:
			if (this.stepStack.isEmpty())
				return line;
			return this.transformSnippetLine(line);
		}
	}

	private String transformSnippetLine(String line) {
		// back to forth
		for (int i = this.stepStack.size() - 1; i >= 0; --i) {
			line = this.stepStack.get(i).line(line);
			if(line == null)
				return null;
		}
		return line;
	}

	private class SnippetBuilder {
		public Snippet.Action action;
		public final int step;
		public final int subStep;
		private final String description;
		private final StringBuilder codeThen = new StringBuilder();
		private final StringBuilder codeElse = new StringBuilder();
		private SnippetBuilder parent = null;
		private boolean inFrom = false, inTo = false;

		public SnippetBuilder(final Action action, final int step, final int subStep, final String description) {
			this.action = action;
			this.step = step;
			this.subStep = subStep;
			this.description = description;
		}

		public void setParent(final SnippetBuilder parent) {
			this.parent = parent;
			parent.addCode(createMarker(this.step, this.subStep)); // create a marker for the new snippet in the old snippet
		}

		public void addCode(final String line) {
			if (this.inTo)
				this.codeElse.append(line).append(XmlSnippet.this.tokenizer.getPostToken());
			else if (this.action != Action.FROM_TO || this.inFrom) {
				if ((this.action == Action.REMOVE || this.action == Action.FROM_TO) && this.parent != null) {
					this.parent.addCode(line); // add the code to the parent
				}
				this.codeThen.append(line).append(XmlSnippet.this.tokenizer.getPostToken());
			}
		}

		public void switchToFrom() {
			this.inFrom = true;
		}

		public void switchToTo() {
			this.inTo = true;
		}

		public Snippet asSnippet() {
			switch (this.action) {
			case INSERT:
				return Snippet.insert(this.subStep, this.description, this.codeThen.toString());
			case REMOVE:
				return Snippet.remove(this.subStep, this.description, this.codeThen.toString());
			case FROM_TO:
				return Snippet.from_to(this.subStep, this.description, this.codeThen.toString(), this.codeElse.toString());
			}
			throw new IllegalStateException();
		}

	}

	private SnippetTransformer extractSnippetTransformer(final String tline) {
		final SnippetBuilder s = this.parseSnippet(tline);

		switch(s.action) {
		case INSERT:
			return new InsertSnippetTransformer(s.step, s.subStep);
		case REMOVE:
			return new RemoveSnippetTransformer(s.step, s.subStep);
		case FROM_TO:
			return new FromToSnippetTransformer(s.step, s.subStep);
		default:
			throw new IllegalStateException("invalid snippet begin line: " + tline);
		}
	}

	private SnippetBuilder parseSnippet(final String tline) {
		final Pattern p = Pattern.compile("<snippet:(\\w+)\\s+step=\"(\\d+)\"\\s+subStep=\"(\\d+)\"\\s+description=\"(.*)\"\\s*>");
		final Matcher matcher = p.matcher(tline);
		if (!matcher.find())
			throw new IllegalStateException("invalid snippet begin line: " + tline);
		final int step = Integer.parseInt(matcher.group(2));
		final int subStep = Integer.parseInt(matcher.group(3));
		if ("INSERT".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.INSERT, step, subStep, matcher.group(4));
		else if ("REMOVE".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.REMOVE, step, subStep, matcher.group(4));
		else if ("fromto".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.FROM_TO, step, subStep, matcher.group(4));
		else
			throw new IllegalStateException("invalid snippet begin line: " + tline);
	}

	static String createMarker(final int step, final int substep) {
		return String.format("<!--**********%d.%d**********-->", step, substep);
	}

	static String extractIntention(final String line) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < line.length(); ++i) {
			if (line.charAt(i) >= ' ')
				break;
			b.append(line.charAt(i));
		}
		return b.toString();
	}

	static String intention(final int intention) {
		assert (intention >= 0);
		final char[] i = new char[intention];
		Arrays.fill(i, '\t');
		return new String(i);
	}

	private static interface SnippetTransformer {
		String begin(String line);
		String line(String line);

		String end(String line);
	}

	public abstract class BaseSnippetTransformer implements SnippetTransformer {
		protected final int snippetStep;
		protected final int snippetSubStep;

		public BaseSnippetTransformer(final int snippetStep, final int snippetSubStep) {
			this.snippetStep = snippetStep;
			this.snippetSubStep = snippetSubStep;
		}

		@Override
		public final String end(final String line) {
			return null;
		}

		@Override
		public final String begin(final String line) {
			if (XmlSnippet.this.createMarkers && !this.afterStep())
				return extractIntention(line) + createMarker(this.snippetStep, this.snippetSubStep);
			return null;
		}

		protected final String correctIntention(final String line, final int correction) {
			if (correction > 0) {
				return XmlSnippet.intention(correction) + line;
			} else {
				String intention = extractIntention(line);
				final String rest = line.substring(intention.length());
				intention = intention.replaceAll("    ", "\t");
				if (intention.length() <= -correction)
					return rest;
				return intention.substring(0, intention.length() + correction) + rest;
			}
		}

		protected boolean afterStep() {
			return this.snippetStep <= XmlSnippet.this.step;
		}
	}

	private class InsertSnippetTransformer extends BaseSnippetTransformer {

		public InsertSnippetTransformer(final int snippetStep, final int snippetSubStep) {
			super(snippetStep, snippetSubStep);
		}

		@Override
		public String line(final String line) {
			return this.afterStep() ? this.correctIntention(line, -1) : null;
		}
	}

	private class RemoveSnippetTransformer extends BaseSnippetTransformer {

		public RemoveSnippetTransformer(final int snippetStep, final int snippetSubStep) {
			super(snippetStep, snippetSubStep);
		}

		@Override
		public String line(final String line) {
			return this.afterStep() ? null : this.correctIntention(line, -1);
		}
	}

	private class FromToSnippetTransformer extends BaseSnippetTransformer {

		private boolean inFrom;
		private boolean inTo;

		public FromToSnippetTransformer(final int snippetStep, final int snippetSubStep) {
			super(snippetStep, snippetSubStep);
		}

		public String from(final String line) {
			this.inFrom = true;
			return null;
		}

		public String to(final String line) {
			this.inTo = true;
			return null;
		}

		@Override
		public String line(final String line) {
			if (this.inTo)
				return this.afterStep() ? this.correctIntention(line, -2) : null;
			else if (this.inFrom)
				return this.afterStep() ? null : this.correctIntention(line, -2);
			else
				return null;
		}
	}

}
