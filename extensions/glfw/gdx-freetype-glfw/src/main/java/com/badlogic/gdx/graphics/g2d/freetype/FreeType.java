/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.*;
import com.github.xpenatan.gdx.teavm.backends.shared.utils.BufferAddressUtils;
import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FreeType {
	/**
	 * GLFW/TeaVM native freetype bridge skeleton.
	 *
	 * This class was originally based on libGDX's JNI-backed freetype wrapper.
	 * For TeaVM GLFW we need to route native calls through TeaVM interop instead
	 * of SharedLibraryLoader + JNI. The implementation below starts by replacing
	 * the loading/address bridge while preserving the original Java API shape,
	 * so existing FreeTypeFontGenerator logic can stay mostly untouched.
	 */
	private static final class NativeBridge {
		private NativeBridge() {
		}

		@Import(name = "gdx2d_freetype_init")
		private static native Address initFreeType();

		@Import(name = "gdx2d_freetype_done")
		private static native void doneFreeType(Address library);

		@Import(name = "gdx2d_freetype_done_face")
		private static native void doneFace(Address face);

		@Import(name = "gdx2d_freetype_new_memory_face")
		private static native Address newMemoryFace(Address library, Address data, int dataSize, int faceIndex);

		@Import(name = "gdx2d_freetype_stroker_new")
		private static native Address strokerNew(Address library);

		@Import(name = "gdx2d_freetype_face_set_pixel_sizes")
		private static native boolean setPixelSizes(Address face, int pixelWidth, int pixelHeight);

		@Import(name = "gdx2d_freetype_face_select_size")
		private static native boolean selectSize(Address face, int strikeIndex);

		@Import(name = "gdx2d_freetype_face_set_char_size")
		private static native boolean setCharSize(Address face, int charWidth, int charHeight,
			int horzResolution, int vertResolution);

		@Import(name = "gdx2d_freetype_face_load_char")
		private static native boolean loadChar(Address face, int charCode, int loadFlags);

		@Import(name = "gdx2d_freetype_face_load_glyph")
		private static native boolean loadGlyph(Address face, int glyphIndex, int loadFlags);

		@Import(name = "gdx2d_freetype_face_get_glyph")
		private static native Address getGlyph(Address face);

		@Import(name = "gdx2d_freetype_glyphslot_get_glyph")
		private static native Address getGlyphObject(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_render_glyph")
		private static native boolean renderGlyph(Address glyphSlot, int renderMode);

		@Import(name = "gdx2d_freetype_glyphslot_get_bitmap")
		private static native Address getGlyphSlotBitmap(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyph_get_bitmap")
		private static native Address getGlyphBitmap(Address glyph);

		@Import(name = "gdx2d_freetype_glyph_to_bitmap")
		private static native Address glyphToBitmap(Address glyph, int renderMode);

		@Import(name = "gdx2d_freetype_glyph_get_left")
		private static native int getGlyphLeft(Address glyph);

		@Import(name = "gdx2d_freetype_glyph_get_top")
		private static native int getGlyphTop(Address glyph);

		@Import(name = "gdx2d_freetype_glyph_done")
		private static native void doneGlyph(Address glyph);

		@Import(name = "gdx2d_freetype_glyph_stroke_border")
		private static native Address strokeBorder(Address glyph, Address stroker, boolean inside);

		@Import(name = "gdx2d_freetype_bitmap_get_buffer")
		private static native Address getBitmapBuffer(Address bitmap);

		@Import(name = "gdx2d_freetype_face_get_face_flags")
		private static native int getFaceFlags(Address face);

		@Import(name = "gdx2d_freetype_face_get_num_glyphs")
		private static native int getNumGlyphs(Address face);

		@Import(name = "gdx2d_freetype_face_get_style_flags")
		private static native int getStyleFlags(Address face);

		@Import(name = "gdx2d_freetype_face_get_ascender")
		private static native int getAscender(Address face);

		@Import(name = "gdx2d_freetype_face_get_descender")
		private static native int getDescender(Address face);

		@Import(name = "gdx2d_freetype_face_get_height")
		private static native int getHeight(Address face);

		@Import(name = "gdx2d_freetype_face_get_max_advance_width")
		private static native int getMaxAdvanceWidth(Address face);

		@Import(name = "gdx2d_freetype_face_get_max_advance_height")
		private static native int getMaxAdvanceHeight(Address face);

		@Import(name = "gdx2d_freetype_face_get_underline_position")
		private static native int getUnderlinePosition(Address face);

		@Import(name = "gdx2d_freetype_face_get_underline_thickness")
		private static native int getUnderlineThickness(Address face);

		@Import(name = "gdx2d_freetype_face_get_size")
		private static native Address getSize(Address face);

		@Import(name = "gdx2d_freetype_face_get_char_index")
		private static native int getCharIndex(Address face, int charCode);

		@Import(name = "gdx2d_freetype_face_has_kerning")
		private static native boolean hasKerning(Address face);

		@Import(name = "gdx2d_freetype_face_get_kerning")
		private static native int getKerning(Address face, int leftGlyph, int rightGlyph, int kernMode);

		@Import(name = "gdx2d_freetype_size_get_metrics")
		private static native Address getSizeMetrics(Address size);

		@Import(name = "gdx2d_freetype_size_metrics_get_ascender")
		private static native int getSizeMetricsAscender(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_descender")
		private static native int getSizeMetricsDescender(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_height")
		private static native int getSizeMetricsHeight(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_max_advance")
		private static native int getSizeMetricsMaxAdvance(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_x_ppem")
		private static native int getSizeMetricsXppem(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_y_ppem")
		private static native int getSizeMetricsYppem(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_x_scale")
		private static native int getSizeMetricsXScale(Address metrics);

		@Import(name = "gdx2d_freetype_size_metrics_get_y_scale")
		private static native int getSizeMetricsYScale(Address metrics);

		@Import(name = "gdx2d_freetype_glyphslot_get_metrics")
		private static native Address getGlyphSlotMetrics(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_format")
		private static native int getGlyphSlotFormat(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_bitmap_left")
		private static native int getGlyphSlotBitmapLeft(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_bitmap_top")
		private static native int getGlyphSlotBitmapTop(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_linear_hori_advance")
		private static native int getGlyphSlotLinearHoriAdvance(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_linear_vert_advance")
		private static native int getGlyphSlotLinearVertAdvance(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_advance_x")
		private static native int getGlyphSlotAdvanceX(Address glyphSlot);

		@Import(name = "gdx2d_freetype_glyphslot_get_advance_y")
		private static native int getGlyphSlotAdvanceY(Address glyphSlot);

		@Import(name = "gdx2d_freetype_bitmap_get_rows")
		private static native int getBitmapRows(Address bitmap);

		@Import(name = "gdx2d_freetype_bitmap_get_width")
		private static native int getBitmapWidth(Address bitmap);

		@Import(name = "gdx2d_freetype_bitmap_get_pitch")
		private static native int getBitmapPitch(Address bitmap);

		@Import(name = "gdx2d_freetype_bitmap_get_num_gray")
		private static native int getBitmapNumGray(Address bitmap);

		@Import(name = "gdx2d_freetype_bitmap_get_pixel_mode")
		private static native int getBitmapPixelMode(Address bitmap);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_width")
		private static native int getGlyphMetricsWidth(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_height")
		private static native int getGlyphMetricsHeight(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_hori_advance")
		private static native int getGlyphMetricsHoriAdvance(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_hori_bearing_x")
		private static native int getGlyphMetricsHoriBearingX(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_hori_bearing_y")
		private static native int getGlyphMetricsHoriBearingY(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_vert_bearing_x")
		private static native int getGlyphMetricsVertBearingX(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_vert_bearing_y")
		private static native int getGlyphMetricsVertBearingY(Address metrics);

		@Import(name = "gdx2d_freetype_glyph_metrics_get_vert_advance")
		private static native int getGlyphMetricsVertAdvance(Address metrics);

		@Import(name = "gdx2d_freetype_stroker_set")
		private static native void setStroker(Address stroker, int radius, int lineCap, int lineJoin, int miterLimit);

		@Import(name = "gdx2d_freetype_stroker_done")
		private static native void doneStroker(Address stroker);

		@Import(name = "gdx2d_freetype_address_is_null")
		@NoSideEffects
		private static native boolean isNull(Address address);

		@Import(name = "gdx2d_freetype_get_last_error")
		@NoSideEffects
		private static native int getLastErrorCode();
	}

	private static final class NativeBytes {
		private NativeBytes() {
		}

		static Address of(ByteBuffer buffer) {
			return BufferAddressUtils.ofInternal(buffer);
		}
	}
	
	/**
	 * 
	 * @return returns the last error code FreeType reported
	 */
	static int getLastErrorCode() {
		return NativeBridge.getLastErrorCode();
	}
	
	private static class Pointer {
		long address;
		
		Pointer(long address) {
			this.address = address;
		}

		Address asAddress() {
			return Address.fromLong(address);
		}
	}
	
	public static class Library extends Pointer implements Disposable {
		LongMap<ByteBuffer> fontData = new LongMap<ByteBuffer>();
		
		Library (long address) {
			super(address);
		}

		@Override
		public void dispose () {
			doneFreeType(address);
			for(ByteBuffer buffer: fontData.values()) {
				disposeByteBuffer(buffer);
			}
		}

		private static void doneFreeType(long library) {
			if (library == 0L) {
				return;
			}
			NativeBridge.doneFreeType(Address.fromLong(library));
		}

		public Face newFace(FileHandle fontFile, int faceIndex) {
			InputStream input = fontFile.read();
			try {
				byte[] data = StreamUtils.copyStreamToByteArray(input, 1024 * 16);
				return newMemoryFace(ByteBuffer.wrap(data), faceIndex);
			} catch (IOException ex) {
				throw new GdxRuntimeException(ex);
			} finally {
				StreamUtils.closeQuietly(input);
			}
		}

		public Face newMemoryFace(byte[] data, int dataSize, int faceIndex) {
			ByteBuffer buffer = ByteBuffer.wrap(data, 0, dataSize).slice();
			return newMemoryFace(buffer, faceIndex);
		}

		public Face newMemoryFace(ByteBuffer buffer, int faceIndex) {
			long face = newMemoryFace(address, buffer, buffer.remaining(), faceIndex);
			if(face == 0) {
				disposeByteBuffer(buffer);
				throw new GdxRuntimeException("Couldn't load font, FreeType error code: " + getLastErrorCode());
			}
			else {
				fontData.put(face, buffer);
				return new Face(face, this);
			}
		}

		private static long newMemoryFace(long library, ByteBuffer data, int dataSize, int faceIndex) {
			if (library == 0L || data == null) {
				return 0L;
			}
			Address face = NativeBridge.newMemoryFace(Address.fromLong(library), NativeBytes.of(data), dataSize, faceIndex);
			return addressToLong(face);
		}

		public Stroker createStroker() {
			long stroker = strokerNew(address);
			if(stroker == 0) throw new GdxRuntimeException("Couldn't create FreeType stroker, FreeType error code: " + getLastErrorCode());
			return new Stroker(stroker);
		}

		private static long strokerNew(long library) {
			if (library == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.strokerNew(Address.fromLong(library)));
		}
	}
	
	public static class Face extends Pointer implements Disposable {
		Library library;
		
		public Face (long address, Library library) {
			super(address);
			this.library = library;
		}
		
		@Override
		public void dispose() {
			doneFace(address);
			ByteBuffer buffer = library.fontData.get(address);
			if(buffer != null) {
				library.fontData.remove(address);
				disposeByteBuffer(buffer);
			}
		}

		private static void doneFace(long face) {
			if (face == 0L) {
				return;
			}
			NativeBridge.doneFace(Address.fromLong(face));
		}

		public int getFaceFlags() {
			return getFaceFlags(address);
		}
		
		private static int getFaceFlags(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getFaceFlags(Address.fromLong(face));
		}
		
		public int getStyleFlags() {
			return getStyleFlags(address);
		}
		
		private static int getStyleFlags(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getStyleFlags(Address.fromLong(face));
		}
		
		public int getNumGlyphs() {
			return getNumGlyphs(address);
		}
		
		private static int getNumGlyphs(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getNumGlyphs(Address.fromLong(face));
		}
		
		public int getAscender() {
			return getAscender(address);
		}
		
		private static int getAscender(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getAscender(Address.fromLong(face));
		}
		
		public int getDescender() {
			return getDescender(address);
		}
		
		private static int getDescender(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getDescender(Address.fromLong(face));
		}
		
		public int getHeight() {
			return getHeight(address);
		}
		
		private static int getHeight(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getHeight(Address.fromLong(face));
		}
		
		public int getMaxAdvanceWidth() {
			return getMaxAdvanceWidth(address);
		}
		
		private static int getMaxAdvanceWidth(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getMaxAdvanceWidth(Address.fromLong(face));
		}
		
		public int getMaxAdvanceHeight() {
			return getMaxAdvanceHeight(address);
		}
		
		private static int getMaxAdvanceHeight(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getMaxAdvanceHeight(Address.fromLong(face));
		}
		
		public int getUnderlinePosition() {
			return getUnderlinePosition(address);
		}
		
		private static int getUnderlinePosition(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getUnderlinePosition(Address.fromLong(face));
		}
		
		public int getUnderlineThickness() {
			return getUnderlineThickness(address);
		}
		
		private static int getUnderlineThickness(long face) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getUnderlineThickness(Address.fromLong(face));
		}
		
		public boolean selectSize(int strikeIndex) {
			return selectSize(address, strikeIndex);
		}

		private static boolean selectSize(long face, int strikeIndex) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.selectSize(Address.fromLong(face), strikeIndex);
		}

		public boolean setCharSize(int charWidth, int charHeight, int horzResolution, int vertResolution) {
			return setCharSize(address, charWidth, charHeight, horzResolution, vertResolution);
		}

		private static boolean setCharSize(long face, int charWidth, int charHeight, int horzResolution, int vertResolution) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.setCharSize(Address.fromLong(face), charWidth, charHeight, horzResolution, vertResolution);
		}

		public boolean setPixelSizes(int pixelWidth, int pixelHeight) {
			return setPixelSizes(address, pixelWidth, pixelHeight);
		}

		private static boolean setPixelSizes(long face, int pixelWidth, int pixelHeight) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.setPixelSizes(Address.fromLong(face), pixelWidth, pixelHeight);
		}

		public boolean loadGlyph(int glyphIndex, int loadFlags) {
			return loadGlyph(address, glyphIndex, loadFlags);
		}

		private static boolean loadGlyph(long face, int glyphIndex, int loadFlags) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.loadGlyph(Address.fromLong(face), glyphIndex, loadFlags);
		}

		public boolean loadChar(int charCode, int loadFlags) {
			return loadChar(address, charCode, loadFlags);
		}

		private static boolean loadChar(long face, int charCode, int loadFlags) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.loadChar(Address.fromLong(face), charCode, loadFlags);
		}

		public GlyphSlot getGlyph() {
			return new GlyphSlot(getGlyph(address));
		}
		
		private static long getGlyph(long face) {
			if (face == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getGlyph(Address.fromLong(face)));
		}
		
		public Size getSize() {
			return new Size(getSize(address));
		}
		
		private static long getSize(long face) {
			if (face == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getSize(Address.fromLong(face)));
		}

		public boolean hasKerning() {
			return hasKerning(address);
		}

		private static boolean hasKerning(long face) {
			if (face == 0L) {
				return false;
			}
			return NativeBridge.hasKerning(Address.fromLong(face));
		}

		public int getKerning(int leftGlyph, int rightGlyph, int kernMode) {
			return getKerning(address, leftGlyph, rightGlyph, kernMode);
		}

		private static int getKerning(long face, int leftGlyph, int rightGlyph, int kernMode) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getKerning(Address.fromLong(face), leftGlyph, rightGlyph, kernMode);
		}

		public int getCharIndex(int charCode) {
			return getCharIndex(address, charCode);
		}

		private static int getCharIndex(long face, int charCode) {
			if (face == 0L) {
				return 0;
			}
			return NativeBridge.getCharIndex(Address.fromLong(face), charCode);
		}

	}
	
	public static class Size extends Pointer {
		Size (long address) {
			super(address);
		}
		
		public SizeMetrics getMetrics() {
			return new SizeMetrics(getMetrics(address));
		}
		
		private static long getMetrics(long address) {
			if (address == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getSizeMetrics(Address.fromLong(address)));
		}
	}
	
	public static class SizeMetrics extends Pointer {
		SizeMetrics (long address) {
			super(address);
		}
		
		public int getXppem() {
			return getXppem(address);
		}
		
		private static int getXppem(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsXppem(Address.fromLong(metrics));
		}
		
		public int getYppem() {
			return getYppem(address);
		}
		
		private static int getYppem(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsYppem(Address.fromLong(metrics));
		}
		
		public int getXScale() {
			return getXscale(address);
		}
		
		private static int getXscale(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsXScale(Address.fromLong(metrics));
		}
		
		public int getYscale() {
			return getYscale(address);
		}
		
		private static int getYscale(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsYScale(Address.fromLong(metrics));
		}
		
		public int getAscender() {
			return getAscender(address);
		}
		
		private static int getAscender(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsAscender(Address.fromLong(metrics));
		}
		
		public int getDescender() {
			return getDescender(address);
		}
		
		private static int getDescender(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsDescender(Address.fromLong(metrics));
		}
		
		public int getHeight() {
			return getHeight(address);
		}
		
		private static int getHeight(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsHeight(Address.fromLong(metrics));
		}
		
		public int getMaxAdvance() {
			return getMaxAdvance(address);
		}
		
		private static int getMaxAdvance(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getSizeMetricsMaxAdvance(Address.fromLong(metrics));
		}
	}
	
	public static class GlyphSlot extends Pointer {
		GlyphSlot (long address) {
			super(address);
		}
		
		public GlyphMetrics getMetrics() {
			return new GlyphMetrics(getMetrics(address));
		}		
		
		private static long getMetrics(long slot) {
			if (slot == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getGlyphSlotMetrics(Address.fromLong(slot)));
		}
		
		public int getLinearHoriAdvance() {
			return getLinearHoriAdvance(address);
		}
		
		private static int getLinearHoriAdvance(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotLinearHoriAdvance(Address.fromLong(slot));
		}
		
		public int getLinearVertAdvance() {
			return getLinearVertAdvance(address);
		}
		
		private static int getLinearVertAdvance(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotLinearVertAdvance(Address.fromLong(slot));
		}
		
		public int getAdvanceX() {
			return getAdvanceX(address);
		}
		
		private static int getAdvanceX(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotAdvanceX(Address.fromLong(slot));
		}
		
		public int getAdvanceY() {
			return getAdvanceY(address);
		}
		
		private static int getAdvanceY(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotAdvanceY(Address.fromLong(slot));
		}
		
		public int getFormat() {
			return getFormat(address);
		}
		
		private static int getFormat(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotFormat(Address.fromLong(slot));
		}
		
		public Bitmap getBitmap() {
			return new Bitmap(getBitmap(address));
		}
		
		private static long getBitmap(long slot) {
			if (slot == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getGlyphSlotBitmap(Address.fromLong(slot)));
		}
		
		public int getBitmapLeft() {
			return getBitmapLeft(address);
		}
		
		private static int getBitmapLeft(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotBitmapLeft(Address.fromLong(slot));
		}
		
		public int getBitmapTop() {
			return getBitmapTop(address);
		}
		
		private static int getBitmapTop(long slot) {
			if (slot == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphSlotBitmapTop(Address.fromLong(slot));
		}

		public boolean renderGlyph(int renderMode) {
			return renderGlyph(address, renderMode);
		}

		private static boolean renderGlyph(long slot, int renderMode) {
			if (slot == 0L) {
				return false;
			}
			return NativeBridge.renderGlyph(Address.fromLong(slot), renderMode);
		}

		public Glyph getGlyph() {
			long glyph = getGlyph(address);
			if(glyph == 0) throw new GdxRuntimeException("Couldn't get glyph, FreeType error code: " + getLastErrorCode());
			return new Glyph(glyph);
		}

		private static long getGlyph(long glyphSlot) {
			if (glyphSlot == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getGlyphObject(Address.fromLong(glyphSlot)));
		}
	}
	
	public static class Glyph extends Pointer implements Disposable {
		private boolean rendered;

		Glyph (long address) {
			super(address);
		}

		@Override
		public void dispose () {
			done(address);
		}

		private static void done(long glyph) {
			if (glyph == 0L) {
				return;
			}
			NativeBridge.doneGlyph(Address.fromLong(glyph));
		}

		public void strokeBorder(Stroker stroker, boolean inside) {
			address = strokeBorder(address, stroker.address, inside);
		}

		private static long strokeBorder(long glyph, long stroker, boolean inside) {
			if (glyph == 0L || stroker == 0L) {
				return glyph;
			}
			return addressToLong(NativeBridge.strokeBorder(Address.fromLong(glyph), Address.fromLong(stroker), inside));
		}

		public void toBitmap(int renderMode) {
			long bitmap = toBitmap(address, renderMode);
			if (bitmap == 0) throw new GdxRuntimeException("Couldn't render glyph, FreeType error code: " + getLastErrorCode());
			address = bitmap;
			rendered = true;
		}

		private static long toBitmap(long glyph, int renderMode) {
			if (glyph == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.glyphToBitmap(Address.fromLong(glyph), renderMode));
		}

		public Bitmap getBitmap() {
			if (!rendered) {
				throw new GdxRuntimeException("Glyph is not yet rendered");
			}
			return new Bitmap(getBitmap(address));
		}

		private static long getBitmap(long glyph) {
			if (glyph == 0L) {
				return 0L;
			}
			return addressToLong(NativeBridge.getGlyphBitmap(Address.fromLong(glyph)));
		}

		public int getLeft() {
			if (!rendered) {
				throw new GdxRuntimeException("Glyph is not yet rendered");
			}
			return getLeft(address);
		}

		private static int getLeft(long glyph) {
			if (glyph == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphLeft(Address.fromLong(glyph));
		}

		public int getTop() {
			if (!rendered) {
				throw new GdxRuntimeException("Glyph is not yet rendered");
			}
			return getTop(address);
		}

		private static int getTop(long glyph) {
			if (glyph == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphTop(Address.fromLong(glyph));
		}

	}

	public static class Bitmap extends Pointer {
		Bitmap (long address) {
			super(address);
		}
		
		public int getRows() {
			return getRows(address);
		}
		
		private static int getRows(long bitmap) {
			if (bitmap == 0L) {
				return 0;
			}
			return NativeBridge.getBitmapRows(Address.fromLong(bitmap));
		}
		
		public int getWidth() {
			return getWidth(address);
		}
		
		private static int getWidth(long bitmap) {
			if (bitmap == 0L) {
				return 0;
			}
			return NativeBridge.getBitmapWidth(Address.fromLong(bitmap));
		}
		
		public int getPitch() {
			return getPitch(address);
		}
		
		private static int getPitch(long bitmap) {
			if (bitmap == 0L) {
				return 0;
			}
			return NativeBridge.getBitmapPitch(Address.fromLong(bitmap));
		}
		
		public ByteBuffer getBuffer() {
			if (getRows() == 0)
				// Issue #768 - CheckJNI frowns upon env->NewDirectByteBuffer with NULL buffer or capacity 0
				//                  "JNI WARNING: invalid values for address (0x0) or capacity (0)"
				//              FreeType sets FT_Bitmap::buffer to NULL when the bitmap is empty (e.g. for ' ')
				//              JNICheck is on by default on emulators and might have a point anyway...
				//              So let's avoid this and just return a dummy non-null non-zero buffer
				return BufferUtils.newByteBuffer(1);
			return getBuffer(address);
		}

		private static ByteBuffer getBuffer(long bitmap) {
			if (bitmap == 0L) {
				return BufferUtils.newByteBuffer(1);
			}
			int size = Math.max(1, getRows(bitmap) * Math.abs(getPitch(bitmap)));
			ByteBuffer buffer = BufferUtils.newByteBuffer(size);
			Address src = NativeBridge.getBitmapBuffer(Address.fromLong(bitmap));
			if (src == null || NativeBridge.isNull(src)) {
				return buffer;
			}
			for (int i = 0; i < size; i++) {
				buffer.put(i, src.add(i).getByte());
			}
			return buffer;
		}

		// @on
		public Pixmap getPixmap (Format format, Color color, float gamma) {
			int width = getWidth(), rows = getRows();
			ByteBuffer src = getBuffer();
			Pixmap pixmap;
			int pixelMode = getPixelMode();
			int rowBytes = Math.abs(getPitch()); // We currently ignore negative pitch.
			if (color == Color.WHITE && pixelMode == FT_PIXEL_MODE_GRAY && rowBytes == width && gamma == 1) {
				pixmap = new Pixmap(width, rows, Format.Alpha);
				BufferUtils.copy(src, pixmap.getPixels(), pixmap.getPixels().capacity());
			} else {
				pixmap = new Pixmap(width, rows, Format.RGBA8888);
				int rgba = Color.rgba8888(color);
				byte[] srcRow = new byte[rowBytes];
				int[] dstRow = new int[width];
				IntBuffer dst = pixmap.getPixels().asIntBuffer();
				if (pixelMode == FT_PIXEL_MODE_MONO) {
					// Use the specified color for each set bit.
					for (int y = 0; y < rows; y++) {
						src.get(srcRow);
						for (int i = 0, x = 0; x < width; i++, x += 8) {
							byte b = srcRow[i];
							for (int ii = 0, n = Math.min(8, width - x); ii < n; ii++) {
								if ((b & (1 << (7 - ii))) != 0)
									dstRow[x + ii] = rgba;
								else
									dstRow[x + ii] = 0;
							}
						}
						dst.put(dstRow);
					}
				} else {
					// Use the specified color for RGB, blend the FreeType bitmap with alpha.
					int rgb = rgba & 0xffffff00;
					int a = rgba & 0xff;
					for (int y = 0; y < rows; y++) {
						src.get(srcRow);
						for (int x = 0; x < width; x++) {
							// Zero raised to any power is always zero.
							// 255 (=one) raised to any power is always one.
							// We only need Math.pow() when alpha is NOT zero and NOT one.
							int alpha = srcRow[x] & 0xff;
							if (alpha == 0)
								dstRow[x] = rgb;
							else if (alpha == 255)
								dstRow[x] = rgb | a;
							else
								dstRow[x] = rgb | (int)(a * (float)Math.pow(alpha / 255f, gamma)); // Inverse gamma.
						}
						dst.put(dstRow);
					}
				}
			}

			Pixmap converted = pixmap;
			if (format != pixmap.getFormat()) {
				converted = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), format);
				converted.setBlending(Blending.None);
				converted.drawPixmap(pixmap, 0, 0);
				converted.setBlending(Blending.SourceOver);
				pixmap.dispose();
			}
			return converted;
		}
		// @off

		public int getNumGray() {
			return getNumGray(address);
		}
		
		private static int getNumGray(long bitmap) {
			if (bitmap == 0L) {
				return 0;
			}
			return NativeBridge.getBitmapNumGray(Address.fromLong(bitmap));
		}
		
		public int getPixelMode() {
			return getPixelMode(address);
		}
		
		private static int getPixelMode(long bitmap) {
			if (bitmap == 0L) {
				return 0;
			}
			return NativeBridge.getBitmapPixelMode(Address.fromLong(bitmap));
		}
	}
	
	public static class GlyphMetrics extends Pointer {
		GlyphMetrics (long address) {
			super(address);
		}
		
		public int getWidth() {
			return getWidth(address);
		}
		
		private static int getWidth(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsWidth(Address.fromLong(metrics));
		}
		
		public int getHeight() {
			return getHeight(address);
		}
		
		private static int getHeight(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsHeight(Address.fromLong(metrics));
		}
		
		public int getHoriBearingX() {
			return getHoriBearingX(address);
		}
		
		private static int getHoriBearingX(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsHoriBearingX(Address.fromLong(metrics));
		}
		
		public int getHoriBearingY() {
			return getHoriBearingY(address);
		}
		
		private static int getHoriBearingY(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsHoriBearingY(Address.fromLong(metrics));
		}
		
		public int getHoriAdvance() {
			return getHoriAdvance(address);
		}
		
		private static int getHoriAdvance(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsHoriAdvance(Address.fromLong(metrics));
		}
	
		public int getVertBearingX() {
			return getVertBearingX(address);
		}
		
		private static int getVertBearingX(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsVertBearingX(Address.fromLong(metrics));
		}
		
		public int getVertBearingY() {
			return getVertBearingY(address);
		}
	
		private static int getVertBearingY(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsVertBearingY(Address.fromLong(metrics));
		 }
		
		public int getVertAdvance() {
			return getVertAdvance(address);
		}
	
		private static int getVertAdvance(long metrics) {
			if (metrics == 0L) {
				return 0;
			}
			return NativeBridge.getGlyphMetricsVertAdvance(Address.fromLong(metrics));
		}
	}

	public static class Stroker extends Pointer implements Disposable {
		Stroker(long address) {
			super(address);
		}

		public void set(int radius, int lineCap, int lineJoin, int miterLimit) {
			set(address, radius, lineCap, lineJoin, miterLimit);
		}

		private static void set(long stroker, int radius, int lineCap, int lineJoin, int miterLimit) {
			if (stroker == 0L) {
				return;
			}
			NativeBridge.setStroker(Address.fromLong(stroker), radius, lineCap, lineJoin, miterLimit);
		}

		@Override
		public void dispose() {
			done(address);
		}

		private static void done(long stroker) {
			if (stroker == 0L) {
				return;
			}
			NativeBridge.doneStroker(Address.fromLong(stroker));
		}
	}

   public static int FT_PIXEL_MODE_NONE = 0;
   public static int FT_PIXEL_MODE_MONO = 1;
   public static int FT_PIXEL_MODE_GRAY = 2;
   public static int FT_PIXEL_MODE_GRAY2 = 3;
   public static int FT_PIXEL_MODE_GRAY4 = 4;
   public static int FT_PIXEL_MODE_LCD = 5;
   public static int FT_PIXEL_MODE_LCD_V = 6;
	
	private static int encode (char a, char b, char c, char d) {
		return (a << 24) | (b << 16) | (c << 8) | d;
	}

	public static int FT_ENCODING_NONE = 0;
	public static int FT_ENCODING_MS_SYMBOL = encode('s', 'y', 'm', 'b');
	public static int FT_ENCODING_UNICODE = encode('u', 'n', 'i', 'c');
	public static int FT_ENCODING_SJIS = encode('s', 'j', 'i', 's');
	public static int FT_ENCODING_GB2312 = encode('g', 'b', ' ', ' ');
	public static int FT_ENCODING_BIG5 = encode('b', 'i', 'g', '5');
	public static int FT_ENCODING_WANSUNG = encode('w', 'a', 'n', 's');
	public static int FT_ENCODING_JOHAB = encode('j', 'o', 'h', 'a');
	public static int FT_ENCODING_ADOBE_STANDARD = encode('A', 'D', 'O', 'B');
	public static int FT_ENCODING_ADOBE_EXPERT = encode('A', 'D', 'B', 'E');
	public static int FT_ENCODING_ADOBE_CUSTOM = encode('A', 'D', 'B', 'C');
	public static int FT_ENCODING_ADOBE_LATIN_1 = encode('l', 'a', 't', '1');
	public static int FT_ENCODING_OLD_LATIN_2 = encode('l', 'a', 't', '2');
	public static int FT_ENCODING_APPLE_ROMAN = encode('a', 'r', 'm', 'n');
	
	public static int FT_FACE_FLAG_SCALABLE          = ( 1 <<  0 );
	public static int FT_FACE_FLAG_FIXED_SIZES       = ( 1 <<  1 );
	public static int FT_FACE_FLAG_FIXED_WIDTH       = ( 1 <<  2 );
	public static int FT_FACE_FLAG_SFNT              = ( 1 <<  3 );
	public static int FT_FACE_FLAG_HORIZONTAL        = ( 1 <<  4 );
	public static int FT_FACE_FLAG_VERTICAL          = ( 1 <<  5 );
	public static int FT_FACE_FLAG_KERNING           = ( 1 <<  6 );
	public static int FT_FACE_FLAG_FAST_GLYPHS       = ( 1 <<  7 );
	public static int FT_FACE_FLAG_MULTIPLE_MASTERS  = ( 1 <<  8 );
	public static int FT_FACE_FLAG_GLYPH_NAMES       = ( 1 <<  9 );
	public static int FT_FACE_FLAG_EXTERNAL_STREAM   = ( 1 << 10 );
	public static int FT_FACE_FLAG_HINTER            = ( 1 << 11 );
	public static int FT_FACE_FLAG_CID_KEYED         = ( 1 << 12 );
	public static int FT_FACE_FLAG_TRICKY            = ( 1 << 13 );
	
	public static int FT_STYLE_FLAG_ITALIC = ( 1 << 0 );
	public static int FT_STYLE_FLAG_BOLD   = ( 1 << 1 );
	
	public static int FT_LOAD_DEFAULT                      = 0x0;
	public static int FT_LOAD_NO_SCALE                     = 0x1;
	public static int FT_LOAD_NO_HINTING                   = 0x2;
	public static int FT_LOAD_RENDER                       = 0x4;
	public static int FT_LOAD_NO_BITMAP                    = 0x8;
	public static int FT_LOAD_VERTICAL_LAYOUT              = 0x10;
	public static int FT_LOAD_FORCE_AUTOHINT               = 0x20;
	public static int FT_LOAD_CROP_BITMAP                  = 0x40;
	public static int FT_LOAD_PEDANTIC                     = 0x80;
	public static int FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH  = 0x200;
	public static int FT_LOAD_NO_RECURSE                   = 0x400;
	public static int FT_LOAD_IGNORE_TRANSFORM             = 0x800;
	public static int FT_LOAD_MONOCHROME                   = 0x1000;
	public static int FT_LOAD_LINEAR_DESIGN                = 0x2000;
	public static int FT_LOAD_NO_AUTOHINT                  = 0x8000;
	
	public static int FT_LOAD_TARGET_NORMAL                = 0x0;
	public static int FT_LOAD_TARGET_LIGHT                 = 0x10000;
	public static int FT_LOAD_TARGET_MONO                  = 0x20000;
	public static int FT_LOAD_TARGET_LCD                   = 0x30000;
	public static int FT_LOAD_TARGET_LCD_V                 = 0x40000;

   public static int FT_RENDER_MODE_NORMAL = 0;
   public static int FT_RENDER_MODE_LIGHT = 1;
   public static int FT_RENDER_MODE_MONO = 2;
   public static int FT_RENDER_MODE_LCD = 3;
   public static int FT_RENDER_MODE_LCD_V = 4;
   public static int FT_RENDER_MODE_MAX = 5;
   
   public static int FT_KERNING_DEFAULT = 0;
   public static int FT_KERNING_UNFITTED = 1;
   public static int FT_KERNING_UNSCALED = 2;
	
	public static int FT_STROKER_LINECAP_BUTT = 0;
	public static int FT_STROKER_LINECAP_ROUND = 1;
	public static int FT_STROKER_LINECAP_SQUARE = 2;

	public static int FT_STROKER_LINEJOIN_ROUND          = 0;
	public static int FT_STROKER_LINEJOIN_BEVEL          = 1;
	public static int FT_STROKER_LINEJOIN_MITER_VARIABLE = 2;
	public static int FT_STROKER_LINEJOIN_MITER          = FT_STROKER_LINEJOIN_MITER_VARIABLE;
	public static int FT_STROKER_LINEJOIN_MITER_FIXED    = 3;

   public static Library initFreeType() {
   	long address = initFreeTypeJni();
   	if(address == 0)
   		throw new GdxRuntimeException("Couldn't initialize FreeType library, FreeType error code: " + getLastErrorCode());
   	else
   		return new Library(address);
   }
   
	private static long initFreeTypeJni() {
		return addressToLong(NativeBridge.initFreeType());
	}

	public static int toInt (int value) {
		return ((value + 63) & -64) >> 6;
	}

	private static long addressToLong(Address address) {
		if (address == null || NativeBridge.isNull(address)) {
			return 0L;
		}
		return address.toLong();
	}

	private static void disposeByteBuffer(ByteBuffer buffer) {
		// TeaVM GLFW does not expose libGDX's desktop unsafe-buffer lifecycle helpers.
		// Buffers used here are regular heap buffers or TeaVM-managed direct views, so
		// there is nothing explicit to release on this path.
	}
   
//	public static void main (String[] args) throws Exception {
//		FreetypeBuild.main(args);
//		new SharedLibraryLoader("libs/gdx-freetype-natives.jar").load("gdx-freetype");
//		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*�?�?�?�?�? ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿À�?ÂÃÄÅÆÇÈÉÊËÌ�?Î�?�?ÑÒÓÔÕÖ×ØÙÚÛÜ�?Þßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
//		
//		Library library = FreeType.initFreeType();
//		Face face = library.newFace(new FileHandle("lsans.ttf"), 0);
//		face.setPixelSizes(0, 15);
//		SizeMetrics faceMetrics = face.getSize().getMetrics();
//		System.out.println(toInt(faceMetrics.getAscender()) + ", " + toInt(faceMetrics.getDescender()) + ", " + toInt(faceMetrics.getHeight()));
//		
//		for(int i = 0; i < chars.length(); i++) {
//			if(!FreeType.loadGlyph(face, FreeType.getCharIndex(face, chars.charAt(i)), 0)) continue;
//			if(!FreeType.renderGlyph(face.getGlyph(), FT_RENDER_MODE_NORMAL)) continue;
//			Bitmap bitmap = face.getGlyph().getBitmap();
//			GlyphMetrics glyphMetrics = face.getGlyph().getMetrics();
//			System.out.println(toInt(glyphMetrics.getHoriBearingX()) + ", " + toInt(glyphMetrics.getHoriBearingY()));
//			System.out.println(toInt(glyphMetrics.getWidth()) + ", " + toInt(glyphMetrics.getHeight()) + ", " + toInt(glyphMetrics.getHoriAdvance()));
//			System.out.println(bitmap.getWidth() + ", " + bitmap.getRows() + ", " + bitmap.getPitch() + ", " + bitmap.getNumGray());
//			for(int y = 0; y < bitmap.getRows(); y++) {
//				for(int x = 0; x < bitmap.getWidth(); x++) {
//					System.out.print(bitmap.getBuffer().get(x + bitmap.getPitch() * y) != 0? "X": " ");
//				}
//				System.out.println();
//			}
//		}
//	
//		face.dispose();
//		library.dispose();
//	}
}
