package org.logicobjects.instrumentation;

/**
 * Symbols used to express parameters:
 * 
 * (Term symbols)
 * $NUMBER : a java method parameter as a term (e.g., '$1' means the first parameter)
 * $$ : all the java method parameters as terms. The parameters are separated by a ','
 * @this : the object declaring the method as term
 * @propertyName : a bean property of the object as term
 * 
 * (Java object symbols)
 * ! : a suffix to the previous symbols. If added, the object will not be transformed to a term, but will be passed to Logtalk as a java  object
 * & : same as '!', but with delayed evaluation
 * @author sergioc78
 *
 */
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.Term;

import org.logicobjects.adapter.LogicObjectAdapter;
import org.logicobjects.adapter.ObjectToTermAdapter;
import org.logicobjects.core.AbstractLogicMethod;
import org.logicobjects.core.LogicEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auxiliary class providing operations for parsing logic method parameters
 * @author sergioc78
 *
 */
public abstract class AbstractLogicMethodParser<LM extends AbstractLogicMethod> {
	private static Logger logger = LoggerFactory.getLogger(AbstractLogicMethodParser.class);

	public static final String generatedMethodsPrefix = "$logicobjects_";
	
	public static final String BEGIN_JAVA_EXPRESSION = "/{";
	public static final String END_JAVA_EXPRESSION = "/}";
	
	
	public static final String INSTANCE_PROPERTY_PREFIX = "@";
	//private static final String INSTANCE_PROPERTY_PREFIX_REX = "\\@";
	
	public static final String PARAMETERS_PREFIX = "$";
	//private static final String PARAMETERS_PREFIX_REX = "\\$";
	
	public static final String THIS_SUFFIX = "0";
	
	public static final String THIS_SYMBOL = PARAMETERS_PREFIX + THIS_SUFFIX;
	
	public static final String ALL_PARAMS_SUFFIX = "$";
	//private static final String ALL_PARAMS_SUFFIX_REX = "\\*";
	
	public static final String ALL_PARAMS_SYMBOL = PARAMETERS_PREFIX + ALL_PARAMS_SUFFIX;
	
	public static final String JAVA_NAME_REX = "([a-zA-Z_\\$][\\w\\$]*)";
	
	public static final String PARAMETER_JAVA_REX = Pattern.quote(BEGIN_JAVA_EXPRESSION) + "(.*?)" + Pattern.quote(END_JAVA_EXPRESSION);
	
	public static final String PARAMETERS_SEPARATOR = "~~~";
	
	public static final String RETURN_SEPARATOR = "~RET~";

	

	
	private LM logicMethod;
	private String allLogicStrings;
	private List<String> foundSymbols;
	private List<String> expressions;
	private Map<String, String> expressionsReplacementMap;
	private String eachSolutionValue;

	public static AbstractLogicMethodParser create(Method method) {
		if(AbstractLogicMethod.isRawQuery(method))
			return new RawQueryParser(method);
		else
			return new LogicMethodParser(method);
	}
	
	AbstractLogicMethodParser(Method method) {
		logicMethod = (LM) AbstractLogicMethod.create(method);
		parse();
	}

	public AbstractLogicMethodParser parse() {
		String paramsLogicString = concatenateTokens(getInputTokens());
		allLogicStrings = paramsLogicString + RETURN_SEPARATOR;
		eachSolutionValue = logicMethod.getEachSolutionValue();
		if(eachSolutionValue != null) {
			allLogicStrings += eachSolutionValue;
		}
		foundSymbols = getAllSymbols(allLogicStrings);
		expressions = getJavaExpressions(allLogicStrings);
		expressionsReplacementMap = expressionsReplacementMap(expressions);
		return this;
	}
	
	public LM getLogicMethod() {
		return logicMethod;
	}
	
	public Method getMethod() {
		return logicMethod.getWrappedMethod();
	}
	
	protected abstract String[] getInputTokens();

	public String[] resolveInputTokens(Object targetObject, Object[] oldParams) {
		String inputString = allLogicStrings.split(RETURN_SEPARATOR)[0];
		inputString = replaceSymbolsAndExpressions(inputString, foundSymbols, expressionsReplacementMap, targetObject, oldParams);
		return splitConcatenatedTokens(inputString);
	}
	
