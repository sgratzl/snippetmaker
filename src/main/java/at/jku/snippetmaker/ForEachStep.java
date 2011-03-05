/*$Id$*/
package at.jku.snippetmaker;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;

public class ForEachStep extends SnippetTask {
	private MacroDef macroDef;

	public Object createSequential() {
		this.macroDef = new MacroDef();
		this.macroDef.setProject(this.getProject());
		return this.macroDef.createSequential();
	}

	@Override
	public void execute() {
		if (this.macroDef == null) {
			throw new BuildException("You must supply an embedded sequential " + "to perform");
		}

		final int stepSize = this.parseSnipplets().getStepSize();

		// Create a macro attribute
		final MacroDef.Attribute attribute = new MacroDef.Attribute();
		attribute.setName("step");
		this.macroDef.addConfiguredAttribute(attribute);

		for (int i = 0; i < stepSize; i++) {
			this.doSequentialIteration(i + "");
		}
	}

	private void doSequentialIteration(final String val) {
		final MacroInstance instance = new MacroInstance();
		instance.setProject(this.getProject());
		instance.setOwningTarget(this.getOwningTarget());
		instance.setMacroDef(this.macroDef);
		instance.setDynamicAttribute("step", val);

		instance.execute();
	}
}
