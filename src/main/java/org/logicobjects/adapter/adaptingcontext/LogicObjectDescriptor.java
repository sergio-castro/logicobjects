package org.logicobjects.adapter.adaptingcontext;

import java.lang.annotation.Annotation;

import org.logicobjects.annotation.LDelegationObject;
import org.logicobjects.annotation.LObject;


/**
 * This class describes a logic object: Currently these are objects annotated with @LObject and @LDelegationObject
 * NOTE: The class is a workaround to the problem than in Java annotations cannot extend other annotations or implement an interface,
 *   since @LObject and @LDelegationObject are quite similar, this class and its subclasses define methods to access common functionality from these two annotations
 *   (without having to be aware which of the two ones are the used ones behind the curtains).
 * @author scastro
 *
 */
public abstract class LogicObjectDescriptor {

	public abstract String name(); 
	public abstract String[] args();
	public abstract String argsArray();
	public abstract String[] imports();
	public abstract String[] modules();
	public abstract boolean automaticImport();

	
	public static LogicObjectDescriptor create(Class clazz) {
		LObject aLObject = (LObject) clazz.getAnnotation(LObject.class);
		if(aLObject != null)
			return create(aLObject);
		
		LDelegationObject aLDelegationObject = (LDelegationObject) clazz.getAnnotation(LDelegationObject.class);
		if(aLDelegationObject != null)
			return create(aLDelegationObject);
		
		throw new RuntimeException("Impossible to create Method invoker description from class: " + clazz.getSimpleName());
	}
	
	public static LogicObjectDescriptor create(LObject aLObject) {
		return new LObjectAnnotationDescriptor(aLObject);
	}
	
	public static LogicObjectDescriptor create(LDelegationObject aLDelegationObject) {
		return new LDelegationObjectAnnotationDescriptor(aLDelegationObject);
	}
	
	
	static class LObjectAnnotationDescriptor extends LogicObjectDescriptor {
		
		LObject aLObject;

		public LObjectAnnotationDescriptor(LObject aLObject) {
			assert(aLObject != null);
			this.aLObject = aLObject;
		}
		
		public String name() {
			return aLObject.name();
		}

		public String[] args() {
			return aLObject.args();
		}

		@Override
		public String argsArray() {
			return aLObject.argsArray();
		}
		
		public String[] imports() {
			return aLObject.imports();
		}

		public String[] modules() {
			return aLObject.modules();
		}

		public boolean automaticImport() {
			return aLObject.automaticImport();
		}


	}
	
	
	
	
	static class LDelegationObjectAnnotationDescriptor extends LogicObjectDescriptor {
		LDelegationObject aLDelegationObject;

		public LDelegationObjectAnnotationDescriptor(LDelegationObject aLDelegationObject) {
			assert(aLDelegationObject != null);
			this.aLDelegationObject = aLDelegationObject;
		}
		
		public String name() {
			return aLDelegationObject.name();
		}

		public String[] args() {
			return aLDelegationObject.args();
		}

		@Override
		public String argsArray() {
			return aLDelegationObject.argsArray();
		}
		
		public String[] imports() {
			return aLDelegationObject.imports();
		}

		public String[] modules() {
			return aLDelegationObject.modules();
		}

		public boolean automaticImport() {
			return aLDelegationObject.automaticImport();
		}


	}
	
	
}