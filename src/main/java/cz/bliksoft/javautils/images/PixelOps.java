package cz.bliksoft.javautils.images;

/**
 * Pure {@code int[]} ARGB pixel-math core shared by the icon-spec engine:
 * geometry transforms, compositing, scaling, and the filters it builds on top
 * of (shadow, outline, mask, monochrome, keymask, …). Pixels are row-major
 * ({@code y * width + x}), matching what JavaFX's
 * {@code PixelReader.getPixels}/{@code PixelWriter.setPixels(...,
 * PixelFormat.getIntArgbInstance(), ...)} and AWT's
 * {@code BufferedImage.getRGB}/{@code setRGB} produce and consume — so callers
 * on either side incur no conversion beyond the unavoidable boundary copy.
 *
 * <p>
 * All functions are pure (return a new array) unless documented otherwise, and
 * perform no bounds validation beyond what's needed for clipping at canvas
 * edges — callers are expected to pass consistent {@code width}/{@code height}
 * for the given array length.
 */
public final class PixelOps {

	private PixelOps() {
	}

	// ---- Geometry -----------------------------------------------------

	/**
	 * Mirrors the image. {@code flipLeftRight == true} reverses each row
	 * (horizontal flip); {@code false} reverses the row order (vertical flip).
	 */
	public static int[] mirror(int[] src, int w, int h, boolean flipLeftRight) {
		int[] dst = new int[src.length];
		for (int y = 0; y < h; y++) {
			int dstBase = y * w;
			if (flipLeftRight) {
				int srcBase = y * w;
				for (int x = 0; x < w; x++)
					dst[dstBase + x] = src[srcBase + (w - 1 - x)];
			} else {
				System.arraycopy(src, (h - 1 - y) * w, dst, dstBase, w);
			}
		}
		return dst;
	}

