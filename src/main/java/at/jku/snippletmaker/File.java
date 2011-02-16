/*$Id$*/
package at.jku.snippletmaker;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import at.jku.snippletmaker.Snipplets.SnippletStep;

public final class File extends SnippletTask {

	private Type type = Type.html;
	private java.io.File file = new java.io.File("snipplets.html");


	public void setType(final Type type) {
		this.type = type;
	}

	public void setFile(final java.io.File file) {
		this.file = file;
	}

	@Override
	public void execute() throws BuildException {
		assert (this.type != null);
		assert (this.file != null);

		try {
			final Properties p = new Properties();
			try {
				p.load(this.getClass().getResourceAsStream("/renderer-" + this.type.name() + ".properties"));
			} catch (final IOException e) {
				this.log("invalid renderer " + this.type, e, Project.MSG_ERR);
				return;
			}
			final Snipplets snipplets = this.parseSnipplets();


			final String endl = System.getProperty("line.separator");
			final MessageFormat head = new MessageFormat(p.getProperty("file.begin"));
			final MessageFormat tail = new MessageFormat(p.getProperty("file.end"));
			final MessageFormat begin = new MessageFormat(p.getProperty("step.begin"));
			final MessageFormat between = new MessageFormat(p.getProperty("step.between"));
			final MessageFormat sinsert = new MessageFormat(p.getProperty("snipplet.insert"));
			final MessageFormat sremove = new MessageFormat(p.getProperty("snipplet.remove"));
			final MessageFormat sfromto = new MessageFormat(p.getProperty("snipplet.fromto"));
			final MessageFormat sbetween = new MessageFormat(p.getProperty("snipplet.between"));

			PrintWriter writer = null;
			try {
				writer = new PrintWriter(this.file);

				writer.append(head.format(this.asArgs(endl)));
				for (final SnippletStep step : snipplets) {
					writer.append(begin.format(this.asArgs(endl, step.getStep())));
					for (final Snipplet snipplet : step) {
						final String fullStep = step.getStep() + "." + snipplet.getSubStep();
						switch (snipplet.getAction()) {
						case INSERT:
							writer.append(sinsert.format(this.asArgs(endl, fullStep, snipplet.getDescription(), snipplet.getCodeToInsert())));
							break;
						case REMOVE:
							writer.append(sremove.format(this.asArgs(endl, fullStep, snipplet.getDescription(), snipplet.getCodeToRemove())));
							break;
						case FROM_TO:
							writer.append(sfromto.format(this.asArgs(endl, fullStep, snipplet.getDescription(), snipplet.getCodeToRemove(),
									snipplet.getCodeToInsert())));
							break;
						}
						writer.append(sbetween.format(this.asArgs(endl)));
					}
					writer.append(between.format(this.asArgs(endl)));
				}
				writer.append(tail.format(this.asArgs(endl)));
			} catch (final IOException e) {
				this.log("can't create snipplet file: " + this.file, e, Project.MSG_ERR);
			} finally {
				if (writer != null)
					writer.close();
			}
		} catch (final Throwable e) {
			e.printStackTrace();
			this.log("error: " + e.getMessage(), e, Project.MSG_ERR);
		}
	}

	private Object[] asArgs(final Object... args) {
		return args;
	}

	public static enum Type {
		html, txt, latex
	}
}
