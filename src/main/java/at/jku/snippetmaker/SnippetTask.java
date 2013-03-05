/*$Id$*/
package at.jku.snippetmaker;

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

import at.jku.snippetmaker.type.CppSnippet;
import at.jku.snippetmaker.type.XmlSnippet;

public abstract class SnippetTask extends Task {
	private final List<ResourceCollection> collections = new ArrayList<ResourceCollection>();

	public final void add(final ResourceCollection collection) {
		this.collections.add(collection);
	}

	protected Snippets parseSnippets() {
		final Snippets snippets = new Snippets();

		for (final ResourceCollection r : this.collections) {
			for (final Iterator<?> it = r.iterator(); it.hasNext();) {
				final Object res = it.next();
				assert (res instanceof Resource);
				snippets.merge(this.parseSnippets(((Resource) res)));
			}
		}
		System.out.println(snippets);
		this.log("dump\n" + snippets, Project.MSG_DEBUG);

		return snippets;
	}

	String[] cppExtensions = { "c", "h", "cpp", "hpp", "cxx", "hxx", "vert", "frag", "geom", "glsl", "cu", "cuh", "cl" };

	private Snippets parseSnippets(final Resource resource) {
		final String ext = this.getExtension(resource.getName());
		try {
			for (final String cppExtension : this.cppExtensions)
				if (cppExtension.equalsIgnoreCase(ext)) {
					this.log("parsing resource: " + resource.getName() + " -> using cpp", Project.MSG_INFO);
					return CppSnippet.createParser(new BufferedReader(new InputStreamReader(resource.getInputStream()))).parse();
				}
			if ("xml".equalsIgnoreCase(ext)) {
				this.log("parsing resource: " + resource.getName() + " -> using xml", Project.MSG_INFO);
				return XmlSnippet.createParser(new BufferedReader(new InputStreamReader(resource.getInputStream()))).parse();
			} else {
				this.log("skipping resource: " + resource.getName() + " -> unknown how to parse snippets", Project.MSG_DEBUG);
			}
		} catch (final IOException e) {
			this.log("skipping resource: " + resource.getName() + " -> error opening reader", e, Project.MSG_WARN);
		}
		return Snippets.empty;

	}

	private String getExtension(final String name) {
		return name.substring(name.lastIndexOf('.') + 1);
	}
}
