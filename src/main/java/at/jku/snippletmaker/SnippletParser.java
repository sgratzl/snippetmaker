/*$Id$*/
package at.jku.snippletmaker;

import java.io.IOException;

public interface SnippletParser {
	Snipplets parse() throws IOException;
}
