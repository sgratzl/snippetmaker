/*$Id$*/
package at.jku.snippletmaker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import at.jku.snippletmaker.Snipplets.SnippletStep;

public final class Snipplets implements Iterable<SnippletStep> {
	private final SortedMap<Integer, List<Snipplet>> snipplets = new TreeMap<Integer, List<Snipplet>>();

	public static final Snipplets empty = new Snipplets();

	public Snipplets() {

	}

	@Override
	public String toString() {
		return this.snipplets.toString();
	}

	public int getStepSize() {
		return this.snipplets.lastKey().intValue();
	}

	public void merge(final Snipplets other) {
		for (final SnippletStep s : other) {
			if (!this.snipplets.containsKey(s.step))
				this.snipplets.put(s.step, s.snipplets);
			else
				this.snipplets.get(s.step).addAll(s.snipplets);
		}
	}

	public void add(final int istep, final Snipplet snipplet) {
		final Integer step = Integer.valueOf(istep);

		if(!this.snipplets.containsKey(step)) {
			this.snipplets.put(step, new ArrayList<Snipplet>());
		}
		this.snipplets.get(step).add(snipplet);
	}

	public static class SnippletStep implements Iterable<Snipplet> {
		private final int step;
		private final List<Snipplet> snipplets;

		SnippletStep(final int step, final List<Snipplet> snipplets) {
			this.step = step;
			this.snipplets = snipplets;
		}

		public int getStep() {
			return this.step;
		}

		@Override
		public Iterator<Snipplet> iterator() {
			return this.snipplets.iterator();
		}
	}

	@Override
	public Iterator<SnippletStep> iterator() {
		final Iterator<Entry<Integer, List<Snipplet>>> iterator = this.snipplets.entrySet().iterator();
		return new Iterator<Snipplets.SnippletStep>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public SnippletStep next() {
				final Entry<Integer, List<Snipplet>> next = iterator.next();
				return new SnippletStep(next.getKey().intValue(), next.getValue());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
