/*$Id$*/
package at.jku.snippetmaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.tools.ant.types.Resource;

import at.jku.snippetmaker.Snippets.SnippetStep;

public final class Snippets implements Iterable<SnippetStep> {
	public static class SnippetStep implements Iterable<Snippet> {
		private final int step;
		private final List<Snippet> snippets = new ArrayList<Snippet>();
		private final List<Resource> resources = new ArrayList<Resource>();

		SnippetStep(final int step) {
			this.step = step;
		}

		public int getStep() {
			return this.step;
		}

		@Override
		public Iterator<Snippet> iterator() {
			return this.snippets.iterator();
		}

		void add(final Snippet snippet) {
			this.snippets.add(snippet);
			Collections.sort(this.snippets);
		}

		void add(final Resource resource) {
			this.resources.add(resource);
		}

		void merge(final SnippetStep that) {
			this.snippets.addAll(that.snippets);
			this.resources.addAll(that.resources);
			Collections.sort(this.snippets);
		}
	}

	private final SortedMap<Integer, SnippetStep> snippets = new TreeMap<Integer, SnippetStep>();

	public static final Snippets empty = new Snippets();

	public Snippets() {

	}

	@Override
	public String toString() {
		return this.snippets.toString();
	}

	public int getStepSize() {
		if (this.snippets.isEmpty())
			return 0;
		return this.snippets.lastKey().intValue();
	}

	public void merge(final Snippets other) {
		for (final SnippetStep s : other) {
			if (!this.snippets.containsKey(s.step))
				this.snippets.put(s.step, s);
			else
				this.snippets.get(s.step).merge(s);
		}
	}

	public void add(final int istep, final Snippet snippet) {
		final Integer step = Integer.valueOf(istep);

		if (!this.snippets.containsKey(step)) {
			this.snippets.put(step, new SnippetStep(step));
		}
		this.snippets.get(step).add(snippet);
	}

	public void add(final int istep, final Resource r) {
		final Integer step = Integer.valueOf(istep);

		if (!this.snippets.containsKey(step)) {
			this.snippets.put(step, new SnippetStep(step));
		}
		this.snippets.get(step).add(r);
	}

	@Override
	public Iterator<SnippetStep> iterator() {
		return this.snippets.values().iterator();
	}
}
