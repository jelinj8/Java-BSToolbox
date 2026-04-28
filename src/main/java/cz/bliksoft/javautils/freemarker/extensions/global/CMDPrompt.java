package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.io.input.CloseShieldInputStream;

import cz.bliksoft.javautils.Messages;
import cz.bliksoft.javautils.StringUtils;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelListSequence;

public class CMDPrompt implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(CMDPrompt.class.getName());

	private static final int RESULT_KEY = 3;
	private static final int DISP_KEY = 2;
	private static final int ID_KEY = 1;

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		String prompt = String.valueOf(arguments.get(0));

		String defaultResult = (arguments.size() > 1 ? String.valueOf(arguments.get(1)) : null);
		if (arguments.size() < 3) {
			try (Scanner scnr = new Scanner(CloseShieldInputStream.wrap(System.in))) { // Create a Scanner object
				if (StringUtils.hasLength(prompt)) {
					System.out.print(prompt);
					if (defaultResult != null)
						System.out.print(" [" + defaultResult + "]");
					System.out.print(">");
				} else {
					System.out.print(Messages.getString("CMDPrompt.EnterValue")); //$NON-NLS-1$
					if (defaultResult != null)
						System.out.print(" [" + defaultResult + "]");
					System.out.print(">");
				}
				String res = scnr.nextLine();
				if (StringUtils.hasLength(res))
					return res;
				else
					return defaultResult;
			}
		} else {
			List<Map<Integer, Object>> options = new LinkedList<>();
			Object items = arguments.get(2);
			@SuppressWarnings("unchecked")
			List<Object> o = ((List<Object>) ((TemplateModelListSequence) items).getWrappedObject());
			int maxOptLen = 0;
			for (Object opt : o) {
				Map<Integer, Object> optionMap = new HashMap<>();
				if (opt instanceof SimpleSequence) {
					SimpleSequence s = (SimpleSequence) opt;
					switch (s.size()) {
					case 3:
						optionMap.put(RESULT_KEY, s.get(2));
					case 2:
						optionMap.put(DISP_KEY, s.get(1));
					case 1:
						optionMap.put(ID_KEY, s.get(0));
						break;
					}
				} else {
					optionMap.put(ID_KEY, String.valueOf(opt));
				}
				options.add(optionMap);
				int l = String.valueOf(optionMap.get(ID_KEY)).length();
				if (l > maxOptLen)
					maxOptLen = l;
			}
			try (Scanner scnr = new Scanner(CloseShieldInputStream.wrap(System.in))) {
				while (true) {
					if (StringUtils.hasLength(prompt)) {
						System.out.println(prompt);
					} else {
						System.out.println(Messages.getString("CMDPrompt.SelectOption")); //$NON-NLS-1$
					}
					for (Map<Integer, Object> opt : options) {
						System.out.print(String.format(" %1$" + maxOptLen + "s", opt.get(ID_KEY)));
						if (opt.get(DISP_KEY) != null) {
							System.out.print(": ");
							System.out.println(opt.get(DISP_KEY));
						} else {
							System.out.println();
						}
					}
					System.out.print(">");
					String resp = scnr.nextLine();
					if (StringUtils.isEmpty(resp))
						return defaultResult;
					for (Map<Integer, Object> opt : options) {
						if (resp.equals(String.valueOf(opt.get(ID_KEY)))) {
							Object result = opt.get(RESULT_KEY);
							if (result != null)
								return result;
							return opt.get(ID_KEY);
						}
					}
				}
			}
		}
	}

}
