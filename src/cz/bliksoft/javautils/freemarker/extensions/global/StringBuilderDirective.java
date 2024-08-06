package cz.bliksoft.javautils.freemarker.extensions.global;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;

/**
 * User directive + TemplateMethod for collecting data into StringWriter
 */
public class StringBuilderDirective implements TemplateDirectiveModel, TemplateMethodModelEx {

	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		if (params.isEmpty() || params.size() != 1) {
			throw new TemplateModelException(
					"This directive expects a single parameter - a previously created StringBuilder instance, argument name is ignored");
		}

		@SuppressWarnings("unchecked")
		Iterator<Entry<String, TemplateModel>> iterator = params.entrySet().iterator();

		Entry<String, TemplateModel> firstParam = iterator.next();

		Writer w = (Writer) DeepUnwrap.permissiveUnwrap(firstParam.getValue());

		if (loopVars.length != 0) {
			throw new TemplateModelException("This directive doesn't allow loop variables.");
		}

		if (body != null) {
			body.render(w);
		} else {
			// empty directive body, do nothing
//			env.getOut().write(w.toString());
		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments.size() == 0) {
			return new StringWriter();
		} else {
			return DeepUnwrap.permissiveUnwrap((TemplateModel) arguments.get(0)).toString();
		}
	}
}
