/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage;

import java.util.List;

import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;

/**
 * A TestFitnessFactory produces a List of goals (of type TestFitnessFunction).
 * Each goal can be given a TestSuite and the goals determines if it is covered
 * by the TestSuite. The difference to TestSuiteFitness functions is, that a
 * goal is either covered or not. (i.e. the search landscape generated by goals
 * is a plateau) GetFitness() allows the TestSuiteMinimizer to work with less
 * assumptions about the used TestFitnessFactory. (I.e. it allows the creation
 * of an adapter, which converts a TestSuiteFitnessFunction into a
 * TestFitnessFactory)
 * 
 * @author Gordon Fraser
 */
public interface TestFitnessFactory<T extends TestFitnessFunction> {

	/**
	 * Generate a list of goals to cover
	 * 
	 * @return a {@link java.util.List} object.
	 */
    List<T> getCoverageGoals();

	/**
	 * Gets the fitness for suite if the goals from this TestFitnessFactory are
	 * applied. Note that some parts of TestSuiteMinimizer assume, that a
	 * smaller Fitness (e.g. 0.1) is better than a larger fitness (e.g. 2.0)
	 * 
	 * @param suite
	 *            a {@link org.evosuite.testsuite.TestSuiteChromosome} object.
	 * @return a double.
	 */
    double getFitness(TestSuiteChromosome suite);
}