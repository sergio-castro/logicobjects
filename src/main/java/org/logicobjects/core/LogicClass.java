package org.logicobjects.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import jpl.Term;

import org.logicobjects.adapter.LogicResourcePathAdapter;
import org.logicobjects.adapter.adaptingcontext.LObjectGenericDescription;
import org.logicobjects.annotation.LObject;
import org.logicobjects.annotation.LDelegationObject;
import org.logicobjects.annotation.LTermAdapter;
import org.logicobjects.util.LogicUtil;
import org.reflectiveutils.visitor.FindFirstTypeVisitor;
import org.reflectiveutils.visitor.TypeVisitor.InterfaceMode;

/**
 * A class providing a description for instantiating logic objects
 * Part of the data is in the logic side 
 * @author scastro
 *
 */
public class LogicClass {
	
	private Class clazz;
	private LogicMetaObject metaObject;
	
	
	public LogicClass(Class clazz) {
		this.clazz = clazz;
	}
	
	public Class getWrappedClass() {
		return clazz;
	}
	
	/**
	 * The meta object is needed to answer meta questions about the logic object represented by this class
	 * For example, which are the parameter names
	 * @return
	 */
	public LogicMetaObject getMetaObject() {
		if(metaObject == null)
			metaObject = LogicObjectFactory.getDefault().createLogicMetaObject(clazz);
		return metaObject;
	}
	
	private LObject getLObjectAnnotation() {
		return (LObject) clazz.getAnnotation(LObject.class);
	}
	
	public String getLObjectName() {
		LObject lObjectAnnotation = getLObjectAnnotation();
		String name = lObjectAnnotation.name();
		if(!name.isEmpty())
			return name;
		else
			return LogicUtil.javaClassNameToProlog(clazz.getSimpleName());
	}
	
	public String[] getLObjectArgs() {
		LObject lObjectAnnotation = getLObjectAnnotation();
		return lObjectAnnotation.args();
	}
	
	public String[] getImports() {
		LObject lObjectAnnotation = getLObjectAnnotation();
		return lObjectAnnotation.imports();
	}
	
	public String[] getModules() {
		LObject lObjectAnnotation = getLObjectAnnotation();
		return lObjectAnnotation.modules();
	}
	
	public boolean automaticImport() {
		LObject lObjectAnnotation = getLObjectAnnotation();
		return lObjectAnnotation.automaticImport();
	}
	
	

	/*
	 * Return a boolean indicating if all the modules and objects were loaded correctly
	 */
	public static boolean loadDependencies(Class clazz) {
		clazz = findDependencyInfoClass(clazz);
		if(clazz == null)
			return false;
		
		LObjectGenericDescription lMethodInvokerDescription = LObjectGenericDescription.create(clazz);  //look at the annotations of the class and return an object having information about how to load dependencies

		if(!lMethodInvokerDescription.automaticImport())
			return false;
		
		boolean result = true;
		
		String[] annotationModules = lMethodInvokerDescription.modules();
		String[] bundleModules = getBundleModules(clazz);
		if(bundleModules == null) {
			bundleModules = new String[] {};
		}
		
		Set<String> allModules = new LinkedHashSet<String>(); //Set is used to avoid duplicates. Using LinkedHashSet instead of HashSet (the faster), since the former will preserve the insertion order
		allModules.addAll(Arrays.asList(bundleModules));
		allModules.addAll(Arrays.asList(annotationModules));
		
		List<Term> moduleTerms = new ArrayList<Term>();
		new LogicResourcePathAdapter().adapt(allModules, moduleTerms);
		
		result = LogicEngine.getDefault().ensureLoaded(moduleTerms); //loading prolog modules
		
		String[] annotationImports = lMethodInvokerDescription.imports();
		String[] bundleImports = getBundleImports(clazz);
		if(bundleImports == null) {
			bundleImports = new String[] {};
		}
		String[] defaultImports = getDefaultImports(clazz);
		
		Set<String> allImports = new LinkedHashSet<String>(); //Set is used to avoid duplicates.
		allImports.addAll(Arrays.asList(bundleImports));
		allImports.addAll(Arrays.asList(annotationImports));
		allImports.addAll(Arrays.asList(defaultImports));
		

		List<Term> importTerms = new ArrayList<Term>();
		new LogicResourcePathAdapter().adapt(allImports, importTerms);
		
		return LogicEngine.getDefault().logtalkLoad(importTerms) && result; //loading Logtalk objects
	}
	
