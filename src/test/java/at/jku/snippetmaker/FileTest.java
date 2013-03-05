/*******************************************************************************
 * Caleydo - visualization for molecular biology - http://caleydo.org
 *
 * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander
 * Lex, Christian Partl, Johannes Kepler University Linz </p>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package at.jku.snippetmaker;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Test;

import at.jku.snippetmaker.File.Type;

/**
 * @author Samuel Gratzl
 *
 */
public class FileTest {

	@Test
	public void test() {
		Project p = new Project();
		File f = new File();
		f.add(new Path(p, "src/test/resources/test1/main.cpp"));
		f.setProject(p);
		f.setType(Type.txt);
		f.setFile(new java.io.File("target/test1.output.txt"));
		f.execute();
	}

}
