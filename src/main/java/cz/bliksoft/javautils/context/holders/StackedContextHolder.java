package cz.bliksoft.javautils.context.holders;

import java.util.Stack;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.context.Context;

public class StackedContextHolder extends SingleContextHolder {

	public StackedContextHolder(String comment) {
		super(comment);
	}

	Stack<Context> stack = new Stack<>();

	public Context peekRoot() {
		return stack.firstElement();
	}

	public Context peek() {
		if (stack.isEmpty())
			return null;
		return stack.peek();
	}

	public Context pop() {
		Context result = stack.pop();
		if (result == null)
			return null;
		Context newTop = peek();
		if (newTop != null)
			newTop.removeContext(result);
		replaceContext(newTop);
		return result;
	}

	public void push(Context context) {
		Context top = peek();
		stack.push(context);
		replaceContext(context);
		if (top != null)
			top.addContext(context);
	}

	@Override
	protected void dumpValues(StringBuilder sb, String prefix) {
		super.dumpValues(sb, prefix);
		for (int i = 0; i < stack.size() - 1; i++)
			sb.append(prefix).append("[stack] ").append(stack.get(i)).append("\n");
	}

	@Override
	public String toString() {
		if (StringUtils.hasText(this.comment))
			return (isLevelContext ? "L" : "") + "StackedCTXHolder: " + this.comment;
		else
			return (isLevelContext ? "L" : "") + "StackedCTXHolder";
	}
}
