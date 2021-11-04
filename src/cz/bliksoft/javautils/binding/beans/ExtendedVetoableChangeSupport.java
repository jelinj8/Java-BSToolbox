package cz.bliksoft.javautils.binding.beans;

import java.beans.VetoableChangeSupport;

public class ExtendedVetoableChangeSupport extends VetoableChangeSupport {

	private static final long serialVersionUID = 8896183689763075602L;

	public ExtendedVetoableChangeSupport(Object sourceBean) {
		super(sourceBean);
	}

}