	private static final String IMPORTS = "imports"; //"objects" in logicobjects files will be loaded with logtalk_load
	private static final String MODULES = "modules"; //"modules" in logicobjects files will be loaded with ensure_loaded
	public static final String BUNDLE_NAME = "logicobjects";
	
	public static ResourceBundle getBundle(Class clazz) {
		try {
			return ResourceBundle.getBundle(clazz.getPackage().getName() + "."+BUNDLE_NAME);
		} catch(MissingResourceException e) {
			return null;
		}
	}
	
	public static String[] getBundleImports(Class clazz) {
		return getBundleProperty(clazz, IMPORTS);
	}
	
	public static String[] getBundleModules(Class clazz) {
		return getBundleProperty(clazz, MODULES);
	}
	
	public static String[] getBundleProperty(Class clazz, String propertyName) {
		ResourceBundle bundle = getBundle(clazz);
		if(bundle == null)
			return null;
		if(bundle.containsKey(propertyName)) {
			String stringProperties = bundle.getString(propertyName);
			String[] properties = stringProperties.split(",");
			return properties;
		} else
			return new String[] {};
	}
	
	public static String[] getAnnotationImports(Class clazz) {
		return ((LObject)clazz.getAnnotation(LObject.class)).imports();
	}
	
	public static String[] getAnnotationModules(Class clazz) {
		return ((LObject)clazz.getAnnotation(LObject.class)).modules();
	}
	
	private static boolean addIfLgtFileExists(String fileName, Class clazz, List<String> destiny) {
		if(fileName == null || fileName.equals(""))
			return false;
		String packageName = clazz.getPackage().getName();
		/**
		 * the getResource method will append before the path of the class
		 * note that for some reason this method is not case sensitive: fileName will match any file with the same name without taking into consideration its case
		 */
		if(clazz.getResource(fileName+".lgt") != null) { 
			destiny.add(packageName+"."+fileName);
			return true;
		}
		return false;
	}
	
	public static String[] getDefaultImports(Class clazz) {
		List<String> defaultImports = new ArrayList<String>();
		/**
		 * It is not a good idea to look for a .lgt class with exactly the same name than the class
		 * 1) If it starts with a capital letter then it does not respect the Prolog conventions
		 * 2) If the only variation in the java name and the prolog name (obtained with 'javaClassNameToProlog') the resource will be loaded twice
		 */
		addIfLgtFileExists(clazz.getSimpleName(), clazz, defaultImports);
		addIfLgtFileExists(LogicUtil.javaClassNameToProlog(clazz.getSimpleName()), clazz, defaultImports);
		
		LObject logicObject = getLogicObjectAnnotationInHierarchy(clazz);
		if(logicObject != null)
			addIfLgtFileExists(logicObject.name(), clazz, defaultImports);
		return defaultImports.toArray(new String[] {});
	}
	
	
	
	

	
	public static LObject getLogicObjectAnnotationInHierarchy(Class clazz) {
		Class annotatedClass = findLogicClass(clazz);
		if(annotatedClass != null)
			return (LObject)annotatedClass.getAnnotation(LObject.class);
		else
			return null;
	}
	
	
	
	/*
	 * The guiding class is the first class in the hierarchy that either implements TermObject, has a LogicObject annotation, or a LogicTerm annotation
	 */
	public static Class findGuidingClass(Class candidateClass) {
		if(candidateClass.equals(Object.class))
			return null;
		if(isGuidingClass(candidateClass))
			return candidateClass;
		else
			return findGuidingClass(candidateClass.getSuperclass());
	}
	
