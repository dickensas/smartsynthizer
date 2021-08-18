package plot

import kotlin.native.concurrent.*
import kotlinx.cinterop.*
import platform.posix.sleep
import kotlin.text.*
import openal.*
import mgl.*
import gtk4.*
import synth.*
import rtmidi.*

class MathUI {
    var graph: CPointer<GObject>? = null
    var step_math: CPointer<GObject>? = null
    var sound_math: CPointer<GObject>? = null
    var envelop1_math: CPointer<GObject>? = null
    var envelop2_math: CPointer<GObject>? = null
    var envelop3_math: CPointer<GObject>? = null
    var envelop4_math: CPointer<GObject>? = null
    var final_math: CPointer<GObject>? = null
    var enable_toggle: CPointer<GObject>? = null
    var math_toggle: CPointer<GObject>? = null
    var math_param: MathParam = MathParam()
    var track: Int = 1
    
    fun init(builder: CPointer<GObject>?) {
        graph = gtk_builder_get_object(builder!!.reinterpret(), "math_graph_track${track}")
        step_math = gtk_builder_get_object(builder!!.reinterpret(), "step_math_track${track}")
        sound_math = gtk_builder_get_object(builder!!.reinterpret(), "sound_math_track${track}")
        envelop1_math = gtk_builder_get_object(builder!!.reinterpret(), "envelop1_math_track${track}")
        envelop2_math = gtk_builder_get_object(builder!!.reinterpret(), "envelop2_math_track${track}")
        envelop3_math = gtk_builder_get_object(builder!!.reinterpret(), "envelop3_math_track${track}")
        envelop4_math = gtk_builder_get_object(builder!!.reinterpret(), "envelop4_math_track${track}")
        final_math = gtk_builder_get_object(builder!!.reinterpret(), "final_math_track${track}")
        math_toggle = gtk_builder_get_object(builder!!.reinterpret(), "math_toggle_track${track}")
        enable_toggle = gtk_builder_get_object(builder!!.reinterpret(), "enable_toggle_track${track}")
        
        g_object_set_data(math_toggle!!.reinterpret(), "track", gtk_label_new(track.toString()))
        g_object_set_data(enable_toggle!!.reinterpret(), "track", gtk_label_new(track.toString()))
        
        toggle_edit(math_toggle!!.reinterpret())
        
        g_signal_connect_data (
            math_toggle!!.reinterpret(), 
            "toggled", 
            staticCFunction {
                track: CPointer<GObject>
                -> global_math_edit_toggled (track)
            }.reinterpret(),
            math_toggle!!.reinterpret(), 
            null, 
            0u
        )
        
        g_signal_connect_data (
            enable_toggle!!.reinterpret(), 
            "toggled", 
            staticCFunction {
                track: CPointer<GObject>
                -> global_math_enable_toggled (track)
            }.reinterpret(),
            enable_toggle!!.reinterpret(), 
            null, 
            0u
        )
        
        gtk_drawing_area_set_draw_func(
            graph!!.reinterpret(),
            staticCFunction {
                glarea: CPointer<GtkDrawingArea>?,
                cr: CPointer<cairo_t>?,
                width: Int,
                height: Int,
                data: CPointer<GObject>
                -> global_render_graph_callback ( glarea, cr, width, height, data)
            }.reinterpret(),
            gtk_label_new(track.toString()), 
            null
        )
        
        math_param=get_params()
    }
    
    fun toggle_edit(togglebutton: CPointer<GtkToggleButton>) {
        var button_state = gtk_toggle_button_get_active(togglebutton);
        
        gtk_widget_set_focusable(step_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(step_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(step_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(sound_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(sound_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(sound_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(envelop1_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(envelop1_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(envelop1_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(envelop2_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(envelop2_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(envelop2_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(envelop3_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(envelop3_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(envelop3_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(envelop4_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(envelop4_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(envelop4_math!!.reinterpret(), button_state)
        
        gtk_widget_set_focusable(final_math!!.reinterpret(), button_state)
        gtk_widget_set_can_focus(final_math!!.reinterpret(), button_state)
        gtk_editable_set_editable(final_math!!.reinterpret(), button_state)
        
        math_param=get_params()
        
        gtk_widget_queue_draw(graph!!.reinterpret())
    }
    
    fun toggle_enable(
                 togglebutton: CPointer<GtkToggleButton>
    )
    {
        if(math_param!=null) {
            math_param.enable = gtk_toggle_button_get_active(togglebutton)
        } 
    }
    
    fun get_params(keyval: Int = 0): MathParam {
        return MathParam (
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(step_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(sound_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(envelop1_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(envelop2_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(envelop3_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(envelop4_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(final_math!!.reinterpret())
            )!!.toKString(),
            keyval.toInt()
        )
    }
    
    fun start_sound_thread(key: Int) {
        math_param = get_params(key)
        math_param.enable = gtk_toggle_button_get_active(
           enable_toggle!!.reinterpret()
        )
    
        Worker
        .start(true, "worker_${track}")
        .execute(TransferMode.UNSAFE, { math_param }) { data ->
            sound_thread(data!!)
            null
        }
    }
    
    fun render_graph(
                 glarea: CPointer<GtkDrawingArea>?, 
                 cr: CPointer<cairo_t>?
    ) = memScoped {
        val gr = mgl_create_graph(WINDOW_WIDTH, WINDOW_HEIGHT)
        if(math_param==null) math_param=get_params()
        val y = generate_samples(math_param!!)
        mgl_set_range_val(gr, "y"[0].toByte(), -15000.0, 15000.0);
        mgl_plot(gr,y,"b","")
        mgl_box(gr)
        
        var w=mgl_get_width(gr)
        var h=mgl_get_height(gr)
        
        var channels = 4
        
        var surface_data = allocArray<UByteVar>((channels * w * h).toInt() * (sizeOf<UByteVar>()).toInt())
        
        var buf = mgl_get_rgba(gr);
        platform.posix.memcpy(surface_data, buf,(channels * w * h).toULong())
        
        var surface = cairo_image_surface_create_for_data (surface_data, CAIRO_FORMAT_ARGB32, w, h, channels * w)
        cairo_surface_flush(surface)
        
        var first = cairo_surface_create_similar(
          cairo_get_target(cr),
          CAIRO_CONTENT_COLOR_ALPHA, WINDOW_WIDTH, WINDOW_HEIGHT
        )
        
        var first_cr = cairo_create(first)
        cairo_set_source_rgb(first_cr, 1.0, 1.0, 1.0)
        cairo_rectangle(first_cr, 0.0, 0.0, WINDOW_WIDTH.toDouble(), WINDOW_HEIGHT.toDouble())
        cairo_fill(first_cr)
      
        cairo_set_source_surface(first_cr, surface, 0.0, 0.0);
        cairo_paint(first_cr)
        
        cairo_surface_flush(surface)
        
        cairo_set_source_surface(cr, first, 0.0, 0.0);
        cairo_paint(cr)
        cairo_surface_flush(first)
        
        cairo_surface_destroy(first)
        cairo_surface_destroy(surface)
        
        cairo_destroy(first_cr)
    }
}
