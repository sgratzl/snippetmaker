/*$Id$*/
package at.jku.snippletmaker;

import org.apache.tools.ant.BuildException;

public final class StepSize extends SnippletTask {

	private String prop = "stepSize";

	public void setAddProperty(final String prop) {
		this.prop  = prop;
	}

	@Override
	public void execute() throws BuildException {
		assert (this.prop != null);
		final Snipplets s = this.parseSnipplets();

		this.getProject().setNewProperty(this.prop, "" + s.getStepSize());
	}
}