	/**
	 * Losslessly rotates by a multiple of 90 degrees ({@code degrees} must be 90,
	 * 180, or 270). The result is {@code h x w} for 90/270, {@code w x h} for 180 —
	 * see {@link #rotatedWidth}/{@link #rotatedHeight}.
	 */
	public static int[] rotateLossless(int[] src, int w, int h, int degrees) {
		int outW = rotatedWidth(w, h, degrees);
		int outH = rotatedHeight(w, h, degrees);
		int[] dst = new int[outW * outH];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int nx, ny;
				switch (degrees) {
				case 90:
					nx = h - 1 - y;
					ny = x;
					break;
				case 180:
					nx = w - 1 - x;
					ny = h - 1 - y;
					break;
				default: // 270
					nx = y;
					ny = w - 1 - x;
					break;
				}
				dst[ny * outW + nx] = src[y * w + x];
			}
		}
		return dst;
	}

	/**
	 * Output width of {@link #rotateLossless} for the given source size and angle.
	 */
	public static int rotatedWidth(int w, int h, int degrees) {
		return (degrees == 180) ? w : h;
	}

	/**
	 * Output height of {@link #rotateLossless} for the given source size and angle.
	 */
	public static int rotatedHeight(int w, int h, int degrees) {
		return (degrees == 180) ? h : w;
	}

	/**
	 * Rotates by an arbitrary angle (degrees, clockwise) around the image centre,
	 * using premultiplied-alpha bilinear sampling. The canvas size is unchanged;
	 * corners may be clipped or left transparent.
	 */
	public static int[] rotateBilinear(int[] src, int w, int h, double angleDeg) {
		double rad = Math.toRadians(angleDeg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double cx = (w - 1) / 2.0;
		double cy = (h - 1) / 2.0;

		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double dxOut = x - cx;
				double dyOut = y - cy;
				// Inverse of CW screen-space rotation: rotate back (CCW)
				double srcX = cx + dxOut * cos + dyOut * sin;
				double srcY = cy - dxOut * sin + dyOut * cos;
				dst[y * w + x] = bilinearSample(src, w, h, srcX, srcY);
			}
		}
		return dst;
	}

	/**
	 * Lossless integer-pixel shift along both axes; pixels shifted off-canvas are
	 * dropped.
	 */
	public static int[] shiftAxisAligned(int[] src, int w, int h, int idx, int idy) {
		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			int sy = y - idy;
			if (sy < 0 || sy >= h)
				continue;
			for (int x = 0; x < w; x++) {
				int sx = x - idx;
				if (sx < 0 || sx >= w)
					continue;
				dst[y * w + x] = src[sy * w + sx];
			}
		}
		return dst;
	}

	/** Shift by an arbitrary offset using premultiplied-alpha bilinear sampling. */
	public static int[] shiftBilinear(int[] src, int w, int h, double dx, double dy) {
		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				dst[y * w + x] = bilinearSample(src, w, h, x - dx, y - dy);
		return dst;
	}

	/** Bilinear-resamples {@code src} (srcW x srcH) to {@code dstW x dstH}. */
	public static int[] scale(int[] src, int srcW, int srcH, int dstW, int dstH) {
		int[] dst = new int[dstW * dstH];
		double sx = (double) srcW / dstW;
		double sy = (double) srcH / dstH;
		for (int y = 0; y < dstH; y++)
			for (int x = 0; x < dstW; x++)
				dst[y * dstW + x] = bilinearSample(src, srcW, srcH, x * sx, y * sy);
		return dst;
	}

	/**
	 * Copies {@code src} (srcW x srcH) onto a new {@code dstW x dstH} canvas at
	 * offset ({@code dstX}, {@code dstY}), clipping to the canvas bounds. Pixels
	 * outside the copied region are transparent.
	 */
	public static int[] place(int[] src, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		int[] dst = new int[dstW * dstH];
		int srcStartX = Math.max(0, -dstX);
		int srcStartY = Math.max(0, -dstY);
		int canvasStartX = Math.max(0, dstX);
		int canvasStartY = Math.max(0, dstY);
		int copyW = Math.min(srcW - srcStartX, dstW - canvasStartX);
		int copyH = Math.min(srcH - srcStartY, dstH - canvasStartY);
		for (int y = 0; y < copyH; y++)
			System.arraycopy(src, (srcStartY + y) * srcW + srcStartX, dst, (canvasStartY + y) * dstW + canvasStartX,
					copyW);
		return dst;
	}

	// ---- Compositing ---------------------------------------------------

	/**
	 * SRC_OVER alpha-composites {@code src} (srcW x srcH) onto {@code dst} (dstW x
	 * dstH, modified in place) at offset ({@code dx}, {@code dy}), clipping to the
	 * destination bounds.
	 */
	public static void alphaCompositeInto(int[] dst, int dstW, int dstH, int[] src, int srcW, int srcH, int dx,
			int dy) {
		for (int y = 0; y < srcH; y++) {
			int ty = dy + y;
			if (ty < 0 || ty >= dstH)
				continue;
			int srcRowBase = y * srcW;
			int dstRowBase = ty * dstW;
			for (int x = 0; x < srcW; x++) {
				int tx = dx + x;
				if (tx < 0 || tx >= dstW)
					continue;
				int s = src[srcRowBase + x];
				int sa = (s >>> 24) & 0xFF;
				if (sa == 0)
					continue;
				int di = dstRowBase + tx;
				if (sa == 255) {
					dst[di] = s;
					continue;
				}

				int sr = (s >>> 16) & 0xFF;
				int sg = (s >>> 8) & 0xFF;
				int sb = s & 0xFF;

				int d = dst[di];
				int da = (d >>> 24) & 0xFF;
				int dr = (d >>> 16) & 0xFF;
				int dg = (d >>> 8) & 0xFF;
				int db = d & 0xFF;

				int invSa = 255 - sa;
				int outA = sa + (da * invSa + 127) / 255;
				int outR = (sr * sa + dr * da * invSa / 255 + 127) / 255;
				int outG = (sg * sa + dg * da * invSa / 255 + 127) / 255;
				int outB = (sb * sa + db * da * invSa / 255 + 127) / 255;

				dst[di] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}
		}
	}

	/**
	 * DST_OUT compositing: {@code src}'s alpha (srcW x srcH) subtracts from
	 * {@code dst}'s alpha (dstW x dstH, modified in place) at offset ({@code dx},
	 * {@code dy}); destination RGB is unchanged.
	 */
	public static void alphaSubtractInto(int[] dst, int dstW, int dstH, int[] src, int srcW, int srcH, int dx, int dy) {
		for (int y = 0; y < srcH; y++) {
			int ty = dy + y;
			if (ty < 0 || ty >= dstH)
				continue;
			int srcRowBase = y * srcW;
			int dstRowBase = ty * dstW;
			for (int x = 0; x < srcW; x++) {
				int tx = dx + x;
				if (tx < 0 || tx >= dstW)
					continue;
				int sa = (src[srcRowBase + x] >>> 24) & 0xFF;
				if (sa == 0)
					continue;
				int di = dstRowBase + tx;
				int d = dst[di];
				int da = (d >>> 24) & 0xFF;
				if (da == 0)
					continue;
				int outA = sa == 255 ? 0 : (da * (255 - sa) + 127) / 255;
				dst[di] = (outA << 24) | (d & 0x00FFFFFF);
			}
		}
	}

	// ---- Filters --------------------------------------------------------

	/**
	 * Diffuse fade-out shadow/glow: dilate the silhouette, blur it, then recolor.
	 */
	public static int[] shadow(int[] src, int w, int h, int argbColor, int radius, boolean filled) {
		int[] silhouette = silhouetteAlpha(src, filled);
		int[] dilated = dilateAlpha(silhouette, w, h, radius);
		int[] blurred = gaussianBlurAlpha(dilated, w, h, radius / 2.0 + 0.5);
		return colorizeMaxAlpha(blurred, silhouette, argbColor);
	}

	/** Sharp silhouette expansion (no blur), recolored to a solid outline. */
	public static int[] outline(int[] src, int w, int h, int argbColor, int radius, boolean filled) {
		int[] silhouette = silhouetteAlpha(src, filled);
		int[] dilated = dilateAlpha(silhouette, w, h, radius);
		return colorizeMaxAlpha(dilated, silhouette, argbColor);
	}

	/**
	 * Replaces every pixel's color with {@code argbColor}, keeping (or inverting)
	 * the source alpha.
	 */
	public static int[] mask(int[] src, int argbColor, boolean invert) {
		int r = (argbColor >>> 16) & 0xFF;
		int g = (argbColor >>> 8) & 0xFF;
		int b = argbColor & 0xFF;
		int colorAlpha = (argbColor >>> 24) & 0xFF;

		int[] dst = new int[src.length];
		for (int i = 0; i < src.length; i++) {
			int a = (src[i] >>> 24) & 0xFF;
			if (invert)
				a = 255 - a;
			int outA = colorAlpha == 255 ? a : (a * colorAlpha + 127) / 255;
			dst[i] = (outA << 24) | (r << 16) | (g << 8) | b;
		}
		return dst;
	}

	/**
	 * Converts to monochrome, tinted by {@code argbColor} and weighted by ITU-R
	 * BT.601 luminance.
	 */
	public static int[] monochrome(int[] src, int argbColor) {
		int cr = (argbColor >>> 16) & 0xFF;
		int cg = (argbColor >>> 8) & 0xFF;
		int cb = argbColor & 0xFF;

		int[] dst = new int[src.length];
		for (int i = 0; i < src.length; i++) {
			int p = src[i];
			int sa = (p >>> 24) & 0xFF;
			int sr = (p >>> 16) & 0xFF;
			int sg = (p >>> 8) & 0xFF;
			int sb = p & 0xFF;
			int l = (299 * sr + 587 * sg + 114 * sb) / 1000;
			dst[i] = (sa << 24) | ((cr * l / 255) << 16) | ((cg * l / 255) << 8) | (cb * l / 255);
		}
		return dst;
	}

	/**
	 * Makes every pixel whose RGB (alpha ignored) equals {@code keyRgb} fully
	 * transparent.
	 */
	public static int[] keymask(int[] src, int keyRgb) {
		int[] dst = new int[src.length];
		for (int i = 0; i < src.length; i++)
			dst[i] = (src[i] & 0x00FFFFFF) == keyRgb ? 0 : src[i];
		return dst;
	}

	/**
	 * Majority RGB (alpha ignored) among the four corner pixels — used to
	 * auto-detect a chroma key.
	 */
	public static int cornerMajorityRgb(int[] src, int w, int h) {
		int[] corners = { src[0] & 0x00FFFFFF, src[w - 1] & 0x00FFFFFF, src[(h - 1) * w] & 0x00FFFFFF,
				src[(h - 1) * w + w - 1] & 0x00FFFFFF };
		int best = corners[0], bestCount = 1;
		for (int i = 0; i < corners.length; i++) {
			int count = 0;
			for (int j = 0; j < corners.length; j++)
				if (corners[j] == corners[i])
					count++;
			if (count > bestCount) {
				bestCount = count;
				best = corners[i];
			}
		}
		return best;
	}

	/**
	 * Finds the bounding box of the "foreground" object on a — possibly textured —
	 * background, for autocrop purposes.
	 *
	 * <p>
	 * The image is downscaled (longest side ≤ 200px, also acting as a smoothing
	 * blur) so background texture noise is suppressed. Background color is then
	 * modelled as a per-channel mean/stddev sampled from a border band of width
	 * {@code borderFraction} of the image size. Pixels whose color deviates from
	 * that distribution by more than {@code sigmaThreshold} standard deviations
	 * (combined across channels) are marked as foreground; a 1px morphological
	 * opening removes speckle noise. A row/column is considered part of the
	 * foreground bounding box only if at least {@code minContentFraction} of its
	 * pixels are foreground. The resulting box is scaled back to the original
	 * resolution and expanded by {@code marginFraction} of the width/height.
	 *
	 * @return {@code {minX, minY, maxX, maxY}} (maxX/maxY exclusive) in the
	 *         original image's coordinates, or {@code null} if no foreground was
	 *         found.
	 */
	public static int[] foregroundBoundingBox(int[] argb, int w, int h, double sigmaThreshold,
			double minContentFraction, double borderFraction, double marginFraction) {
		if (w <= 0 || h <= 0)
			return null;

		final int maxDim = 200;
		int dw = w, dh = h;
		int[] work = argb;
		if (Math.max(w, h) > maxDim) {
			double s = (double) maxDim / Math.max(w, h);
			dw = Math.max(1, (int) Math.round(w * s));
			dh = Math.max(1, (int) Math.round(h * s));
			work = scale(argb, w, h, dw, dh);
		}

		int bx = Math.max(1, (int) Math.round(dw * borderFraction));
		int by = Math.max(1, (int) Math.round(dh * borderFraction));
		double sumR = 0, sumG = 0, sumB = 0, sumR2 = 0, sumG2 = 0, sumB2 = 0;
		int count = 0;
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				if (x >= bx && x < dw - bx && y >= by && y < dh - by)
					continue;
				int p = work[y * dw + x];
				int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
				sumR += r;
				sumG += g;
				sumB += b;
				sumR2 += (double) r * r;
				sumG2 += (double) g * g;
				sumB2 += (double) b * b;
				count++;
			}
		}
		if (count == 0)
			return null;
		double meanR = sumR / count, meanG = sumG / count, meanB = sumB / count;
		final double stdFloor = 4.0;
		double stdR = Math.max(stdFloor, Math.sqrt(Math.max(0, sumR2 / count - meanR * meanR)));
		double stdG = Math.max(stdFloor, Math.sqrt(Math.max(0, sumG2 / count - meanG * meanG)));
		double stdB = Math.max(stdFloor, Math.sqrt(Math.max(0, sumB2 / count - meanB * meanB)));

		int[] mask = new int[dw * dh];
		for (int i = 0; i < work.length; i++) {
			int p = work[i];
			double dr = ((p >>> 16) & 0xFF) - meanR;
			double dg = ((p >>> 8) & 0xFF) - meanG;
			double db = (p & 0xFF) - meanB;
			double z = Math.sqrt((dr / stdR) * (dr / stdR) + (dg / stdG) * (dg / stdG) + (db / stdB) * (db / stdB));
			mask[i] = (z > sigmaThreshold) ? 255 : 0;
		}
		mask = dilateAlpha(erodeAlpha(mask, dw, dh, 1), dw, dh, 1);

		int minRowPixels = Math.max(1, (int) Math.ceil(dw * minContentFraction));
		int minColPixels = Math.max(1, (int) Math.ceil(dh * minContentFraction));

		int minX = -1, maxX = -1, minY = -1, maxY = -1;
		for (int y = 0; y < dh; y++) {
			int c = 0;
			for (int x = 0; x < dw; x++)
				if (mask[y * dw + x] != 0)
					c++;
			if (c >= minRowPixels) {
				if (minY < 0)
					minY = y;
				maxY = y;
			}
		}
		for (int x = 0; x < dw; x++) {
			int c = 0;
			for (int y = 0; y < dh; y++)
				if (mask[y * dw + x] != 0)
					c++;
			if (c >= minColPixels) {
				if (minX < 0)
					minX = x;
				maxX = x;
			}
		}
		if (minX < 0 || minY < 0 || minX >= maxX || minY >= maxY)
			return null;

		double sx = (double) w / dw, sy = (double) h / dh;
		int origMinX = (int) Math.floor(minX * sx);
		int origMinY = (int) Math.floor(minY * sy);
		int origMaxX = (int) Math.ceil((maxX + 1) * sx);
		int origMaxY = (int) Math.ceil((maxY + 1) * sy);

		int marginX = (int) Math.round(w * marginFraction);
		int marginY = (int) Math.round(h * marginFraction);
		origMinX = Math.max(0, origMinX - marginX);
		origMinY = Math.max(0, origMinY - marginY);
		origMaxX = Math.min(w, origMaxX + marginX);
		origMaxY = Math.min(h, origMaxY + marginY);

		if (origMinX >= origMaxX || origMinY >= origMaxY)
			return null;
		return new int[] { origMinX, origMinY, origMaxX, origMaxY };
	}

	private static int[] colorizeMaxAlpha(int[] alphaA, int[] alphaB, int argbColor) {
		int r = (argbColor >>> 16) & 0xFF;
		int g = (argbColor >>> 8) & 0xFF;
		int b = argbColor & 0xFF;
		int[] dst = new int[alphaA.length];
		for (int i = 0; i < dst.length; i++) {
			int a = Math.max(alphaA[i], alphaB[i]);
			dst[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		return dst;
	}

	// ---- Filter helpers --------------------------------------------------

	/**
	 * Returns the alpha channel, binarising to 0/255 when {@code filled} is true.
	 */
	public static int[] silhouetteAlpha(int[] src, boolean filled) {
		int[] alpha = new int[src.length];
		for (int i = 0; i < src.length; i++) {
			int a = (src[i] >>> 24) & 0xFF;
			alpha[i] = (filled && a > 0) ? 255 : a;
		}
		return alpha;
	}

	/**
	 * Circular morphological dilation of an alpha channel by {@code radius} pixels.
	 */
	public static int[] dilateAlpha(int[] alpha, int w, int h, int radius) {
		int[] out = new int[w * h];
		int r2 = radius * radius;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int maxA = 0;
				for (int dy = -radius; dy <= radius && maxA < 255; dy++) {
					int ny = y + dy;
					if (ny < 0 || ny >= h)
						continue;
					for (int dx = -radius; dx <= radius && maxA < 255; dx++) {
						if (dx * dx + dy * dy > r2)
							continue;
						int nx = x + dx;
						if (nx < 0 || nx >= w)
							continue;
						int a = alpha[ny * w + nx];
						if (a > maxA)
							maxA = a;
					}
				}
				out[y * w + x] = maxA;
			}
		}
		return out;
	}

	/**
	 * Circular morphological erosion of an alpha channel by {@code radius} pixels.
	 * Pixels outside the canvas are treated as 0 (background), so the foreground
	 * shrinks away from the canvas edges too.
	 */
	public static int[] erodeAlpha(int[] alpha, int w, int h, int radius) {
		int[] out = new int[w * h];
		int r2 = radius * radius;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int minA = 255;
				for (int dy = -radius; dy <= radius && minA > 0; dy++) {
					int ny = y + dy;
					for (int dx = -radius; dx <= radius && minA > 0; dx++) {
						if (dx * dx + dy * dy > r2)
							continue;
						int nx = x + dx;
						int a = (nx < 0 || nx >= w || ny < 0 || ny >= h) ? 0 : alpha[ny * w + nx];
						if (a < minA)
							minA = a;
					}
				}
				out[y * w + x] = minA;
			}
		}
		return out;
	}

	/** Separable two-pass Gaussian blur of an alpha channel. */
	public static int[] gaussianBlurAlpha(int[] alpha, int w, int h, double sigma) {
		int radius = (int) Math.ceil(sigma * 3);
		if (radius <= 0)
			return alpha.clone();

		double[] kernel = new double[2 * radius + 1];
		double twoSigmaSq = 2 * sigma * sigma;
		for (int i = -radius; i <= radius; i++)
			kernel[i + radius] = Math.exp(-(double) i * i / twoSigmaSq);

		int[] tmp = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double sum = 0, wt = 0;
				for (int k = -radius; k <= radius; k++) {
					int nx = x + k;
					if (nx < 0 || nx >= w)
						continue;
					double kw = kernel[k + radius];
					sum += alpha[y * w + nx] * kw;
					wt += kw;
				}
				tmp[y * w + x] = (int) Math.round(sum / wt);
			}
		}

		int[] out = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double sum = 0, wt = 0;
				for (int k = -radius; k <= radius; k++) {
					int ny = y + k;
					if (ny < 0 || ny >= h)
						continue;
					double kw = kernel[k + radius];
					sum += tmp[ny * w + x] * kw;
					wt += kw;
				}
				out[y * w + x] = (int) Math.round(sum / wt);
			}
		}
		return out;
	}

	/**
	 * Premultiplied-alpha bilinear sample at fractional ({@code x}, {@code y}); 0
	 * (transparent) outside bounds.
	 */
	public static int bilinearSample(int[] src, int srcW, int srcH, double x, double y) {
		if (x < 0 || y < 0 || x > srcW - 1 || y > srcH - 1)
			return 0;
		int x0 = (int) x, y0 = (int) y;
		int x1 = Math.min(x0 + 1, srcW - 1);
		int y1 = Math.min(y0 + 1, srcH - 1);
		double tx = x - x0, ty = y - y0;

		int p00 = src[y0 * srcW + x0], p10 = src[y0 * srcW + x1];
		int p01 = src[y1 * srcW + x0], p11 = src[y1 * srcW + x1];

		// Blend in premultiplied-alpha space to avoid colour bleed at transparent edges
		int a00 = (p00 >>> 24) & 0xFF, a10 = (p10 >>> 24) & 0xFF;
		int a01 = (p01 >>> 24) & 0xFF, a11 = (p11 >>> 24) & 0xFF;
		int outA = clamp255((int) Math.round(bilerp(a00, a10, a01, a11, tx, ty)));

		if (outA == 0)
			return 0;

		double pm00 = a00 / 255.0, pm10 = a10 / 255.0, pm01 = a01 / 255.0, pm11 = a11 / 255.0;
		int outR = clamp255((int) Math.round(bilerp((p00 >> 16 & 0xFF) * pm00, (p10 >> 16 & 0xFF) * pm10,
				(p01 >> 16 & 0xFF) * pm01, (p11 >> 16 & 0xFF) * pm11, tx, ty) / (outA / 255.0)));
		int outG = clamp255((int) Math.round(bilerp((p00 >> 8 & 0xFF) * pm00, (p10 >> 8 & 0xFF) * pm10,
				(p01 >> 8 & 0xFF) * pm01, (p11 >> 8 & 0xFF) * pm11, tx, ty) / (outA / 255.0)));
		int outB = clamp255((int) Math.round(
				bilerp((p00 & 0xFF) * pm00, (p10 & 0xFF) * pm10, (p01 & 0xFF) * pm01, (p11 & 0xFF) * pm11, tx, ty)
						/ (outA / 255.0)));
		return (outA << 24) | (outR << 16) | (outG << 8) | outB;
	}

	public static double bilerp(double v00, double v10, double v01, double v11, double tx, double ty) {
		return v00 * (1 - tx) * (1 - ty) + v10 * tx * (1 - ty) + v01 * (1 - tx) * ty + v11 * tx * ty;
	}

	public static int clamp255(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}
}
