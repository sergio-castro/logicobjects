package org.logicobjects.converter.old;

import org.jpc.Jpc;

public abstract class AnnotatedContextFactory {

	protected Jpc jpcContext;

	public AnnotatedContextFactory(Jpc jpcContext) {
		this.jpcContext = jpcContext;
	}
	
}
