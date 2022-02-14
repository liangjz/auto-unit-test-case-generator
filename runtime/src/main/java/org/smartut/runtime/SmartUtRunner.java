/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and SmartUt
 * contributors
 *
 * This file is part of SmartUt.
 *
 * SmartUt is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * SmartUt is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with SmartUt. If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartut.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.smartut.annotations.SmartUtTest;
import org.junit.Test;
import org.smartut.runtime.instrumentation.SmartUtClassLoader;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special JUnit Runner needed for the test cases generated by SmartUt
 *
 * @author arcuri
 *
 */
public class SmartUtRunner extends BlockJUnit4ClassRunner {

	/*
	 * We need this class due to some weird behavior of JVM
	 */

    private static final Logger logger = LoggerFactory.getLogger(SmartUtRunner.class);

    /**
     * Dirty hack, to use with care.
     * In some very special cases, we want to skip agent instrumentation.
     * Still, we would need to use a classloader that will do such instrumentation.
     * This is for example done in -measureCoverage, as we need a more details instrumentation,
     * and, at the same time, we want to avoid a double instrumentation from agent
     */
    public static boolean useAgent = true;

    /**
     * Another dirty hack, to use with care. This is to make useSeparateClassLoader
     * work together with -measureCoverage
     */
    public static boolean useClassLoader = true;

    /**
     * key:loaded case name
     * value:case build smartutclassloader object
     */
    public static final Map<String,SmartUtClassLoader> smartUtClassLoaderMap = new HashMap<>();
    /**
     * key:Thread id
     * value:contextClassloader corresponding to thread
     */
    public static final Map<Long,ClassLoader> resetClassLoaderMap = new HashMap<>();

    public SmartUtRunner(Class<?> klass)
            throws InitializationError {
		/*
		 * extremely important that getClass is called _BEFORE_ super is executed.
		 * The constructor of BlockJUnit4ClassRunner does reflection on klass, eg
		 * to check that it has only one constructor.
		 * For some arcane reasons, such reflection code ends up in native JVM code
		 * that "might" start the loading of some classes whose type is used for
		 * variables and casting inside the methods of "klass", 
		 * although the code of those methods is _NOT_ executed (note: not
		 * talking of static initializers here).
		 */
        super(getClass(klass));
    }

    private static Class<?> getClass(Class<?> klass) throws InitializationError{

        SmartUtRunnerParameters ep = klass.getAnnotation(SmartUtRunnerParameters.class);

        if(ep == null){
            throw new IllegalStateException("SmartUt test class "+klass.getName()+
                    " is not annotated with "+SmartUtRunnerParameters.class.getName());
        }

        RuntimeSettings.resetStaticState = ep.resetStaticState();
        RuntimeSettings.mockJVMNonDeterminism = ep.mockJVMNonDeterminism();
        RuntimeSettings.mockGUI = ep.mockGUI();
        RuntimeSettings.useVFS = ep.useVFS();
        RuntimeSettings.useVNET = ep.useVNET();
        RuntimeSettings.useSeparateClassLoader = ep.separateClassLoader();
        RuntimeSettings.useJEE = ep.useJEE();

        if(RuntimeSettings.useSeparateClassLoader && useClassLoader) {
            return getFromSmartUtClassloader(klass);
        }

        if(useAgent) {
            org.smartut.runtime.agent.InstrumentingAgent.initialize();
        }

        org.smartut.runtime.agent.InstrumentingAgent.activate();

        try {
			/*
			 *  be sure that reflection on "klass" is executed here when
			 *  the agent is active
			 */
            klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            //shouldn't really happen
            logger.error("Failed to initialize test class "+klass.getName());
        }
        org.smartut.runtime.agent.InstrumentingAgent.deactivate();

        return klass;
    }

    private static Class<?> getFromSmartUtClassloader(Class<?> clazz) throws InitializationError {
        try {
	    	/*
	    	 *  properties like REPLACE_CALLS will be set directly in the JUnit files
	    	 */

            // LoggingUtils.loadLogbackForSmartUt();

	    	/*
	    	 * This approach does throw away all the possible instrumentation done on the input clazz,
	    	 * eg code coverage of Emma, Cobertura, Javalanche, etc.
	    	 * Furthermore, if the classloader used to load SmartUtRunner is not the same as CUT,
	    	 * then loading CUTs will fail (this does happen in "mvn test")
	    	 */

            SmartUtClassLoader classLoader = new SmartUtClassLoader();
            classLoader.skipInstrumentation(clazz.getName());
            //Record the corresponding SmartUtClassLoader
            smartUtClassLoaderMap.put(clazz.getName(), classLoader);
            RuntimeSettings.caseName = clazz.getName();
            if (!resetClassLoaderMap.containsKey(Thread.currentThread().getId()))
                resetClassLoaderMap.put(Thread.currentThread().getId(), Thread.currentThread().getContextClassLoader());
//            Thread.currentThread().setContextClassLoader(classLoader);
            return Class.forName(clazz.getName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    /**
     * use smartut classloader
     */
    public static void useSmartUtClassLoader(){
        if(RuntimeSettings.useSeparateClassLoader && useClassLoader) {
           Thread.currentThread().setContextClassLoader(smartUtClassLoaderMap.get(RuntimeSettings.caseName));
        }
    }

    /**
     * reset thread classloader
     */
    public static void resetSmartUtClassLoader(){
        if(RuntimeSettings.useSeparateClassLoader && useClassLoader) {
            Thread.currentThread().setContextClassLoader(resetClassLoaderMap.get(Thread.currentThread().getId()));
        }
    }

    /**
     * Returns the methods that run tests. Default implementation returns all
     * methods annotated with {@code @Test} on this class and superclasses that
     * are not overridden.
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        Set<FrameworkMethod> testMethods = new HashSet<>();
        testMethods.addAll(getTestClass().getAnnotatedMethods(SmartUtTest.class));
        testMethods.addAll(getTestClass().getAnnotatedMethods(Test.class));
        return new ArrayList<>(testMethods);
    }

    /**
     * Adds to {@code errors} for each method annotated with {@code @Test}that
     * is not a public, void instance method with no arguments.
     */
    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        Set<FrameworkMethod> testMethods = new HashSet<>();
        testMethods.addAll(getTestClass().getAnnotatedMethods(SmartUtTest.class));
        testMethods.addAll(getTestClass().getAnnotatedMethods(Test.class));
        for (FrameworkMethod eachTestMethod : testMethods) {
            eachTestMethod.validatePublicVoidNoArg(false, errors);
        }
    }

}
