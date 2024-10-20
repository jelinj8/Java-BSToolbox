package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;
import java.util.regex.Pattern;

import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * better to use ?replace("\\s*$", "", "r") or ?replace(^\\s*", "", "r")
 */
public class Trim implements TemplateMethodModelEx {

	public enum Direction {
		LEFT, RIGHT, BOTH
	}

	private Pattern left = null;
	private Pattern right = null;

	public Trim(Direction d) {
		dir = d;
	}

	private Direction dir = Direction.LEFT;

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		try {
			switch (dir) {
			case BOTH:
				return arg0.get(0).toString().trim();
			case LEFT:
				if (left == null)
					left = Pattern.compile("^\\s*");
				return left.matcher(arg0.get(0).toString()).replaceAll("");
			case RIGHT:
				if (right == null)
					right = Pattern.compile("\\s*$");
				return right.matcher(arg0.get(0).toString()).replaceAll("");
			}
			return freemarker.ext.dom.NodeModel.wrap(XmlUtils.convertStringToNode(arg0.get(0).toString()));
		} catch (Exception e) {
			throw new TemplateModelException("Failed to parse XML", e);
		}
	}

}
