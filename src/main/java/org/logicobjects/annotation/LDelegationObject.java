package org.logicobjects.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation specify which is the logtalk object receiving the message of declared logic methods
 * However, when converting this class into a term, this annotation will be ignored
 * A class should not be annotated at the same time with LObject and LDelegationObject
 * @author sergioc78
 *
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
//TODO Maybe choose a better name ? LMethodDispatcher, LMethodInvoker, LProxyObject, LPseudoObject ...
public @interface LDelegationObject {
	String name() default "";  //the name of the logic object
	String[] params() default {}; //the parameters of the logic object

	String[] imports() default {};
	String[] modules() default {};
	boolean automaticImport() default true;
}