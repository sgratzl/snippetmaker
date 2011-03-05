/*$Id$*/
package at.jku.snippetmaker;

import java.io.Reader;

import org.apache.tools.ant.filters.ChainableReader;

import at.jku.snippetmaker.type.CppSnippet;
import at.jku.snippetmaker.type.XmlSnippet;

public final class StepTransformer implements ChainableReader {
	private int step = 0;
	private boolean createMarker = true;
	private Type type = Type.cpp;

	public StepTransformer() {
	}

	public void setStep(final int step) {
		this.step = step;
	}

	public void setType(final Type type) {
		this.type = type;
	}

	public void setCreateMarker(final boolean createMarker) {
		this.createMarker = createMarker;
	}

	public void setFinal(final boolean finalStep) {
		if (finalStep)
			this.step = Integer.MAX_VALUE;
	}

	public final Reader chain(final Reader reader) {
		switch (this.type) {
		case cpp:
			return CppSnippet.createFilterReader(reader, this.step, this.createMarker);
		case xml:
			return XmlSnippet.createFilterReader(reader, this.step, this.createMarker);
		default:
			return reader;
		}
	}

	public static enum Type {
		cpp, xml
	}

}
