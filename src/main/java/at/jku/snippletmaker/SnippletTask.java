/*$Id$*/
package at.jku.snippletmaker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

import at.jku.snippletmaker.type.CppSnipplet;
import at.jku.snippletmaker.type.XmlSnipplet;

public abstract class SnippletTask extends Task {
	private final List<ResourceCollection> collections = new ArrayList<ResourceCollection>();

	public final void add(final ResourceCollection collection) {
		this.collections.add(collection);
	}

	protected Snipplets parseSnipplets() {
		final Snipplets snipplets = new Snipplets();

		for (final ResourceCollection r : this.collections) {
			for (final Iterator<?> it = r.iterator(); it.hasNext();) {
				final Object res = it.next();
				assert (res instanceof Resource);
				snipplets.merge(this.parseSnipplets(((Resource) res)));
			}
		}
		this.log("dump\n" + snipplets, Project.MSG_DEBUG);

		return snipplets;
	}

	private Snipplets parseSnipplets(final Resource resource) {
		final String ext = this.getExtension(resource.getName());
		try {
			if ("cpp".equalsIgnoreCase(ext) || "h".equalsIgnoreCase(ext) || "glsl".equalsIgnoreCase(ext) || "frag".equalsIgnoreCase(ext)
					|| "vert".equalsIgnoreCase(ext) || "geom".equalsIgnoreCase(ext)) {
				this.log("parsing resource: " + resource.getName() + " -> using cpp", Project.MSG_INFO);
				return CppSnipplet.createParser(new BufferedReader(new InputStreamReader(resource.getInputStream()))).parse();
			} else if ("xml".equalsIgnoreCase(ext)) {
				this.log("parsing resource: " + resource.getName() + " -> using xml", Project.MSG_INFO);
				return XmlSnipplet.createParser(new BufferedReader(new InputStreamReader(resource.getInputStream()))).parse();
			} else {
				this.log("skipping resource: " + resource.getName() + " -> unknown how to parse snipplets", Project.MSG_DEBUG);
			}
		} catch (final IOException e) {
			this.log("skipping resource: " + resource.getName() + " -> error opening reader", e, Project.MSG_WARN);
		}
		return Snipplets.empty;

	}

	private String getExtension(final String name) {
		return name.substring(name.lastIndexOf('.') + 1);
	}
}
