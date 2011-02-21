/*$Id$*/
package at.jku.snippletmaker.type;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.util.LineTokenizer;

import at.jku.snippletmaker.Snipplet;
import at.jku.snippletmaker.Snipplet.Action;
import at.jku.snippletmaker.SnippletParser;
import at.jku.snippletmaker.Snipplets;

public final class CppSnipplet extends BaseFilterReader implements SnippletParser {

	private final int step;
	private final boolean createMarkers;
	private final LineTokenizer tokenizer = new LineTokenizer();

	private String _line = null;
	private int _linePos = 0;

	public static BaseFilterReader createFilterReader(final Reader in, final int step, final boolean createMarkers) {
		return new CppSnipplet(in, step, createMarkers);
	}

	public static SnippletParser createParser(final Reader in) {
		return new CppSnipplet(in);
	}

	private CppSnipplet(final Reader in, final int step, final boolean createMarkers) {
		super(in);
		this.step = step;
		this.createMarkers = createMarkers;
	}

	private CppSnipplet(final Reader in) {
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
	public Snipplets parse() throws IOException {
		String line = null;
		final Snipplets snipplets = new Snipplets();

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
				case SNIPPLET_INCLUDE:
					break;
				case SNIPPLET_BEGIN:
					final SnippletBuilder current = this.parseSnipplet(line.trim());
					if(!snippletTree.empty())
						snippletTree.peek().addCode(createMarker(current.step, current.subStep)); // create a marker for the new snipplet in the old snipplet
					snippletTree.push(current);
					break;
				case IF:
					if(!snippletTree.empty()) {
						snippletTree.peek().nestedIfs++;
						snippletTree.peek().addCode(line);
					}
					break;
				case ELIF:
				case ELSE:
					if (!snippletTree.empty() && snippletTree.peek().nestedIfs > 0) {
						snippletTree.peek().addCode(line);
					} else if (!snippletTree.empty())
						snippletTree.peek().switchToElseOrElseIf();
					break;
				case ENDIF:
					if (!snippletTree.empty() && snippletTree.peek().nestedIfs > 0) {
						snippletTree.peek().addCode(line);
						snippletTree.peek().nestedIfs--;
					} else if (!snippletTree.empty()) {
						// close the snipplet
						final SnippletBuilder sb = snippletTree.pop();
						assert (sb.nestedIfs == 0);
						snipplets.add(sb.step, sb.asSnipplet());
					}
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
		LINE, SNIPPLET_INCLUDE, SNIPPLET_DEFINE, SNIPPLET_BEGIN, IF, ELIF, ELSE, ENDIF
	}

	private Token next(final String line) {
		final String tline = line.trim();
		if (tline.isEmpty())
			return Token.LINE;
		if (tline.matches("# *include *[\"<]snipplets\\.h[\">]")) // snipplet include
			return Token.SNIPPLET_INCLUDE;
		if (tline.matches("# *define +SNIPPLET_STEP.*")) // snipplet step definition
			return Token.SNIPPLET_DEFINE;

		if (tline.matches("# *if +.*")) {
			if (tline.matches("# *if +SNIPPLET_\\w+.*"))
				return Token.SNIPPLET_BEGIN;
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

	private final Stack<SnippletTransformer> stepStack = new Stack<SnippletTransformer>();

	private String transformLine(final String line) {
		final Token token = this.next(line);
		switch (token) {
		case SNIPPLET_DEFINE:
		case SNIPPLET_INCLUDE:
			return null;
		case SNIPPLET_BEGIN:
			this.stepStack.push(this.extractSnippletTransformer(line.trim()));
			return this.stepStack.peek().begin(line);
		case IF:
			this.stepStack.push(new IdentitySnippletTransformer()); // ordinary if but have to remember
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
		public Snipplet.Action action;
		public final int step;
		public final int subStep;
		private final String description;
		private final StringBuilder codeThen = new StringBuilder();
		private final StringBuilder codeElse = new StringBuilder();
		public int nestedIfs = 0;
		private boolean inElsePath = false;

		public SnippletBuilder(final Action action, final int step, final int subStep, final String description) {
			this.action = action;
			this.step = step;
			this.subStep = subStep;
			this.description = description;
		}

		public void addCode(final String line) {
			if (this.inElsePath)
				this.codeElse.append(line).append(CppSnipplet.this.tokenizer.getPostToken());
			else
				this.codeThen.append(line).append(CppSnipplet.this.tokenizer.getPostToken());
		}

		public void switchToElseOrElseIf() {
			this.inElsePath = true;
		}

		public Snipplet asSnipplet() {
			switch (this.action) {
			case INSERT:
				return Snipplet.insert(this.subStep, this.description, this.codeThen.toString());
			case REMOVE:
				return Snipplet.remove(this.subStep, this.description, this.codeThen.toString());
			case FROM_TO:
				return Snipplet.from_to(this.subStep, this.description, this.codeThen.toString(), this.codeElse.toString());
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
		final Pattern p = Pattern.compile("# *if +SNIPPLET_(\\w+)\\s*\\(\\s*(\\d+),\\s*(\\d+),\\s*\"(.*)\"\\s*(,.*)?\\)");
		final Matcher matcher = p.matcher(tline);
		if (!matcher.find())
			throw new IllegalStateException("invalid snipplet begin line: >>'" + tline + "<<");
		final int step = Integer.parseInt(matcher.group(2));
		final int subStep = Integer.parseInt(matcher.group(3));
		if ("INSERT".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.INSERT, step, subStep, matcher.group(4));
		else if ("REMOVE".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.REMOVE, step, subStep, matcher.group(4));
		else if ("FROM_TO".equalsIgnoreCase(matcher.group(1)))
			return new SnippletBuilder(Action.FROM_TO, step, subStep, matcher.group(4));
		else
			throw new IllegalStateException("invalid snipplet begin line: " + tline);
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

	private static interface SnippletTransformer {
		String begin(String line);
		String line(String line);

		String elif(String line);
		String else_(String line);
		String end(String line);
	}

	private static class IdentitySnippletTransformer implements SnippletTransformer {
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

	public abstract class BaseSnippletTransformer implements SnippletTransformer {
		protected boolean inElsePath = false;
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
			if (CppSnipplet.this.createMarkers && !this.afterStep())
				return extractIntention(line) + createMarker(this.snippletStep, this.snippletSubStep);
			return null;
		}

		protected boolean afterStep() {
			return this.snippletStep <= CppSnipplet.this.step;
		}

		@Override
		public final String elif(final String line) {
			throw new IllegalStateException("snipplets doesn't support elif");
		}

		@Override
		public final String else_(final String line) {
			this.inElsePath = true;
			return null;
		}
	}

	private class InsertSnippletTransformer extends BaseSnippletTransformer {

		public InsertSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
		}

		@Override
		public String line(final String line) {
			if (this.inElsePath)
				return null;
			else
				return this.afterStep() ? line : null;
		}
	}

	private class RemoveSnippletTransformer extends BaseSnippletTransformer {

		public RemoveSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
		}

		@Override
		public String line(final String line) {
			if (this.inElsePath)
				return null;
			else
				return this.afterStep() ? null : line;
		}
	}

	private class FromToSnippletTransformer extends BaseSnippletTransformer {

		public FromToSnippletTransformer(final int snippletStep, final int snippletSubStep) {
			super(snippletStep, snippletSubStep);
		}

		@Override
		public String line(final String line) {
			if (this.inElsePath)
				return this.afterStep() ? line : null;
			else
				return this.afterStep() ? null : line;
		}
	}
}
