package cz.bliksoft.javautils;

/**
 * Converting of common units e.g. for printing support
 */
public class UnitConverter {
	/**
	 * Converts millimeters (mm) to points (pt)
	 * 
	 * @param mm the value in mm
	 * @return the value in pt
	 */
	public static double mm2pt(double mm) {
		return mm * 2.8346;
	}

	/**
	 * Converts points (pt) to millimeters (mm)
	 * 
	 * @param pt the value in pt
	 * @return the value in mm
	 */
	public static double pt2mm(double pt) {
		return pt / 2.8346;
	}

	/**
	 * Converts millimeters (mm) to inches (in)
	 * 
	 * @param mm the value in mm
	 * @return the value in inches
	 */
	public static double mm2in(double mm) {
		return mm / 25.4;
	}

	/**
	 * Converts inches (in) to millimeters (mm)
	 * 
	 * @param in the value in inches
	 * @return the value in mm
	 */
	public static double in2mm(double in) {
		return in * 25.4;
	}

	/**
	 * Converts millimeters (mm) to pixels (px)
	 * 
	 * @param mm         the value in mm
	 * @param resolution the resolution in dpi (dots per inch)
	 * @return the value in pixels
	 */
	public static int mm2px(double mm, int dpi) {
		return (int) Math.round(mm2in(mm) * dpi);
	}

	/**
	 * Converts pixels (px) to millimeters (mm)
	 * 
	 * @param px         dimension in px
	 * @param resolution dpi (dotsPerInch)
	 * @return size in mm
	 */
	public static double px2mm(double px, int dpi) {
		return in2mm(px) / dpi;
	}
}
