/*$Id$*/
package at.jku.snippetmaker;

import java.io.IOException;

public interface SnippetParser {
	Snippets parse() throws IOException;
}