	public String resolveEachSolutionValue(Object targetObject, Object[] oldParams) {
		if(eachSolutionValue == null)
			return null;
		String solvedEachSolutionValue = replaceSymbolsAndExpressions(eachSolutionValue, foundSymbols, expressionsReplacementMap, targetObject, oldParams);
		return solvedEachSolutionValue;
	}
	
	private static String concatenateTokens(Object[] tokens) {
		String concatenatedParams = "";
		for(int i=0; i<tokens.length; i++) {
			concatenatedParams+=tokens[i];
			if(i<tokens.length-1)
				concatenatedParams+=PARAMETERS_SEPARATOR;
		}
		return concatenatedParams;
	}
	
	private static String[] splitConcatenatedTokens(String tokensString) {
		return tokensString.split(PARAMETERS_SEPARATOR);
	}
	
	
	
	/**
	 * 
	 * @param pos a 1-based index of the java parameter
	 * @return the parameter symbol representation
	 */
	public static String parameterSymbol(int pos) {
		return PARAMETERS_PREFIX+pos;
	}
	
	/**
	 * 
	 * @param symbol
	 * @return true if @symbol is a valid instance property symbol
	 */
	public static boolean isInstancePropertySymbol(String symbol) {
		if(symbol==null)
			return false;
		Pattern pattern = Pattern.compile(Pattern.quote(INSTANCE_PROPERTY_PREFIX) + JAVA_NAME_REX);
		Matcher findingMatcher = pattern.matcher(symbol);
		return findingMatcher.matches();
		//return symbol.substring(0, 1).equals(INSTANCE_PROPERTY_PREFIX);
	}
	
	private static String getPropertyName(String symbol) {
		return symbol.substring(INSTANCE_PROPERTY_PREFIX.length());
	}
	
	
	/**
	 * 
	 * @param concatenatedParams
	 * @return A list of symbol params referenced in the string sent as a parameter
	 */
	public static List<String> getAllSymbols(String concatenatedParams) {
		/**
		 * A Set is used to avoid duplicates
		 * LinkedHashSet preserves the insertion order
		 */
		Set<String> symbolsSet = new LinkedHashSet<String>();
		Pattern pattern = Pattern.compile("("+Pattern.quote(PARAMETERS_PREFIX)+"(\\d+|"+Pattern.quote(ALL_PARAMS_SUFFIX)+"))|"+Pattern.quote(INSTANCE_PROPERTY_PREFIX)+JAVA_NAME_REX);
		Matcher findingMatcher = pattern.matcher(concatenatedParams);
		while(findingMatcher.find()) {
			String match = findingMatcher.group();
			symbolsSet.add(match);
		}
		return new ArrayList<String>(symbolsSet);
	}
	

	/**
	 * Extract the expression value from a delimited expression
	 * @param delimitedExpression
	 * @return
	 */
	public static String getExpressionValue(String delimitedExpression) {
		Pattern pattern = Pattern.compile(PARAMETER_JAVA_REX);
		Matcher findingMatcher = pattern.matcher(delimitedExpression);
		findingMatcher.find();
		return findingMatcher.group(1);
	}
	
	/**
	 * 
	 * @param expression
	 * @return true if expression is a valid expression
	 */
	private static boolean isValidExpression(String expression) {
		return normalizeExpression(expression) != null;
	}
	
	/**
	 * Currently this method just trims the expression sent as parameter, and delete any sequence of ";" or blanck spaces at the end of the expression
	 * @param expression is the original Java expression to be normalized
	 * @return the normalized expression, null if once normalized the expression was empty
	 */
	public static String normalizeExpression(String expression) {
		if(expression == null)
			return null;
		expression = expression.trim();
		if(expression.isEmpty())
			return null;
		if(expression.substring(expression.length()-1).equals(";")) {
			Pattern pattern = Pattern.compile("(.*?)([\\s;]+)$");
			Matcher findingMatcher = pattern.matcher(expression);
			if(!findingMatcher.find())
				return null;
			expression = findingMatcher.group(1);
		}
		if(expression.isEmpty())
			return null;
		return expression;
	}
	
	
	
