/*$Id$*/
package at.jku.snippetmaker.type;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.util.LineTokenizer;

import at.jku.snippetmaker.Snippet;
import at.jku.snippetmaker.Snippet.Action;
import at.jku.snippetmaker.Snippet.Option;
import at.jku.snippetmaker.SnippetParser;
import at.jku.snippetmaker.Snippets;

public final class CppSnippet extends BaseFilterReader implements SnippetParser {

	private final int step;
	private final boolean createMarkers;
	private final LineTokenizer tokenizer = new LineTokenizer();

	private String _line = null;
	private int _linePos = 0;

	public static BaseFilterReader createFilterReader(final Reader in, final int step, final boolean createMarkers) {
		return new CppSnippet(in, step, createMarkers);
	}

	public static SnippetParser createParser(final Reader in) {
		return new CppSnippet(in);
	}

	private CppSnippet(final Reader in, final int step, final boolean createMarkers) {
		super(in);
		this.step = step;
		this.createMarkers = createMarkers;
	}

	private CppSnippet(final Reader in) {
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
				case SNIPPET_INCLUDE:
					break;
				case SNIPPET_BEGIN:
					final SnippetBuilder current = this.parseSnippet(line.trim());
					if (!snippetTree.empty()) {
						current.setParent(snippetTree.peek());
					}
					snippetTree.push(current);
					break;
				case IF:
					if (!snippetTree.empty()) {
						snippetTree.peek().nestedIfs++;
						snippetTree.peek().addCode(line);
					}
					break;
				case ELIF:
				case ELSE:
					if (!snippetTree.empty() && snippetTree.peek().nestedIfs > 0) {
						snippetTree.peek().addCode(line);
					} else if (!snippetTree.empty())
						snippetTree.peek().switchToElseOrElseIf();
					break;
				case ENDIF:
					if (!snippetTree.empty() && snippetTree.peek().nestedIfs > 0) {
						snippetTree.peek().addCode(line);
						snippetTree.peek().nestedIfs--;
					} else if (!snippetTree.empty()) {
						// close the snippet
						final SnippetBuilder sb = snippetTree.pop();
						assert (sb.nestedIfs == 0);
						snippets.add(sb.step, sb.asSnippet());
					}
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
		LINE, SNIPPET_INCLUDE, SNIPPET_DEFINE, SNIPPET_BEGIN, IF, ELIF, ELSE, ENDIF
	}

	private Token next(final String line) {
		final String tline = line.trim();
		if (tline.isEmpty())
			return Token.LINE;
		if (tline.matches("# *include *[\"<]snippets\\.h[\">]")) // snippet include
			return Token.SNIPPET_INCLUDE;
		if (tline.matches("# *define +SNIPPET_.*")) // snippet step definition
			return Token.SNIPPET_DEFINE;

		if (tline.matches("# *if +.*")) {
			if (tline.matches("# *if +SNIPPET_\\w+.*"))
				return Token.SNIPPET_BEGIN;
			return Token.IF;
		}
		if (tline.matches("# *elif +.*"))
			return Token.ELIF;
		if (tline.matches("# *else.*"))
			return Token.ELSE;
		if (tline.matches("# *endif.*"))
			return Token.ENDIF;
		return Token.LINE;
	}

	private final Stack<SnippetTransformer> stepStack = new Stack<SnippetTransformer>();

