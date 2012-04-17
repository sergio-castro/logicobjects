package org.logicobjects.lib;

import java.util.ArrayList;

import org.logicobjects.annotation.LDelegationObject;
import org.logicobjects.annotation.method.LMethod;
import org.logicobjects.annotation.method.LSolution;

/**
 * A logic list
 *
 */
@LDelegationObject(name="list", imports="library(types_loader)")
//LObject(name="list", imports="library(types_loader)")
public abstract class LList<T> extends ArrayList<T> {
	
	@LSolution("L")
	@LMethod(parameters={"@this", "L"})
	public abstract int length();
	

	@LMethod(parameters={"@this", "_"})
	public abstract int member();
	
}
