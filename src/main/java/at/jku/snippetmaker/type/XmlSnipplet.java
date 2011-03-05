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
import at.jku.snippetmaker.SnippetParser;
import at.jku.snippetmaker.Snippets;
import at.jku.snippetmaker.Snippet.Action;

public final class XmlSnipplet extends BaseFilterReader implements SnippetParser {

	private final int step;
	private final boolean createMarkers;
	private final LineTokenizer tokenizer = new LineTokenizer();

	private String _line = null;
	private int _linePos = 0;

	public static BaseFilterReader createFilterReader(final Reader in, final int step, final boolean createMarkers) {
		return new XmlSnipplet(in, step, createMarkers);
	}

	public static SnippetParser createParser(final Reader in) {
		return new XmlSnipplet(in);
	}

	private XmlSnipplet(final Reader in, final int step, final boolean createMarkers) {
		super(in);
		this.step = step;
		this.createMarkers = createMarkers;
	}

	private XmlSnipplet(final Reader in) {
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
		final Snippets snipplets = new Snippets();

		final Stack<SnippletBuilder> snippletTree = new Stack<SnippletBuilder>();

		for (;;) {
			line = this.tokenizer.getToken(this.in);
			if (line == null) {
				break;
			}
			{
				final Token token = this.next(line);
				switch (token) {
				case SNIPPLET_DEFINE:
					break;
				case SNIPPLET_BEGIN:
					final SnippletBuilder current = this.parseSnipplet(line.trim());
					if (!snippletTree.empty()) {
						current.setParent(snippletTree.peek());
					}
					snippletTree.push(current);
					break;
				case SNIPPLET_FROM:
					assert (!snippletTree.empty());
					snippletTree.peek().switchToFrom();
					break;
				case SNIPPLET_TO:
					assert (!snippletTree.empty());
					snippletTree.peek().switchToTo();
					break;
				case SNIPPLET_END_FROMTO:
					break; // ignore
				case SNIPPLET_END:
					assert (!snippletTree.empty());
					// close the snipplet
					final SnippletBuilder sb = snippletTree.pop();
					snipplets.add(sb.step, sb.asSnipplet());
					break;
				default:
					if (!snippletTree.empty())
						snippletTree.peek().addCode(line);
					break;
				}
			}
		}
		return snipplets;
	}

	private enum Token {
		LINE, SNIPPLET_DEFINE, SNIPPLET_BEGIN, SNIPPLET_END, SNIPPLET_FROM, SNIPPLET_TO, SNIPPLET_END_FROMTO
	}

	private Token next(final String line) {
		final String tline = line.trim();
		if (tline.isEmpty())
			return Token.LINE;
		if (tline.matches("<.* xmlns:snipplet=\".*\".*>")) // snipplet xml namespace define
			return Token.SNIPPLET_DEFINE;
		if (tline.matches("<snipplet:(insert|remove|fromto).*>"))
			return Token.SNIPPLET_BEGIN;
		if (tline.matches("<snipplet:from\\s*>"))
			return Token.SNIPPLET_FROM;
		if (tline.matches("<snipplet:to\\s*>"))
			return Token.SNIPPLET_TO;
		if (tline.matches("</snipplet:(to|from)\\s*>"))
			return Token.SNIPPLET_END_FROMTO;
		if (tline.matches("</snipplet:\\w+\\s*>"))
			return Token.SNIPPLET_END;
		return Token.LINE;
	}

	private final Stack<SnippletTransformer> stepStack = new Stack<SnippletTransformer>();

