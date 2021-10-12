package cz.bliksoft.javautils.ws;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import cz.bliksoft.javautils.ws.handlers.SOAPLogHandler;

public abstract class BaseService<T> {
	protected SecurityHandler securityHandler = null;

	public SecurityHandler getSecurityHandler() {
		return securityHandler;
	}

	protected T service;

	public T getService() {
		return service;
	}

	protected Class<?> serviceClass;

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	@SuppressWarnings("unchecked")
	protected BaseService(Service svc, Class<?> cls, String endpoint) {
		serviceClass = cls;
		service = (T) svc.getPort(cls);
		securityHandler = new SecurityHandler();
		Binder.bindService((BindingProvider) service, securityHandler, endpoint);
	}

	@SuppressWarnings("unchecked")
	protected BaseService(Service svc, Class<?> cls, String endpoint, SecurityHandler securityHandler) {
		serviceClass = cls;
		service = (T) svc.getPort(cls);
		if (securityHandler != null) {
			this.securityHandler = securityHandler;
			Binder.bindService((BindingProvider) service, securityHandler, endpoint);
		}
	}

	protected void setCredentials(String user, String pwd) {
		securityHandler.setUser(user);
		securityHandler.setPass(pwd);
	}

	public void addLogHandler() {
		Binder.addHandler((BindingProvider) service, new SOAPLogHandler(serviceClass.getSimpleName()));
	}

	public void addLogHandler(String serviceName) {
		if (serviceName == null)
			addLogHandler();
		else
			Binder.addHandler((BindingProvider) service, new SOAPLogHandler(serviceName));
	}
}