	/**
	 * 
	 * @param expression a delimited string with a java expression on it
	 * @return a list with all the java expressions found, in the same order that they were located. No duplicates are included
	 */
	public static List<String> getJavaExpressions(String expression) {
		/**
		 * A Set is used to avoid duplicates
		 * LinkedHashSet preserves the insertion order
		 */
		Set<String> javaExpressionsSet = new LinkedHashSet<String>(); 
		/*
		 * the question mark is to specify a reluctant quantifier, so 'any' characters (the '.') will occur the minimum possible amount of times
		 */
		Pattern pattern = Pattern.compile(PARAMETER_JAVA_REX);
		Matcher findingMatcher = pattern.matcher(expression);
		while(findingMatcher.find()) {
			String match = findingMatcher.group();
			javaExpressionsSet.add(match);
		}
		return new ArrayList<String>(javaExpressionsSet);
	}

	/**
	 * This method answers the method name returning a Java expression declared in position @position at the original logic method
	 * The name of the method returning the expression is based on the original method name (obtained with: method.toGenericString())
	 * Note that this name depend on the class where the method is implemented
	 * At the time the code is generated, the method used to generate this name belongs to the abstract logic class
	 * At the time the method is invoked, the method used to generate this name belongs to the NON-abstract generated class (with suffix GENERATED_CLASS_SUFFIX)
	 * For that reason, this method attempts to return the same result at both times by means of:
	 * - dropping any abstract keyword from the method name
	 * - dropping the GENERATED_CLASS_SUFFIX from the method name
	 * @param method
	 * @param position
	 * @return
	 */
	private static String methodNameForExpression(Method method, int position) {
		String normalizedMethodName = method.toGenericString();
		normalizedMethodName = normalizedMethodName.replaceAll(Pattern.quote(LogicObjectInstrumentation.GENERATED_CLASS_SUFFIX), "");
		normalizedMethodName = normalizedMethodName.replaceAll("<.*>", ""); //suppress generics information from the method name
		normalizedMethodName = normalizedMethodName.replaceAll(" abstract ", "_");
		normalizedMethodName = normalizedMethodName.replaceAll("\\(|\\)", "");
		normalizedMethodName = normalizedMethodName.replaceAll(" |\\.", "_");
		return generatedMethodsPrefix + normalizedMethodName + "_exp" + position;
	}
	
	
	private String methodNameForExpression(int position) {
		return methodNameForExpression(getMethod(), position);
	}

	
	/**
	 * 
	 * @param termString a string with all the new params concatenated
	 * @param oldParams the java method params
	 * @return
	 */
	private String replaceSymbolsAndExpressions(String termString, List<String> setSymbols, Map<String, String> expressionsMap, Object targetObject, Object[] oldParams) {
		Map<String, String> symbolsMap = symbolsReplacementMap(targetObject, oldParams, setSymbols);

		//replacing symbols
		for(String symbol : setSymbols) {			
			String termObjectString = symbolsMap.get(symbol);
			termString=termString.replaceAll(Pattern.quote(symbol), termObjectString);
		}
		//replacing java expressions for the result of a method invocation
		for(Entry<String, String> entry : expressionsMap.entrySet()) {
			try {
				String replacementValue;
				String methodName = entry.getValue();
				if(methodName == null || methodName.isEmpty())
					replacementValue = "";
				else {
					Method method = targetObject.getClass().getMethod(methodName);
					Object expressionResult = method.invoke(targetObject); //result contains the value of the java expression
					Term expressionAsTerm = ObjectToTermAdapter.asTerm(expressionResult);
					replacementValue = expressionAsTerm.toString();
				}
				termString=termString.replaceAll(Pattern.quote(entry.getKey()), replacementValue);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return termString;
	}


	/**
	 * Builds a dictionary of symbol params to values
	 * @param parameters the java method parameters
	 * @return
	 */
	private Map<String, String> symbolsReplacementMap(Object targetObject, Object[] parameters, List<String> symbols) {
		Map<String, String> dictionary = new HashMap<String, String>();
		if(parameters.length > 0) {
			LogicEngine engine = LogicEngine.getDefault();
			List<Term> listTerms = new ArrayList<Term>();
			boolean allParamsRequired = symbols.contains(AbstractLogicMethodParser.ALL_PARAMS_SYMBOL);
			for(int i = 0; i<parameters.length; i++) {
				String paramName = AbstractLogicMethodParser.parameterSymbol(i+1);
				if(allParamsRequired || symbols.contains(paramName)) {
					Term termParam = ObjectToTermAdapter.asTerm(parameters[i]);
					if(engine.nonAnonymousVariablesNames(termParam).size() > 0)
						throw new RuntimeException("Parameter objects cannot contain free non-anonymous variables: "+termParam);//in order to avoid name collisions
					dictionary.put(paramName, termParam.toString());
					listTerms.add(termParam);
				}
			}
			if(allParamsRequired)
				dictionary.put(AbstractLogicMethodParser.ALL_PARAMS_SYMBOL, engine.termListToTextSequence(listTerms));
		}
		if(symbols.contains(AbstractLogicMethodParser.THIS_SYMBOL)) {
			Term thisAsTerm = new ObjectToTermAdapter().adapt(targetObject);
			dictionary.put(AbstractLogicMethodParser.THIS_SYMBOL, thisAsTerm.toString());
		}
		
		for(String setSymbol : symbols) {
			if(AbstractLogicMethodParser.isInstancePropertySymbol(setSymbol)) {
				String instanceVarName = AbstractLogicMethodParser.getPropertyName(setSymbol);
				Term instanceVarAsTerm = instanceVarAsTerm = LogicObjectAdapter.fieldAsTerm(targetObject, instanceVarName); //TODO this method should be in ObjectToTermAdapter
				dictionary.put(setSymbol, instanceVarAsTerm.toString());
			}
		}
		return dictionary;
	}
	
	private Map<String, String> expressionsReplacementMap(List<String> delimitedExpressions) {		
		Map<String, String> expressionsReplacementMap = new HashMap<String, String>();
		for(int i = 0; i<delimitedExpressions.size(); i++) {
			String delimitedExpression = delimitedExpressions.get(i);
			String expression = AbstractLogicMethodParser.getExpressionValue(delimitedExpression);
			String substitutionValue;
			if(AbstractLogicMethodParser.isValidExpression(expression)) {
				substitutionValue = methodNameForExpression(i + 1);//i+1 to work with a 1-based index
			} else {
				logger.warn("The expression: " + delimitedExpression + "in the method "+ getMethod().toGenericString()+" is not valid. It will be ignored.");
				substitutionValue = "";
			}
			expressionsReplacementMap.put(delimitedExpression, substitutionValue);
		}
		return expressionsReplacementMap;
	}

	
	public Map<String, String> generatedMethodsMap() {
		Map<String, String> generatedMethodsMap = new HashMap<String, String>();
		for(Entry<String, String> entry : expressionsReplacementMap.entrySet()) {
			String expression = entry.getKey();
			expression = AbstractLogicMethodParser.getExpressionValue(expression);
			if(AbstractLogicMethodParser.isValidExpression(expression)) {
				expression = AbstractLogicMethodParser.normalizeExpression(expression);
				String methodName = entry.getValue();
				generatedMethodsMap.put(methodName, expression);
			}
		}
		return generatedMethodsMap;
	}
	

	/*
	public Map<String, String> generatedMethodsMap() {
		Map<String, String> parametersExpressionsMethodsMap, returnExpressionMethodsMap, allExpressionMethodsMap;
		parametersExpressionsMethodsMap = findExpressionHelperMethods(expressionReplacementMap_parameters);
		
		String returnString = logicMethod.getEachSolutionValue();
		if(returnString != null) {
			returnExpressionMethodsMap = findExpressionHelperMethods(expressionReplacementMap_returnValue);
		} else {
			returnExpressionMethodsMap = new HashMap<String, String>();
		}
		allExpressionMethodsMap = parametersExpressionsMethodsMap;
		allExpressionMethodsMap.putAll(returnExpressionMethodsMap);
		return allExpressionMethodsMap;
	}
*/
	
}


