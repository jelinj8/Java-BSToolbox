package cz.bliksoft.javautils.binding.binder;

import cz.bliksoft.javautils.binding.binder.interfaces.IActionBean;
import cz.bliksoft.javautils.binding.binder.interfaces.IActionObject;
import cz.bliksoft.javautils.binding.binder.interfaces.IActionPresentationModel;
import cz.bliksoft.javautils.binding.binder.interfaces.IPresentationModel;

/*
 * Copyright (c) 2002-2015 JGoodies Software GmbH. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of JGoodies Software GmbH nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Creates binders for Objects, Beans, PresentationModels, and ActionObject,
 * ActionBeans, and ActionPresentationModels. Since the latter three classes do
 * not ship with the JGoodies Binding, these are described by interfaces.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 *
 * @since 2.7
 */
public final class Binders {

	private Binders() {
		// Overrides default constructor; prevents instantiation.
	}

	/**
	 * Creates and returns a general binder for objects that can bind Actions,
	 * ListModel + ListSelectionModel, ValueModels, and SelectionInLists.
	 *
	 * @return the created binder
	 */
	public static ObjectBinder binder() {
		return new ObjectBinderImpl();
	}

	/**
	 * Creates and returns a binder that adds the capability to bind Actions that
	 * are looked up by an Action name to the general binder returned by
	 * {@link #binder()}.
	 *
	 * @param object provides Actions for Action names
	 * @return the created binder
	 * 
	 * @deprecated Replaced by {@code Handlers.handlerFor(IActionObject)} from the
	 *             JGoodies Sandbox.
	 */
	@Deprecated
	public static ActionObjectBinder binderFor(IActionObject object) {
		return new ActionObjectBinderImpl<IActionObject>(object);
	}

	/**
	 * Creates and returns a binder for beans that can bind bean properties as well
	 * as the object bindings: Actions, ListModel + ListSelectionModel, ValueModels,
	 * and SelectionInLists.
	 * <p>
	 *
	 * The parameter type is Object, not ObservableBean or ObservableBean2, because
	 * any Object may be a bean. If {@code bean} is not a bean, runtime exceptions
	 * will be thrown during the execution.
	 *
	 * @param bean the bean used to bind bean properties
	 * @return the created binder
	 */
	public static BeanBinder binderFor(Object bean) {
		return new BeanBinderImpl(bean);
	}

	/**
	 * Creates and returns a binder that adds the capability to bind Actions that
	 * are looked up by an Action name to the binder returned by
	 * {@link #binderFor(Object)}.
	 *
	 * @param bean the bean used to bind bean properties and to look up Actions
	 * @return the created binder
	 * 
	 * @deprecated Replaced by {@code Handlers.handlerFor(IActionBean)} from the
	 *             JGoodies Sandbox.
	 */
	@Deprecated
	public static ActionBeanBinder binderFor(IActionBean bean) {
		return new ActionBeanBinderImpl(bean);
	}

	/**
	 * Creates and returns a binder for presentation models that can bind bean
	 * properties, (synthetic) properties of the presentation model, as well as the
	 * object bindings: Actions, ListModel + ListSelectionModel, ValueModels, and
	 * SelectionInLists.
	 *
	 * @param model the presentation model that holds a bean (used to bind bean
	 *              properties) and that may provide its own synthetic properties
	 * @return the created binder
	 */
	public static PresentationModelBinder binderFor(IPresentationModel<?> model) {
		return new PresentationModelBinderImpl(model);
	}

	/**
	 * Creates and returns a binder that adds the capability to bind Actions that
	 * are looked up by an Action name to the binder returned by
	 * {@link #binderFor(IPresentationModel)}.
	 *
	 * @param model the presentation model that holds a bean (used to bind bean
	 *              properties), and that may provide its own synthetic properties,
	 *              and that provides Actions for Action names
	 * @return the created binder
	 * 
	 * @deprecated Replaced by {@code Handlers.handlerFor(IActionPresentationModel)}
	 *             from the JGoodies Sandbox.
	 */
	@Deprecated
	public static ActionPresentationModelBinder binderFor(IActionPresentationModel<?> model) {
		return new ActionPresentationModelBinderImpl(model);
	}

}