	public static boolean isGuidingClass(Class candidateClass) {
		return isTermObjectClass(candidateClass) || candidateClass.getAnnotation(LObject.class) != null || candidateClass.getAnnotation(LTermAdapter.class) != null;
	}
	
	public static boolean isTermObjectClass(Class aClass) {
		for(Class anInterface : aClass.getInterfaces()) {
			if(ITermObject.class.isAssignableFrom(anInterface))
				return true;
		}
		return false;
	}


	/*
	 * This method returns the first class or interface is the hierarchy annotated with the LObject annotation 
	 */
	public static Class findLogicClass(Class candidateClass) {
		/*
		if(candidateClass.equals(Object.class)) //end of the hierarchy
			return null;
		if(candidateClass.getAnnotation(LObject.class) != null)
			return candidateClass;
		else {
			Class logicClass = null;
			for(Class interfaze : candidateClass.getInterfaces()) { //answers only the interfaces declared by the class, does not include the ones in its superclass
				logicClass = findLogicClass(interfaze);
				if(logicClass != null)
					return logicClass;
			}
			if(!candidateClass.isInterface()) {
				logicClass = findLogicClass(candidateClass.getSuperclass());
			} 
			return logicClass;
		}
		*/
		
		FindFirstTypeVisitor finderVisitor = new FindFirstTypeVisitor(InterfaceMode.EXCLUDE_INTERFACES) {
			@Override
			public boolean match(Class clazz) {
				return clazz.getAnnotation(LObject.class) != null;
			}
		};
		finderVisitor.visit(candidateClass);
		return finderVisitor.getFoundType();
	}
	
	//TODO
	/**
	 * Answers the first class/interface in the class hierarchy specifying the logic object method invoker
	 * @param candidateClass
	 * @return
	 */
	public static Class findMethodInvokerClass(Class candidateClass) {
		FindFirstTypeVisitor finderVisitor = new FindFirstTypeVisitor(InterfaceMode.EXCLUDE_INTERFACES) {
			
			@Override
			public boolean match(Class clazz) {
				return clazz.getAnnotation(LDelegationObject.class) != null || isGuidingClass(clazz);
			}
		};
		finderVisitor.visit(candidateClass);
		return finderVisitor.getFoundType();
	}
	
	/**
	 * Answers the delegation class in the hierarchy
	 * If before arriving to the class, it is found a "guiding" class (a class with LObject annotation or other information for converting an object to a LObject) the method will return null
	 * @param candidateClass
	 * @return
	 */
	public static Class findDelegationObjectClass(Class candidateClass) {
		FindFirstTypeVisitor finderVisitor = new FindFirstTypeVisitor(InterfaceMode.EXCLUDE_INTERFACES) {
			@Override
			public boolean doVisit(Class clazz) {
				boolean shouldContinue = super.doVisit(clazz);
				if(shouldContinue)
					shouldContinue = !isGuidingClass(clazz);
				return shouldContinue;
			}
			
			@Override
			public boolean match(Class clazz) {
				return clazz.getAnnotation(LDelegationObject.class) != null;
			}
		};
		finderVisitor.visit(candidateClass);
		return finderVisitor.getFoundType();
	}
	
	
	/**
	 * Answers the first class in the hierarchy providing information to import the dependencies of the logic object
	 * @param candidateClass
	 * @return
	 */
	public static Class findDependencyInfoClass(Class candidateClass) {
		FindFirstTypeVisitor finderVisitor = new FindFirstTypeVisitor(InterfaceMode.EXCLUDE_INTERFACES) {
			@Override
			public boolean doVisit(Class clazz) {
				boolean shouldContinue = super.doVisit(clazz);
				if(shouldContinue)
					shouldContinue = !isTermObjectClass(clazz);
				return shouldContinue;
			}
			
			@Override
			public boolean match(Class clazz) {
				return clazz.getAnnotation(LDelegationObject.class) != null || clazz.getAnnotation(LObject.class) != null;
			}
		};
		finderVisitor.visit(candidateClass);
		return finderVisitor.getFoundType();
	}
	
}

