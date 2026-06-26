#include <stdint.h>
#include <stdlib.h>

#include "gdx2d_freetype_bridge.h"

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_STROKER_H
#include FT_GLYPH_H

static int gdx2d_freetype_last_error = 0;

static void gdx2d_freetype_set_error(FT_Error error) {
    gdx2d_freetype_last_error = (int)error;
}

int gdx2d_freetype_get_last_error(void) {
    return gdx2d_freetype_last_error;
}

int gdx2d_freetype_address_is_null(void* address) {
    return address == NULL;
}

void* gdx2d_freetype_init(void) {
    FT_Library library = NULL;
    FT_Error error = FT_Init_FreeType(&library);
    gdx2d_freetype_set_error(error);
    if(error) {
        return NULL;
    }
    return library;
}

void gdx2d_freetype_done(void* library) {
    if(library == NULL) {
        return;
    }
    FT_Done_FreeType((FT_Library)library);
}

void gdx2d_freetype_done_face(void* face) {
    if(face == NULL) {
        return;
    }
    FT_Done_Face((FT_Face)face);
}

void* gdx2d_freetype_new_memory_face(void* library, void* data, int data_size, int face_index) {
    FT_Face face = NULL;
    FT_Error error = FT_New_Memory_Face((FT_Library)library, (const FT_Byte*)data, data_size, face_index, &face);
    gdx2d_freetype_set_error(error);
    if(error) {
        return NULL;
    }
    return face;
}

void* gdx2d_freetype_stroker_new(void* library) {
    FT_Stroker stroker = NULL;
    FT_Error error = FT_Stroker_New((FT_Library)library, &stroker);
    gdx2d_freetype_set_error(error);
    if(error) {
        return NULL;
    }
    return stroker;
}

