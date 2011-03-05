/*$Id$*/
package at.jku.snippetmaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import at.jku.snippetmaker.Snippets.SnippetStep;

public final class Snippets implements Iterable<SnippetStep> {
	private final SortedMap<Integer, List<Snippet>> snippets = new TreeMap<Integer, List<Snippet>>();

	public static final Snippets empty = new Snippets();

	public Snippets() {

	}

	@Override
	public String toString() {
		return this.snippets.toString();
	}

	public int getStepSize() {
		return this.snippets.lastKey().intValue();
	}

	public void merge(final Snippets other) {
		for (final SnippetStep s : other) {
			if (!this.snippets.containsKey(s.step))
				this.snippets.put(s.step, s.snippets);
			else
				this.snippets.get(s.step).addAll(s.snippets);
		}
	}

	public void add(final int istep, final Snippet snippet) {
		final Integer step = Integer.valueOf(istep);

		if (!this.snippets.containsKey(step)) {
			this.snippets.put(step, new ArrayList<Snippet>());
		}
		this.snippets.get(step).add(snippet);
	}

	public static class SnippetStep implements Iterable<Snippet> {
		private final int step;
		private final List<Snippet> snippets;

		SnippetStep(final int step, final List<Snippet> snippets) {
			this.step = step;
			this.snippets = snippets;
		}

		public int getStep() {
			return this.step;
		}

		@Override
		public Iterator<Snippet> iterator() {
			return this.snippets.iterator();
		}
	}

	@Override
	public Iterator<SnippetStep> iterator() {
		final Iterator<Entry<Integer, List<Snippet>>> iterator = this.snippets.entrySet().iterator();
		return new Iterator<Snippets.SnippetStep>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public SnippetStep next() {
				final Entry<Integer, List<Snippet>> next = iterator.next();
				Collections.sort(next.getValue());
				return new SnippetStep(next.getKey().intValue(), next.getValue());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
