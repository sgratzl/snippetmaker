/*$Id$*/
package at.jku.snippetmaker;

import java.util.BitSet;

public class StepFilter {
	private RangeFilter includes = null;
	private RangeFilter excludes = null;

	public void setIncludes(final String includes) {
		this.includes = new RangeFilter(includes);
	}

	public void setExcludes(final String excludes) {
		this.excludes = new RangeFilter(excludes);
	}

	public boolean include(final int step) {
		if (this.excludes != null && this.excludes.bit.get(step))
			return false;
		if (this.includes != null)
			return this.includes.bit.get(step);
		return this.excludes != null;
	}

	private static class RangeFilter {
		private final BitSet bit = new BitSet();

		public RangeFilter(final String filter) {
			for (final String s : filter.split(",")) {
				if (s.length() <= 0)
					continue;
				if (s.indexOf('-') >= 0) {
					if (s.charAt(0) == '-')
						this.bit.set(0, Integer.parseInt(s.substring(1)));
					else {
						final String[] r = s.split("-");
						this.bit.set(Integer.parseInt(r[0]), Integer.parseInt(r[1]));
					}
				} else
					this.bit.set(Integer.parseInt(s));
			}
		}
	}
}
