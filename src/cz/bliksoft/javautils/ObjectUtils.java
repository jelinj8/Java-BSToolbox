package cz.bliksoft.javautils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cz.bliksoft.javautils.net.http.MultiPart;
import cz.bliksoft.javautils.net.http.MultiPart.PartType;

public class ObjectUtils {

	ObjectUtils() {

	}

	/**
	 * Provides a means to copy objects that do not implement Cloneable. Performs a
	 * deep copy where the copied object has no references to the original object
	 * for any object that implements Serializable. If the original is {@code null},
	 * this method just returns {@code null}.
	 *
	 * @param <T>
	 *                 the type of the object to be cloned
	 * @param original
	 *                 the object to copied, may be {@code null}
	 * @return the copied object
	 *
	 * @since 1.1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T deepCopy(T original) {
		if (original == null) {
			return null;
		}
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			final ObjectOutputStream oas = new ObjectOutputStream(baos);
			oas.writeObject(original);
			oas.flush();
			// close is unnecessary
			final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			final ObjectInputStream ois = new ObjectInputStream(bais);
			return (T) ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException("Deep copy failed", e); //$NON-NLS-1$
		}
	}

	/**
	 * Checks and answers if the two objects are both {@code null} or equal.
	 *
	 * <pre>
	 * Objects.equals(null, null) == true
	 * Objects.equals("Hi", "Hi") == true
	 * Objects.equals("Hi", null) == false
	 * Objects.equals(null, "Hi") == false
	 * Objects.equals("Hi", "Ho") == false
	 * </pre>
	 *
	 * @param o1
	 *           the first object to compare
	 * @param o2
	 *           the second object to compare
	 * @return boolean {@code true} if and only if both objects are {@code null} or
	 *         equal according to {@link Object#equals(Object) equals} invoked on
	 *         the first object
	 */
	public static boolean equals(Object o1, Object o2) {
		return o1 == o2 || o1 != null && o1.equals(o2);
	}

	/**
	 * checks if both objects are not null and identic or equal
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public static boolean equalsNotNull(Object o1, Object o2) {
		return o1 != null && o2 != null && (o1 == o2 || o1.equals(o2));
	}

	private static final String PAD_TEXT = "\t";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String describe(Object o, String pad) {
		if (pad == null)
			pad = "";

		final String localPad = pad;
		final String localPad2 = pad + PAD_TEXT;
		StringBuilder sb = new StringBuilder();
		if (o == null) {
			sb.append("<NULL>");
		} else if (o instanceof Boolean) {
			sb.append(o.toString());
		} else if (o instanceof Integer) {
			sb.append(NumericUtils.numberAsString(o));
		} else if (o instanceof String) {
			sb.append(StringUtils.prependLines("\"" + (String) o + "\"", null, localPad2 + "|"));
		} else if (o instanceof Entry) {
			Entry e = (Entry) o;
			sb.append("[");
			sb.append(describe(e.getKey(), null));
			sb.append("]=");
			sb.append(describe(e.getValue(), null));
		} else if (o instanceof MultiPart) {
			MultiPart mp = (MultiPart) o;
			if (StringUtils.hasLength(mp.name)) {
				sb.append("[");
				sb.append(mp.name);
				sb.append("]=");
				if (mp.type == PartType.TEXT) {
					sb.append(describe(mp.value, localPad));
				} else {
					sb.append("FILE:");
					sb.append(mp.contentType);
					sb.append(":");
					sb.append(mp.filename);
				}
			}
			// } else if (o instanceof Optional) {
			// Optional op = (Optional) o;
			// if (op.isPresent()) {
			// sb.append("[X]");
			// sb.append(describe(op.get(), localPad2));
			// } else {
			// sb.append("[O]");
			// }
		} else if (o instanceof Map) {
			sb.append("{");
			if (((Map) o).size() > 0) {
				sb.append("\n");
				((Map) o).forEach((k, v) -> {
					sb.append(localPad);
					sb.append(PAD_TEXT);
					sb.append("[");
					sb.append(describe(k, localPad2));
					sb.append("]");
					sb.append("=");
					sb.append(describe(v, localPad2));
					sb.append("\n");
				});

				sb.append(localPad);
			}
			sb.append("}");
		} else if (o instanceof List) {
			sb.append("[");
			if (((List) o).size() > 0) {
				sb.append("\n");

				((List) o).forEach((v) -> {
					sb.append(localPad);
					sb.append(PAD_TEXT);
					sb.append(describe(v, localPad2));
					sb.append("\n");
				});

				sb.append(localPad);
			}
			sb.append("]");
		} else if (o instanceof Enumeration) {
			// sb.append(localPad);
			sb.append("[");
			Enumeration e = (Enumeration) o;
			if (e.hasMoreElements()) {
				sb.append("\n");
				Object o2 = e.nextElement();
				while (o2 != null) {
					sb.append(localPad);
					sb.append(PAD_TEXT);
					sb.append(describe(o2, localPad2));
					sb.append("\n");
				}
				sb.append(localPad);
			}
			sb.append("]");
		} else if (o instanceof Collection) {
			sb.append("[");
			Collection e = (Collection) o;
			if (!e.isEmpty())
				sb.append("\n");
			for (Object object : e) {
				sb.append(localPad);
				sb.append(PAD_TEXT);
				sb.append(describe(object, localPad2));
				sb.append("\n");
			}
			sb.append(localPad);
			sb.append("]");
		} else {
			sb.append("{");
			sb.append(o.getClass().getName());
			sb.append("}: ");
			sb.append(StringUtils.prependLines(o.toString(), null, localPad));
		}

		return sb.toString();
	}

	public static void serializeToFile(File file, Object data) throws FileNotFoundException, IOException {
		try (FileOutputStream fs = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fs)) {
			oos.writeObject(data);
		}
	}

	public static Object deserializeFromFile(File file)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
			return ois.readObject();
		}
	}
	
}
