/*
 * Copyright (C) 2010 Saarland University
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unisb.cs.st.evosuite.assertion;

import java.util.HashSet;
import java.util.Set;

import de.unisb.cs.st.evosuite.testcase.Scope;
import de.unisb.cs.st.evosuite.testcase.TestCase;
import de.unisb.cs.st.evosuite.testcase.VariableReference;

/**
 * Assertion on comparison value of two objects
 * 
 * @author Gordon Fraser
 * 
 */
public class CompareAssertion extends Assertion {

	public VariableReference dest;

	/**
	 * Create a copy of the compare assertion
	 */
	@Override
	public Assertion clone(TestCase newTestCase) {
		CompareAssertion s = new CompareAssertion();
		s.source = newTestCase.getStatement(source.statement).getReturnValue();
		s.dest = newTestCase.getStatement(dest.statement).getReturnValue();
		s.value = value;
		return s;
	}

	/**
	 * This method returns the Java Code
	 */
	@Override
	public String getCode() {
		if (source.getType().equals(Integer.class)) {
			if ((Integer) value == 0)
				return "assertTrue(" + source.getName() + " == "
				        + dest.getName() + ");";
			else if ((Integer) value < 0)
				return "assertTrue(" + source.getName() + " < "
				        + dest.getName() + ");";
			else
				return "assertTrue(" + source.getName() + " > "
				        + dest.getName() + ");";

		} else {
			return "assertEquals(" + source.getName() + ".compareTo("
			        + dest.getName() + "), " + value + ");";
		}
	}

	/**
	 * Determine if assertion holds in current scope
	 * 
	 * @param scope
	 *            The scope of the test case execution
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean evaluate(Scope scope) {
		Comparable<Object> comparable = (Comparable<Object>) scope.get(source);
		if (comparable == null)
			if ((Integer) value == 0)
				return scope.get(dest) == null;
			else
				return true; // TODO - true or false?
		else {
			try {
				return comparable.compareTo(scope.get(dest)) == (Integer) value;
			} catch (Exception e) {
				return true; // TODO - true or false?
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dest == null) ? 0 : dest.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		CompareAssertion other = (CompareAssertion) obj;
		if (dest == null) {
			if (other.dest != null)
				return false;
		} else if (!dest.equals(other.dest))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if ((Integer) value > 0) {
			if ((Integer) other.value <= 0)
				return false;
		} else if ((Integer) value < 0) {
			if ((Integer) other.value >= 0)
				return false;
		} else if ((Integer) value == 0) {
			if ((Integer) other.value != 0)
				return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.unisb.cs.st.evosuite.assertion.Assertion#getReferencedVariables()
	 */
	@Override
	public Set<VariableReference> getReferencedVariables() {
		Set<VariableReference> vars = new HashSet<VariableReference>();
		vars.add(source);
		vars.add(dest);
		return vars;
	}

}
