package cz.bliksoft.javautils.binding.interfaces;

public interface IBeanChannel<B> {
	public IValueModel<B> getBeanChannel();
	public void release();
}
