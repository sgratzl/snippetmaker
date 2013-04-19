/*$Id$*/
package at.jku.snippetmaker;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class Snippet implements Comparable<Snippet> {
	public enum Action {
		INSERT, REMOVE, FROM_TO
	}

	public enum Option {
		USE_COMMENTS, NO_MARKER, USE_INSERT_COMMENTS, USE_REMOVE_COMMENTS, USE_CODE_MARKERS
	}

	private final int subStep;
	private final Action action;
	private final String code_insert;
	private final String code_remove;
	private final String description;
	private final Set<Option> options;

	private Snippet(final Action action, final int subStep, final String description, final String code, final String code2, final Set<Option> options) {
		this.action = action;
		this.description = description;
		this.subStep = subStep;
		this.code_insert = code;
		this.code_remove = code2;
		this.options = options;
	}

	public static Snippet insert(final int subStep, final String description, final String code, final Set<Option> options) {
		return new Snippet(Action.INSERT, subStep, description, code, null, options);
	}

	public static Snippet insert(final int subStep, final String description, final String code, final Option... options) {
		return insert(subStep, description, code, asSet(options));
	}

	public static Snippet remove(final int subStep, final String description, final String code, final Set<Option> options) {
		return new Snippet(Action.REMOVE, subStep, description, null, code, options);
	}

	public static Snippet remove(final int subStep, final String description, final String code, final Option... options) {
		return remove(subStep, description, code, asSet(options));
	}

	public static Snippet from_to(final int subStep, final String description, final String from, final String to, final Set<Option> options) {
		return new Snippet(Action.FROM_TO, subStep, description, to, from, options);
	}

	public static Snippet from_to(final int subStep, final String description, final String from, final String to, final Option... options) {
		return from_to(subStep, description, from, to, asSet(options));
	}

	private static Set<Option> asSet(final Option[] options) {
		if (options.length > 0)
			return EnumSet.of(options[0], options);
		return Collections.emptySet();
	}


	public String getCodeToInsert() {
		return this.code_insert;
	}

	public String getCodeToRemove() {
		return this.code_remove;
	}

	public Action getAction() {
		return this.action;
	}

	public String getDescription() {
		return this.description;
	}

	public boolean hasOption(final Option option) {
		return this.options.contains(option);
	}

	@Override
	public int compareTo(final Snippet o) {
		return this.getSubStep() - o.getSubStep();
	}

	@Override
	public String toString() {
		return String.format("%s:%s\ncode_insert\n%s\ncode_remove\n%s", this.action, this.description, this.code_insert,
				this.code_remove);
	}

	public int getSubStep() {
		return this.subStep;
	}

}
