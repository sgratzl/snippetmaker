/*$Id$*/
package at.jku.snippletmaker;

public final class Snipplet implements Comparable<Snipplet> {
	public enum Action {
		INSERT, REMOVE, FROM_TO
	}

	private final int subStep;
	private final Action action;
	private final String code_insert;
	private final String code_remove;
	private final String description;


	private Snipplet(final Action action, final int subStep, final String description, final String code, final String code2) {
		this.action = action;
		this.description = description;
		this.subStep = subStep;
		this.code_insert = code;
		this.code_remove = code2;
	}

	public static Snipplet insert(final int subStep, final String description, final String code) {
		return new Snipplet(Action.INSERT, subStep, description, code, null);
	}

	public static Snipplet remove(final int subStep, final String description, final String code) {
		return new Snipplet(Action.REMOVE, subStep, description, null, code);
	}

	public static Snipplet from_to(final int subStep, final String description, final String from, final String to) {
		return new Snipplet(Action.FROM_TO, subStep, description, to, from);
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

	@Override
	public int compareTo(final Snipplet o) {
		return this.getSubStep() - o.getSubStep();
	}

	@Override
	public String toString() {
		return String.format("%s:%s\ncode_insert\n%s\ncode_remove\n%s", this.action, this.description, this.code_insert,
				this.code_remove);
	}

	public int getSubStep() {
		return subStep;
	}

}