	private String transformLine(final String line) {
		final Token token = this.next(line);
		switch (token) {
		case SNIPPET_DEFINE:
		case SNIPPET_INCLUDE:
			return null;
		case SNIPPET_BEGIN:
			this.stepStack.push(this.extractSnippetTransformer(line.trim()));
			return this.stepStack.peek().begin(line);
		case IF:
			this.stepStack.push(new IdentitySnippetTransformer()); // ordinary if but have to remember
			return this.stepStack.peek().begin(line);
		case ELSE:
			assert (!this.stepStack.isEmpty());
			return this.stepStack.peek().else_(line);
		case ELIF:
			assert (!this.stepStack.isEmpty());
			return this.stepStack.peek().elif(line);
		case ENDIF:
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

	private static Set<Option> parseOptions(final String options) {
		final Set<Option> r = EnumSet.noneOf(Snippet.Option.class);
		if (options == null || options.trim().length() == 0)
			return r;
		for (String option : options.split(",")) {
			option = option.trim();
			if (option.length() == 0)
				continue;
			if ("useComments".equalsIgnoreCase(option))
				r.add(Snippet.Option.USE_COMMENTS);
			else if ("noMarker".equalsIgnoreCase(option))
				r.add(Snippet.Option.NO_MARKER);
		}
		return r;
	}

	private class SnippetBuilder {
		public Snippet.Action action;
		public final int step;
		public final int subStep;
		private final String description;
		private final Set<Snippet.Option> options;
		private final StringBuilder codeThen = new StringBuilder();
		private final StringBuilder codeElse = new StringBuilder();
		private SnippetBuilder parent = null;
		public int nestedIfs = 0;
		private boolean inElsePath = false;

		public SnippetBuilder(final Action action, final int step, final int subStep, final String description, final String options) {
			this.action = action;
			this.step = step;
			this.subStep = subStep;
			this.description = description;
			this.options = parseOptions(options);
		}

		public void setParent(final SnippetBuilder parent) {
			this.parent = parent;
			if (CppSnippet.this.createMarkers && !this.hasOption(Snippet.Option.NO_MARKER))
				parent.addCode(createMarker(this.step, this.subStep)); // create a marker for the new snippet in the old snippet
		}

		public void addCode(final String line) {
			if (this.inElsePath) {
				this.codeElse.append(line).append(CppSnippet.this.tokenizer.getPostToken());
			} else {
				if ((this.action == Action.REMOVE || this.action == Action.FROM_TO) && this.parent != null) {
					this.parent.addCode(line); // add the code to the parent
				}
				this.codeThen.append(line).append(CppSnippet.this.tokenizer.getPostToken());
			}
		}

		public boolean hasOption(final Snippet.Option option) {
			return this.options.contains(option);
		}

		public void switchToElseOrElseIf() {
			this.inElsePath = true;
		}

		public Snippet asSnippet() {
			switch (this.action) {
			case INSERT:
				return Snippet.insert(this.subStep, this.description, this.codeThen.toString(), this.options);
			case REMOVE:
				return Snippet.remove(this.subStep, this.description, this.codeThen.toString(), this.options);
			case FROM_TO:
				return Snippet.from_to(this.subStep, this.description, this.codeThen.toString(), this.codeElse.toString(), this.options);
			}
			throw new IllegalStateException();
		}

	}

	private SnippetTransformer extractSnippetTransformer(final String tline) {
		final SnippetBuilder s = this.parseSnippet(tline);

		switch(s.action) {
		case INSERT:
			return new InsertSnippetTransformer(s);
		case REMOVE:
			return new RemoveSnippetTransformer(s);
		case FROM_TO:
			return new FromToSnippetTransformer(s);
		default:
			throw new IllegalStateException("invalid snippet begin line: " + tline);
		}
	}

	private SnippetBuilder parseSnippet(final String tline) {
		final Pattern p = Pattern.compile("# *if +SNIPPET_(\\w+)\\s*\\(\\s*(\\d+),\\s*(\\d+),\\s*\"(.*)\"\\s*(,.*)?\\)");
		final Matcher matcher = p.matcher(tline);
		if (!matcher.find())
			throw new IllegalStateException("invalid snippet begin line: >>'" + tline + "<<");
		final int step = Integer.parseInt(matcher.group(2));
		final int subStep = Integer.parseInt(matcher.group(3));
		final String options = (matcher.groupCount() > 4) ? matcher.group(5) : "";
		if ("INSERT".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.INSERT, step, subStep, matcher.group(4), options);
		else if ("REMOVE".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.REMOVE, step, subStep, matcher.group(4), options);
		else if ("FROM_TO".equalsIgnoreCase(matcher.group(1)))
			return new SnippetBuilder(Action.FROM_TO, step, subStep, matcher.group(4), options);
		else
			throw new IllegalStateException("invalid snippet begin line: " + tline);
	}

	static String createMarker(final int step, final int substep) {
		return String.format("/*----------%d.%d----------*/", step, substep);
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

	private static interface SnippetTransformer {
		String begin(String line);
		String line(String line);

		String elif(String line);
		String else_(String line);
		String end(String line);
	}

	private static class IdentitySnippetTransformer implements SnippetTransformer {
		@Override
		public String end(final String line) {
			return line;
		}
		@Override
		public String begin(final String line) {
			return line;
		}

		@Override
		public String line(final String line) {
			return line;
		}

		@Override
		public String elif(final String line) {
			return line;
		}

		@Override
		public String else_(final String line) {
			return line;
		}
	}

	public abstract class BaseSnippetTransformer implements SnippetTransformer {
		private boolean inElsePath = false;
		protected final SnippetBuilder snippet;

		public BaseSnippetTransformer(final SnippetBuilder snippet) {
			this.snippet = snippet;
		}

		@Override
		public final String end(final String line) {
			return null;
		}

		@Override
		public final String begin(final String line) {
			if (CppSnippet.this.createMarkers && !this.afterStep() && !this.snippet.hasOption(Snippet.Option.NO_MARKER))
				return extractIntention(line) + createMarker(this.snippet.step, this.snippet.subStep);
			return null;
		}

		protected boolean afterStep() {
			return this.snippet.step <= CppSnippet.this.step;
		}

		@Override
		public final String elif(final String line) {
			throw new IllegalStateException("snippets doesn't support elif");
		}

		@Override
		public final String else_(final String line) {
			this.inElsePath = true;
			return null;
		}

		protected final boolean inThen() {
			return !this.inElsePath;
		}

		protected final String commentOrNull(final String line) {
			return this.snippet.hasOption(Snippet.Option.USE_COMMENTS) ? ("//" + line) : null;
		}
	}

	private class InsertSnippetTransformer extends BaseSnippetTransformer {

		public InsertSnippetTransformer(final SnippetBuilder snippet) {
			super(snippet);
		}

		@Override
		public String line(final String line) {
			if (this.inThen())
				return this.afterStep() ? line : this.commentOrNull(line);
			return null;
		}
	}

	private class RemoveSnippetTransformer extends BaseSnippetTransformer {

		public RemoveSnippetTransformer(final SnippetBuilder snippet) {
			super(snippet);
		}

		@Override
		public String line(final String line) {
			if (this.inThen())
				return this.afterStep() ? this.commentOrNull(line) : line;
			return null;
		}
	}

	private class FromToSnippetTransformer extends BaseSnippetTransformer {

		public FromToSnippetTransformer(final SnippetBuilder snippet) {
			super(snippet);
		}

		@Override
		public String line(final String line) {
			if (this.inThen())
				return this.afterStep() ? this.commentOrNull(line) : line;
			else
				return this.afterStep() ? line : this.commentOrNull(line);
		}
	}
}
