/*$Id$*/
package at.jku.snippletmaker;

public final class Snipplet {
	public enum Action {
		INSERT, REMOVE, FROM_TO
	}

	private final Action action;
	private final String code_insert;
	private final String code_remove;
	private final String description;


	private Snipplet(final Action action, final String description, final String code, final String code2) {
		this.action = action;
		this.description = description;
		this.code_insert = code;
		this.code_remove = code2;
	}

	public static Snipplet insert(final String description, final String code) {
		return new Snipplet(Action.INSERT, description, code, null);
	}

	public static Snipplet remove(final String description, final String code) {
		return new Snipplet(Action.REMOVE, description, null, code);
	}

	public static Snipplet from_to(final String description, final String from, final String to) {
		return new Snipplet(Action.FROM_TO, description, to, from);
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
	public String toString() {
		return String.format("%s:%s\ncode_insert\n%s\ncode_remove\n%s", this.action, this.description, this.code_insert,
				this.code_remove);
	}

}
