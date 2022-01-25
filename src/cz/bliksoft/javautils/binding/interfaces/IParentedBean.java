package cz.bliksoft.javautils.binding.interfaces;

public interface IParentedBean<P> {
	public static final String PROP_BEAN_PARENT = "beanParent";
	
	P getBeanParent();
}
