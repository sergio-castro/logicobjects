package org.logicobjects.context;

import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jpl.Term;
import jpl.Variable;

import org.logicobjects.adapter.Adapter;
import org.logicobjects.adapter.methodresult.solutioncomposition.WrapperAdapter;
import org.logicobjects.annotation.IgnoreLAdapter;
import org.logicobjects.core.LogicObjectClass;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public abstract class AbstractLContext {

	public abstract void addSearchFilter(String packageName);
	public abstract void addSearchUrlFromClass(Class clazz);
	public abstract void addSearchUrls(URL... urls);
	
	public abstract Set<Class<?>> getLogicClasses();
	public abstract Set<Class<? extends WrapperAdapter>> getWrapperAdapters();
	
	
	protected void filterLogicClasses(Set<Class<?>> unfilteredLogicClasses, Set<Class<?>> filteredClasses) {
		for(Class clazz : unfilteredLogicClasses) {
			if(!clazz.isInterface()) {
				filteredClasses.add(clazz);
			} /*else {
				for(Object _implementor : system_reflections.getSubTypesOf(clazz)) {//TODO discover how i can put a Class in the for
					Class implementor = (Class)_implementor; //this kind of things make me hate java ...
					if(!implementor.isInterface() && ReflectionUtil.includesInterfaceInHierarchy(implementor, clazz))
						filteredClasses.add(implementor);
				}
			} */
		}
	}
	
	protected void filterAdapters(Set<Class<? extends Adapter>> foundAdapters, Set<Class<? extends WrapperAdapter>> wrapperAdapters) {
		for(Class<? extends Adapter> adapterClass : foundAdapters) {
			if(adapterClass.getAnnotation(IgnoreLAdapter.class) == null) {
				if(WrapperAdapter.class.isAssignableFrom(adapterClass) && !Modifier.isAbstract(adapterClass.getModifiers()))
					wrapperAdapters.add((Class<? extends WrapperAdapter>) adapterClass);
			}
		}
	}
	
	
	

	/**
	 * This method is a workaround to the problem that the current version of reflections (at the moment of testing: 0.9.5 ) does not recognize JBoss URLs.
	 * TODO check if next versions of Reflections still have this problem, otherwise this method can be removed.
	 * @param urls
	 * @return
	 */
	protected Set<URL> filterURLs(Set<URL> urls) {
        Set<URL> results = new HashSet<URL>(urls.size());
        for (URL url : urls) {
            String cleanURL = url.toString();
            // Fix JBoss URLs
            if (url.getProtocol().startsWith("vfszip")) {
                cleanURL = cleanURL.replaceFirst("vfszip:", "file:");
            } else if (url.getProtocol().startsWith("vfsfile")) {
                cleanURL = cleanURL.replaceFirst("vfsfile:", "file:");
            } else if(url.getProtocol().startsWith("vfs")) {//added by me
                  cleanURL = cleanURL.replaceFirst("vfs:", "file:");
            } 
            
            
            cleanURL = cleanURL.replaceFirst("\\.jar/", ".jar!/");
            try {
                results.add(new URL(cleanURL));
            } catch (MalformedURLException ex) {
                // Shouldn't happen, but we can't do more to fix this URL.
            }
        }
        return results;
    }
	
	
	public Class findLogicClass(String logicName, int args) {
		Set<Class<?>> set = getLogicClasses();
		for(Class clazz : set) {
			LogicObjectClass logicClass = new LogicObjectClass(clazz);
			if(logicClass.getLObjectName().equals(logicName) && logicClass.getLObjectArgs().length == args)
				return clazz;
		}
		return null;
	}
	
	public Class findLogicClass(Term term) {
		if( term instanceof Variable || term instanceof jpl.Integer || term instanceof jpl.Float || term.name().equals(".") )
			return null;
		return findLogicClass(term.name(), term.args().length);
	}
	

}