int gdx2d_freetype_face_set_pixel_sizes(void* face, int pixel_width, int pixel_height) {
    FT_Error error = FT_Set_Pixel_Sizes((FT_Face)face, (FT_UInt)pixel_width, (FT_UInt)pixel_height);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

int gdx2d_freetype_face_select_size(void* face, int strike_index) {
    FT_Error error = FT_Select_Size((FT_Face)face, strike_index);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

int gdx2d_freetype_face_set_char_size(void* face, int char_width, int char_height,
                                      int horz_resolution, int vert_resolution) {
    FT_Error error = FT_Set_Char_Size((FT_Face)face, char_width, char_height,
                                      horz_resolution, vert_resolution);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

int gdx2d_freetype_face_load_char(void* face, int char_code, int load_flags) {
    FT_Error error = FT_Load_Char((FT_Face)face, (FT_ULong)char_code, load_flags);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

int gdx2d_freetype_face_load_glyph(void* face, int glyph_index, int load_flags) {
    FT_Error error = FT_Load_Glyph((FT_Face)face, glyph_index, load_flags);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

void* gdx2d_freetype_face_get_glyph(void* face) {
    FT_Face ft_face = (FT_Face)face;
    if(ft_face == NULL) {
        return NULL;
    }
    return ft_face->glyph;
}

void* gdx2d_freetype_glyphslot_get_glyph(void* glyph_slot) {
    FT_Glyph glyph = NULL;
    FT_Error error = FT_Get_Glyph((FT_GlyphSlot)glyph_slot, &glyph);
    gdx2d_freetype_set_error(error);
    if(error) {
        return NULL;
    }
    return glyph;
}

int gdx2d_freetype_glyphslot_render_glyph(void* glyph_slot, int render_mode) {
    FT_Error error = FT_Render_Glyph((FT_GlyphSlot)glyph_slot, (FT_Render_Mode)render_mode);
    gdx2d_freetype_set_error(error);
    return error == 0;
}

void* gdx2d_freetype_glyphslot_get_bitmap(void* glyph_slot) {
    FT_GlyphSlot slot = (FT_GlyphSlot)glyph_slot;
    if(slot == NULL) {
        return NULL;
    }
    return &slot->bitmap;
}

void* gdx2d_freetype_glyph_get_bitmap(void* glyph) {
    FT_BitmapGlyph bitmap_glyph = (FT_BitmapGlyph)glyph;
    if(bitmap_glyph == NULL) {
        return NULL;
    }
    return &bitmap_glyph->bitmap;
}

void* gdx2d_freetype_glyph_to_bitmap(void* glyph, int render_mode) {
    FT_Glyph bitmap = (FT_Glyph)glyph;
    FT_Error error = FT_Glyph_To_Bitmap(&bitmap, (FT_Render_Mode)render_mode, NULL, 1);
    gdx2d_freetype_set_error(error);
    if(error) {
        return NULL;
    }
    return bitmap;
}

int gdx2d_freetype_glyph_get_left(void* glyph) {
    FT_BitmapGlyph bitmap_glyph = (FT_BitmapGlyph)glyph;
    if(bitmap_glyph == NULL) {
        return 0;
    }
    return bitmap_glyph->left;
}

int gdx2d_freetype_glyph_get_top(void* glyph) {
    FT_BitmapGlyph bitmap_glyph = (FT_BitmapGlyph)glyph;
    if(bitmap_glyph == NULL) {
        return 0;
    }
    return bitmap_glyph->top;
}

void gdx2d_freetype_glyph_done(void* glyph) {
    if(glyph == NULL) {
        return;
    }
    FT_Done_Glyph((FT_Glyph)glyph);
}

void* gdx2d_freetype_glyph_stroke_border(void* glyph, void* stroker, int inside) {
    FT_Glyph border_glyph = (FT_Glyph)glyph;
    FT_Glyph_StrokeBorder(&border_glyph, (FT_Stroker)stroker, inside, 1);
    return border_glyph;
}

void* gdx2d_freetype_bitmap_get_buffer(void* bitmap) {
    FT_Bitmap* ft_bitmap = (FT_Bitmap*)bitmap;
    if(ft_bitmap == NULL) {
        return NULL;
    }
    return ft_bitmap->buffer;
}

int gdx2d_freetype_face_get_face_flags(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->face_flags;
}

int gdx2d_freetype_face_get_num_glyphs(void* face) {
    return face == NULL ? 0 : (int)((FT_Face)face)->num_glyphs;
}

int gdx2d_freetype_face_get_style_flags(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->style_flags;
}

int gdx2d_freetype_face_get_ascender(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->ascender;
}

int gdx2d_freetype_face_get_descender(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->descender;
}

int gdx2d_freetype_face_get_height(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->height;
}

int gdx2d_freetype_face_get_max_advance_width(void* face) {
    return face == NULL ? 0 : (int)((FT_Face)face)->max_advance_width;
}

int gdx2d_freetype_face_get_max_advance_height(void* face) {
    return face == NULL ? 0 : (int)((FT_Face)face)->max_advance_height;
}

int gdx2d_freetype_face_get_underline_position(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->underline_position;
}

int gdx2d_freetype_face_get_underline_thickness(void* face) {
    return face == NULL ? 0 : ((FT_Face)face)->underline_thickness;
}

void* gdx2d_freetype_face_get_size(void* face) {
    return face == NULL ? NULL : ((FT_Face)face)->size;
}

int gdx2d_freetype_face_get_char_index(void* face, int char_code) {
    return face == NULL ? 0 : (int)FT_Get_Char_Index((FT_Face)face, (FT_ULong)char_code);
}

int gdx2d_freetype_face_has_kerning(void* face) {
    return face != NULL && FT_HAS_KERNING((FT_Face)face);
}

int gdx2d_freetype_face_get_kerning(void* face, int left_glyph, int right_glyph, int kern_mode) {
    FT_Vector kerning;
    FT_Error error = FT_Get_Kerning((FT_Face)face, left_glyph, right_glyph, kern_mode, &kerning);
    gdx2d_freetype_set_error(error);
    if(error) {
        return 0;
    }
    return (int)kerning.x;
}

void* gdx2d_freetype_size_get_metrics(void* size) {
    return size == NULL ? NULL : &((FT_Size)size)->metrics;
}

int gdx2d_freetype_size_metrics_get_ascender(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->ascender;
}

int gdx2d_freetype_size_metrics_get_descender(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->descender;
}

int gdx2d_freetype_size_metrics_get_height(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->height;
}

int gdx2d_freetype_size_metrics_get_max_advance(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->max_advance;
}

int gdx2d_freetype_size_metrics_get_x_ppem(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->x_ppem;
}

int gdx2d_freetype_size_metrics_get_y_ppem(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->y_ppem;
}

int gdx2d_freetype_size_metrics_get_x_scale(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->x_scale;
}

int gdx2d_freetype_size_metrics_get_y_scale(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Size_Metrics*)metrics)->y_scale;
}

void* gdx2d_freetype_glyphslot_get_metrics(void* glyph_slot) {
    return glyph_slot == NULL ? NULL : &((FT_GlyphSlot)glyph_slot)->metrics;
}

int gdx2d_freetype_glyphslot_get_format(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : (int)((FT_GlyphSlot)glyph_slot)->format;
}

int gdx2d_freetype_glyphslot_get_bitmap_left(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : ((FT_GlyphSlot)glyph_slot)->bitmap_left;
}

int gdx2d_freetype_glyphslot_get_bitmap_top(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : ((FT_GlyphSlot)glyph_slot)->bitmap_top;
}

int gdx2d_freetype_glyphslot_get_linear_hori_advance(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : (int)((FT_GlyphSlot)glyph_slot)->linearHoriAdvance;
}

int gdx2d_freetype_glyphslot_get_linear_vert_advance(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : (int)((FT_GlyphSlot)glyph_slot)->linearVertAdvance;
}

int gdx2d_freetype_glyphslot_get_advance_x(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : (int)((FT_GlyphSlot)glyph_slot)->advance.x;
}

int gdx2d_freetype_glyphslot_get_advance_y(void* glyph_slot) {
    return glyph_slot == NULL ? 0 : (int)((FT_GlyphSlot)glyph_slot)->advance.y;
}

int gdx2d_freetype_bitmap_get_rows(void* bitmap) {
    return bitmap == NULL ? 0 : ((FT_Bitmap*)bitmap)->rows;
}

int gdx2d_freetype_bitmap_get_width(void* bitmap) {
    return bitmap == NULL ? 0 : ((FT_Bitmap*)bitmap)->width;
}

int gdx2d_freetype_bitmap_get_pitch(void* bitmap) {
    return bitmap == NULL ? 0 : ((FT_Bitmap*)bitmap)->pitch;
}

int gdx2d_freetype_bitmap_get_num_gray(void* bitmap) {
    return bitmap == NULL ? 0 : ((FT_Bitmap*)bitmap)->num_grays;
}

int gdx2d_freetype_bitmap_get_pixel_mode(void* bitmap) {
    return bitmap == NULL ? 0 : ((FT_Bitmap*)bitmap)->pixel_mode;
}

int gdx2d_freetype_glyph_metrics_get_width(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->width;
}

int gdx2d_freetype_glyph_metrics_get_height(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->height;
}

int gdx2d_freetype_glyph_metrics_get_hori_advance(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->horiAdvance;
}

int gdx2d_freetype_glyph_metrics_get_hori_bearing_x(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->horiBearingX;
}

int gdx2d_freetype_glyph_metrics_get_hori_bearing_y(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->horiBearingY;
}

int gdx2d_freetype_glyph_metrics_get_vert_bearing_x(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->vertBearingX;
}

int gdx2d_freetype_glyph_metrics_get_vert_bearing_y(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->vertBearingY;
}

int gdx2d_freetype_glyph_metrics_get_vert_advance(void* metrics) {
    return metrics == NULL ? 0 : ((FT_Glyph_Metrics*)metrics)->vertAdvance;
}

void gdx2d_freetype_stroker_set(void* stroker, int radius, int line_cap, int line_join, int miter_limit) {
    if(stroker == NULL) {
        return;
    }
    FT_Stroker_Set((FT_Stroker)stroker, radius, (FT_Stroker_LineCap)line_cap,
                   (FT_Stroker_LineJoin)line_join, miter_limit);
}

void gdx2d_freetype_stroker_done(void* stroker) {
    if(stroker == NULL) {
        return;
    }
    FT_Stroker_Done((FT_Stroker)stroker);
}
