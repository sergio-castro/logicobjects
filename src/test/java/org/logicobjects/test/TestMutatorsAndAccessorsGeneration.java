package org.logicobjects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.logicobjects.annotation.LObject;
import org.logicobjects.core.LogicObjectFactory;
import org.reflectiveutils.ReflectionUtil;
import org.reflectiveutils.wrappertype.AbstractTypeWrapper;

@RunWith(value = Parameterized.class)
public class TestMutatorsAndAccessorsGeneration {

	@LObject(args = {"p"})
	public static class PrimitiveProperty {
		int p;
	}
	
	//testing with a class that does not need to be imported
	@LObject(args = {"p"})
	public static class StringProperty {
		String p;
	}
	
	//testing with a class that must be imported
	@LObject(args = {"p"})
	public static class ListProperty {
		List p;
	}
	
	@LObject(args = {"p"})
	public static class GenericsProperty {
		List<String> p;
	}
	
	@LObject(args = {"p"})
	public static abstract class GenericsPropertyFromGetter {
		public abstract List<String> getP();
	}
	
	@LObject(args = {"p"})
	public static abstract class GenericsPropertyFromSetter {
		public abstract void setP(List<String> p);
	}
	
	@LObject(args = {"p"})
	public static abstract class GenericsPropertyFromGetterAndSetter {
		public abstract List<String> getP();
		public abstract void setP(List<String> p);
	}
	
	@LObject(args = {"p"})
	public static class ArrayProperty {
		String[] p;
	}
	
	@LObject(args = {"p"})
	public static class ArrayGenericsProperty {
		List<String>[] p;
	}
	
	@LObject(args = {"p"})
	public static class ParameterizedTypeProperty<X> {
		X p;
	}
	
	private static final String TESTED_FIELD = "p";
	
	private Class testingClass;
	
	public TestMutatorsAndAccessorsGeneration(Class testingClass) {
		this.testingClass = testingClass;
	}
	
	@Parameters
	public static List<Object[]> testParameters() {
		Object[][] testParameters = new Object[][] {
				{PrimitiveProperty.class},
				{StringProperty.class},
				{ListProperty.class},
				{GenericsProperty.class},
				{GenericsPropertyFromGetter.class},
				{GenericsPropertyFromSetter.class},
				{GenericsPropertyFromGetterAndSetter.class},
				{ArrayProperty.class},
				{ArrayGenericsProperty.class},
				{ParameterizedTypeProperty.class}
		};
		return Arrays.asList(testParameters);
	}
	
	@Test
	public void testPrimitiveProperty() {
		
		Object lo = LogicObjectFactory.getDefault().create(testingClass);
		assertEquals(lo.getClass().getSuperclass(), testingClass);
		Field field;
		try {
			field = testingClass.getDeclaredField(TESTED_FIELD);
			Type fieldType = field.getGenericType();
			AbstractTypeWrapper fieldTypeWrapper = AbstractTypeWrapper.wrap(fieldType);
			
			Method getterMethod = lo.getClass().getMethod(ReflectionUtil.getterName(TESTED_FIELD, fieldTypeWrapper.asClass()));
			assertNotNull(getterMethod);
			Type getterType = getterMethod.getGenericReturnType();
			AbstractTypeWrapper getterTypeWrapper = AbstractTypeWrapper.wrap(getterType);
			assertEquals(fieldTypeWrapper, getterTypeWrapper);
			
			Method setterMethod = lo.getClass().getMethod(ReflectionUtil.setterName(TESTED_FIELD), fieldTypeWrapper.asClass());
			assertNotNull(setterMethod);
			Type setterParameterType = setterMethod.getGenericParameterTypes()[0];
			AbstractTypeWrapper setterParameterTypeWrapper = AbstractTypeWrapper.wrap(setterParameterType);
			assertEquals(fieldTypeWrapper, setterParameterTypeWrapper);
			
		} catch (NoSuchFieldException | SecurityException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
}
