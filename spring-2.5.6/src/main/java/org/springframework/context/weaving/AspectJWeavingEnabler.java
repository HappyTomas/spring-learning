/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.weaving;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * Post-processor that registers AspectJ's
 * {@link org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter}
 * with the Spring application context's default
 * {@link org.springframework.instrument.classloading.LoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.5
 */
public class AspectJWeavingEnabler
		implements BeanFactoryPostProcessor, BeanClassLoaderAware, LoadTimeWeaverAware, Ordered {

	private ClassLoader beanClassLoader;

	private LoadTimeWeaver loadTimeWeaver;


	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}


	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		LoadTimeWeaver weaverToUse = this.loadTimeWeaver;
		if (weaverToUse == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			weaverToUse = new InstrumentationLoadTimeWeaver(this.beanClassLoader);
		}
		weaverToUse.addTransformer(new AspectJClassBypassingClassFileTransformer(
					new ClassPreProcessorAgentAdapter()));
	}


	/*
	 * Decorator to suppress processing AspectJ classes, hence avoiding potential LinkageErrors.
	 * OC4J and Tomcat (in Glassfish) definitely need such bypassing of AspectJ classes.
	 */
	private static class AspectJClassBypassingClassFileTransformer implements ClassFileTransformer {

		private final ClassFileTransformer delegate;
		
		public AspectJClassBypassingClassFileTransformer(ClassFileTransformer delegate) {
			this.delegate = delegate;
		}
		
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
				ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

			if (className.startsWith("org.aspectj") || className.startsWith("org/aspectj")) {
				return classfileBuffer;
			}
			return this.delegate.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		}
	}

}
