/*$Id$*/
package at.jku.snippetmaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import at.jku.snippetmaker.Snippets.SnippletStep;

public final class Snippets implements Iterable<SnippletStep> {
	private final SortedMap<Integer, List<Snippet>> snipplets = new TreeMap<Integer, List<Snippet>>();

	public static final Snippets empty = new Snippets();

	public Snippets() {

	}

	@Override
	public String toString() {
		return this.snipplets.toString();
	}

	public int getStepSize() {
		return this.snipplets.lastKey().intValue();
	}

	public void merge(final Snippets other) {
		for (final SnippletStep s : other) {
			if (!this.snipplets.containsKey(s.step))
				this.snipplets.put(s.step, s.snipplets);
			else
				this.snipplets.get(s.step).addAll(s.snipplets);
		}
	}

	public void add(final int istep, final Snippet snipplet) {
		final Integer step = Integer.valueOf(istep);

		if(!this.snipplets.containsKey(step)) {
			this.snipplets.put(step, new ArrayList<Snippet>());
		}
		this.snipplets.get(step).add(snipplet);
	}

	public static class SnippletStep implements Iterable<Snippet> {
		private final int step;
		private final List<Snippet> snipplets;

		SnippletStep(final int step, final List<Snippet> snipplets) {
			this.step = step;
			this.snipplets = snipplets;
		}

		public int getStep() {
			return this.step;
		}

		@Override
		public Iterator<Snippet> iterator() {
			return this.snipplets.iterator();
		}
	}

	@Override
	public Iterator<SnippletStep> iterator() {
		final Iterator<Entry<Integer, List<Snippet>>> iterator = this.snipplets.entrySet().iterator();
		return new Iterator<Snippets.SnippletStep>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public SnippletStep next() {
				final Entry<Integer, List<Snippet>> next = iterator.next();
				Collections.sort(next.getValue());
				return new SnippletStep(next.getKey().intValue(), next.getValue());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
