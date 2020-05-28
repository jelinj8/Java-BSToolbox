package cz.bliksoft.javautils;

import java.util.ListIterator;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ExtendedPosixParser extends PosixParser {
	private boolean ignoreUnrecognizedOption;

	public ExtendedPosixParser(final boolean ignoreUnrecognizedOption) {
		this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void processOption(final String arg, final ListIterator iter)
			throws ParseException {
		boolean hasOption = getOptions().hasOption(arg);

		if (hasOption || !ignoreUnrecognizedOption) {
			super.processOption(arg, iter);
		}/*else{
			iter.next();
		}*/
	}
}
