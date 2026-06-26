#ifndef GDX2D_FREETYPE_BRIDGE_H
#define GDX2D_FREETYPE_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

int gdx2d_freetype_get_last_error(void);
int gdx2d_freetype_address_is_null(void* address);

void* gdx2d_freetype_init(void);
void gdx2d_freetype_done(void* library);
void gdx2d_freetype_done_face(void* face);
void* gdx2d_freetype_new_memory_face(void* library, void* data, int data_size, int face_index);
void* gdx2d_freetype_stroker_new(void* library);

int gdx2d_freetype_face_set_pixel_sizes(void* face, int pixel_width, int pixel_height);
int gdx2d_freetype_face_select_size(void* face, int strike_index);
int gdx2d_freetype_face_set_char_size(void* face, int char_width, int char_height,
                                      int horz_resolution, int vert_resolution);
int gdx2d_freetype_face_load_char(void* face, int char_code, int load_flags);
int gdx2d_freetype_face_load_glyph(void* face, int glyph_index, int load_flags);
void* gdx2d_freetype_face_get_glyph(void* face);
int gdx2d_freetype_face_get_face_flags(void* face);
int gdx2d_freetype_face_get_num_glyphs(void* face);
int gdx2d_freetype_face_get_style_flags(void* face);
int gdx2d_freetype_face_get_ascender(void* face);
int gdx2d_freetype_face_get_descender(void* face);
int gdx2d_freetype_face_get_height(void* face);
int gdx2d_freetype_face_get_max_advance_width(void* face);
int gdx2d_freetype_face_get_max_advance_height(void* face);
int gdx2d_freetype_face_get_underline_position(void* face);
int gdx2d_freetype_face_get_underline_thickness(void* face);
void* gdx2d_freetype_face_get_size(void* face);
int gdx2d_freetype_face_get_char_index(void* face, int char_code);
int gdx2d_freetype_face_has_kerning(void* face);
int gdx2d_freetype_face_get_kerning(void* face, int left_glyph, int right_glyph, int kern_mode);

void* gdx2d_freetype_size_get_metrics(void* size);
int gdx2d_freetype_size_metrics_get_ascender(void* metrics);
int gdx2d_freetype_size_metrics_get_descender(void* metrics);
int gdx2d_freetype_size_metrics_get_height(void* metrics);
int gdx2d_freetype_size_metrics_get_max_advance(void* metrics);
int gdx2d_freetype_size_metrics_get_x_ppem(void* metrics);
int gdx2d_freetype_size_metrics_get_y_ppem(void* metrics);
int gdx2d_freetype_size_metrics_get_x_scale(void* metrics);
int gdx2d_freetype_size_metrics_get_y_scale(void* metrics);

void* gdx2d_freetype_glyphslot_get_glyph(void* glyph_slot);
int gdx2d_freetype_glyphslot_render_glyph(void* glyph_slot, int render_mode);
void* gdx2d_freetype_glyphslot_get_bitmap(void* glyph_slot);
void* gdx2d_freetype_glyphslot_get_metrics(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_format(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_bitmap_left(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_bitmap_top(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_linear_hori_advance(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_linear_vert_advance(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_advance_x(void* glyph_slot);
int gdx2d_freetype_glyphslot_get_advance_y(void* glyph_slot);

void* gdx2d_freetype_glyph_get_bitmap(void* glyph);
void* gdx2d_freetype_glyph_to_bitmap(void* glyph, int render_mode);
int gdx2d_freetype_glyph_get_left(void* glyph);
int gdx2d_freetype_glyph_get_top(void* glyph);
void gdx2d_freetype_glyph_done(void* glyph);
void* gdx2d_freetype_glyph_stroke_border(void* glyph, void* stroker, int inside);

void* gdx2d_freetype_bitmap_get_buffer(void* bitmap);
int gdx2d_freetype_bitmap_get_rows(void* bitmap);
int gdx2d_freetype_bitmap_get_width(void* bitmap);
int gdx2d_freetype_bitmap_get_pitch(void* bitmap);
int gdx2d_freetype_bitmap_get_num_gray(void* bitmap);
int gdx2d_freetype_bitmap_get_pixel_mode(void* bitmap);

int gdx2d_freetype_glyph_metrics_get_width(void* metrics);
int gdx2d_freetype_glyph_metrics_get_height(void* metrics);
int gdx2d_freetype_glyph_metrics_get_hori_advance(void* metrics);
int gdx2d_freetype_glyph_metrics_get_hori_bearing_x(void* metrics);
int gdx2d_freetype_glyph_metrics_get_hori_bearing_y(void* metrics);
int gdx2d_freetype_glyph_metrics_get_vert_bearing_x(void* metrics);
int gdx2d_freetype_glyph_metrics_get_vert_bearing_y(void* metrics);
int gdx2d_freetype_glyph_metrics_get_vert_advance(void* metrics);

void gdx2d_freetype_stroker_set(void* stroker, int radius, int line_cap, int line_join, int miter_limit);
void gdx2d_freetype_stroker_done(void* stroker);

#ifdef __cplusplus
}
#endif

#endif