	private String transformLine(final String line) {
		final Token token = this.next(line);
		switch (token) {
		case SNIPPLET_DEFINE:
			return line.replaceFirst("xmlns:snipplet=\".*\"", ""); // cut out namespace definition
		case SNIPPLET_BEGIN:
			this.stepStack.push(this.extractSnippletTransformer(line.trim()));
			return this.stepStack.peek().begin(line);
		case SNIPPLET_FROM:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippletTransformer);
			return ((FromToSnippletTransformer) this.stepStack.peek()).from(line);
		case SNIPPLET_TO:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippletTransformer);
			return ((FromToSnippletTransformer) this.stepStack.peek()).to(line);
		case SNIPPLET_END_FROMTO:
			assert (!this.stepStack.isEmpty());
			assert (this.stepStack.peek() instanceof FromToSnippletTransformer);
			return null;
		case SNIPPLET_END:
			assert (!this.stepStack.isEmpty());
			return this.stepStack.pop().end(line);
		default:
			if (this.stepStack.isEmpty())
				return line;
			return this.transformSnippletLine(line);
		}
	}

	private String transformSnippletLine(String line) {
		// back to forth
		for (int i = this.stepStack.size() - 1; i >= 0; --i) {
			line = this.stepStack.get(i).line(line);
			if(line == null)
				return null;
		}
		return line;
	}

	private class SnippletBuilder {
		public Snippet.Action action;
		public final int step;
		public final int subStep;
		private final String description;
		private final StringBuilder codeThen = new StringBuilder();
		private final StringBuilder codeElse = new StringBuilder();
		private SnippletBuilder parent = null;
		private boolean inFrom = false, inTo = false;

		public SnippletBuilder(final Action action, final int step, final int subStep, final String description) {
			this.action = action;
			this.step = step;
			this.subStep = subStep;
			this.description = description;
		}

		public void setParent(final SnippletBuilder parent) {
			this.parent = parent;
			parent.addCode(createMarker(this.step, this.subStep)); // create a marker for the new snipplet in the old snipplet
		}

		public void addCode(final String line) {
			if (this.inTo)
				this.codeElse.append(line).append(XmlSnipplet.this.tokenizer.getPostToken());
			else if (this.action != Action.FROM_TO || this.inFrom) {
				if ((this.action == Action.REMOVE || this.action == Action.FROM_TO) && this.parent != null) {
					this.parent.addCode(line); // add the code to the parent
				}
				this.codeThen.append(line).append(XmlSnipplet.this.tokenizer.getPostToken());
			}
		}

		public void switchToFrom() {
			this.inFrom = true;
		}

		public void switchToTo() {
			this.inTo = true;
		}

		public Snippet asSnipplet() {
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

	private SnippletTransformer extractSnippletTransformer(final String tline) {
		final SnippletBuilder s = this.parseSnipplet(tline);

		switch(s.action) {
		case INSERT:
			return new InsertSnippletTransformer(s.step, s.subStep);
		case REMOVE:
			return new RemoveSnippletTransformer(s.step, s.subStep);
		case FROM_TO:
			return new FromToSnippletTransformer(s.step, s.subStep);
		default:
			throw new IllegalStateException("invalid snipplet begin line: " + tline);
		}
	}

	private SnippletBuilder parseSnipplet(final String tline) {
		final Pattern p = Pattern.compile("<snipplet:(\\w+)\\s+step=\"(\\d+)\"\\s+subStep=\"(\\d+)\"\\s+description=\"(.*)\"\\s*>");
		final Matcher matcher = p.matcher(tline);
		if (!matcher.find())
			throw new IllegalStateException("invalid snipplet begin line: " + tline);
		final int step = Integer.parseInt(matcher.group(2));
		final int subStep = Integer.parseInt(matcher.group(3));
		if ("INSERT".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.INSERT, step, subStep, matcher.group(4));
		else if ("REMOVE".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.REMOVE, step, subStep, matcher.group(4));
		else if ("fromto".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.FROM_TO, step, subStep, matcher.group(4));
		else
			throw new IllegalStateException("invalid snipplet begin line: " + tline);
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

	private static interface SnippletTransformer {
		String begin(String line);
		String line(String line);

		String end(String line);
	}

	public abstract class BaseSnippletTransformer implements SnippletTransformer {
		protected final int snippletStep;
		protected final int snippletSubStep;

		public BaseSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			this.snippletStep = snippletStep;
			this.snippletSubStep = snippletSubStep;
		}

		@Override
		public final String end(final String line) {
			return null;
		}

		@Override
		public final String begin(final String line) {
			if (XmlSnipplet.this.createMarkers && !this.afterStep())
				return extractIntention(line) + createMarker(this.snippletStep, this.snippletSubStep);
			return null;
		}

		protected final String correctIntention(final String line, final int correction) {
			if (correction > 0) {
				return XmlSnipplet.intention(correction) + line;
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
			return this.snippletStep <= XmlSnipplet.this.step;
		}
	}

	private class InsertSnippletTransformer extends BaseSnippletTransformer {

		public InsertSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
		}

		@Override
		public String line(final String line) {
			return this.afterStep() ? this.correctIntention(line, -1) : null;
		}
	}

	private class RemoveSnippletTransformer extends BaseSnippletTransformer {

		public RemoveSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
		}

		@Override
		public String line(final String line) {
			return this.afterStep() ? null : this.correctIntention(line, -1);
		}
	}

	private class FromToSnippletTransformer extends BaseSnippletTransformer {

		private boolean inFrom;
		private boolean inTo;

		public FromToSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
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
