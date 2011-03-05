/*$Id$*/
package at.jku.snippetmaker;

import org.apache.tools.ant.BuildException;

public final class StepSize extends SnippetTask {

	private String prop = "stepSize";

	public void setAddProperty(final String prop) {
		this.prop  = prop;
	}

	@Override
	public void execute() throws BuildException {
		assert (this.prop != null);
		final Snippets s = this.parseSnippets();

		this.getProject().setNewProperty(this.prop, "" + s.getStepSize());
	}
}
