package plot

import kotlin.native.concurrent.*
import kotlinx.cinterop.*
import platform.posix.sleep
import kotlin.text.*
import openal.*
import mgl.*
import gtk4.*
import synth.*
import kotlin.math.*

var dev: CPointer<ALCdevice>? = null
var ctx: CPointer<ALCcontext>? = null
var app:CPointer<GtkApplication>? = null
var keys: CPointer<GObject>? = null
var graph: CPointer<GObject>? = null
var ui_step_math: CPointer<GObject>? = null
var ui_sound_math: CPointer<GObject>? = null
var ui_envelop1_math: CPointer<GObject>? = null
var ui_envelop2_math: CPointer<GObject>? = null
var ui_envelop3_math: CPointer<GObject>? = null
var ui_envelop4_math: CPointer<GObject>? = null
var ui_final_math: CPointer<GObject>? = null
var mathParamGlobal: MathParam? = null
val keyStates: HashMap<UInt, Boolean> = HashMap<UInt, Boolean>()
lateinit var g_wglarea: CPointer<GtkGLArea>

@ThreadLocal
val sounds: Array<Int> = arrayOf<Int>(
'z'.code, 's'.code, 'x'.code, 'd'.code, 
'c'.code, 'v'.code, 'g'.code, 'b'.code,
'h'.code, 'n'.code, 'j'.code, 'm'.code,
','.code, 'l'.code, '.'.code, ';'.code,
'/'.code, 'q'.code, '2'.code, 'w'.code,
'3'.code, 'e'.code, 'r'.code, '5'.code,
't'.code, '6'.code, 'y'.code, '7'.code,
'u'.code, 'i'.code, '9'.code, 'o'.code,
'0'.code, 'p'.code, '['.code, '='.code,
']'.code
)

data class MathParam(
                 val step: String, 
                 val sound: String, 
                 val envelop1: String,
                 val envelop2: String,
                 val envelop3: String,
                 val envelop4: String,
                 val finalMath: String,
                 val key: Int,
                 var sr: Int = 44100,
                 var d: Float = 1.0f,
)

var WINDOW_WIDTH = 957
var WINDOW_HEIGHT = 124

fun generate_samples(mathParam: MathParam): HMDT? {
    var key2 = sounds.indexOf(mathParam.key)
    val key = "${key2}".toInt()
    
    var d = mathParam.d
    var sr = mathParam.sr
    var y = mgl_create_data_size(sr.toInt(),1,0)
    
    var step = mathParam.step
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    var sound = mathParam.sound
    .replace(oldValue= "\${step}", newValue = step)
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    var finalMath = mathParam.finalMath
    .replace(oldValue= "\${sound}", newValue = sound)
    .replace(oldValue= "\${envelop1}", newValue = mathParam.envelop1)
    .replace(oldValue= "\${envelop2}", newValue = mathParam.envelop2)
    .replace(oldValue= "\${envelop3}", newValue = mathParam.envelop3)
    .replace(oldValue= "\${envelop4}", newValue = mathParam.envelop4)
    .replace(oldValue= "\${step}", newValue = step)
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    mgl_data_modify(y, finalMath ,0);
        
    return y
}

fun sound_thread(mathParam: MathParam) = memScoped {
    initRuntimeIfNeeded()
  
    var buf = allocArray<ALuintVar>(1)
    alGenBuffers(1, buf)
  
    val y = generate_samples(mathParam)
    
    var sr = mathParam.sr
    
    var samples = ShortArray(sr.toInt())
    for(i in 0..sr-1) {
       samples[i] = mgl_data_get_value(y,i,0,0).toInt().toShort()
    }
    
    
    samples.usePinned {
       alBufferData(
           buf[0], AL_FORMAT_MONO16, 
           it.addressOf(0), 
           (sr * sizeOf<ShortVar>()).toInt(), sr.toInt()
       )
    }
  
    var src = allocArray<ALuintVar>(1)
    alGenSources(1.toInt(), src)
    alSourcei(src[0], AL_BUFFER, buf[0].toInt())
    
    alSourcePlay(src[0])
    
    var d = mathParam.d
    sleep(d.toUInt())
  
    alSourcei(src[0], AL_BUFFER, 0)
    alDeleteSources(1, src)
    alDeleteBuffers(1, buf)
}

fun realize_callback(
                 widget:CPointer<GtkWidget>?
) {
    println("realize_callback")
}

fun render_graph_callback(
                 glarea:CPointer<GtkDrawingArea>?, 
                 cr:CPointer<cairo_t>?
) = memScoped {

    val gr = mgl_create_graph(WINDOW_WIDTH, WINDOW_HEIGHT)
    if(mathParamGlobal==null) mathParamGlobal=get_params()
    val y = generate_samples(mathParamGlobal!!)
    mgl_set_range_val(gr, "y"[0].toByte(), -15000.0, 15000.0);
    mgl_plot(gr,y,"b","")
    mgl_box(gr)
    
    var w=mgl_get_width(gr)
    var h=mgl_get_height(gr)
    
    var channels = 4
    
    var surface_data = allocArray<UByteVar>((channels * w * h).toInt() * (sizeOf<UByteVar>()).toInt())
    
    var buf = mgl_get_rgba(gr);
    platform.posix.memcpy(surface_data, buf,(channels * w * h).toULong())
    
    /*cairo_set_source_rgb(cr, 1.0, 1.0, 1.0)
    cairo_paint(cr)
    
    cairo_set_source_rgb(cr, 1.0, 0.0, 0.0)
    cairo_rectangle(cr,0.0,0.0,50.0,50.0)
    cairo_fill(cr)*/
    
    var surface = cairo_image_surface_create_for_data (surface_data, CAIRO_FORMAT_ARGB32, w, h, channels * w)
    cairo_surface_flush(surface)
    //cairo_mask_surface(cr, surface, 0.0, 0.0)
    //cairo_set_source_surface(cr, surface, 0.0, 0.0)
    //cairo_rectangle (cr, 0.0,0.0,150.0,150.0)
    //cairo_paint (cr)
    
    //cairo_surface_destroy(surface)
    
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

fun render_callback(
                 glarea:CPointer<GtkDrawingArea>?, 
                 cr: CPointer<cairo_t>?
) = memScoped {

    var error = alloc<CPointerVar<GError>>()
    var handle = rsvg_handle_new_from_file ("svg/key_white.svg", error.ptr);
    if(error.value!=null)
        throw Error("unable to process key_white.svg " + error.value)
    if(handle==null)
        throw Error("unable to load key_white.svg")
   
var style = """
.wbutton {
  fill:#ffffff;fill-opacity:1;stroke:#000000;stroke-opacity:1;stroke-width:0.26458334;stroke-miterlimit:4;stroke-dasharray:none
}
.wbutton {
  height: 25px;
  transform: scaleY(0.5);
}
.bbutton {
  height: 12px;
  transform: scaleY(0.5);
}
"""

for ((key, value) in keyStates) {
    if(value) {
    var zkey = key.toString()
    if(zkey.length==1) { zkey = "00"+zkey }
    if(zkey.length==2) { zkey = "0"+zkey }

style = style + """

#rect${zkey} {
  fill:#B0C4DE;fill-opacity:1;stroke:#000000;stroke-opacity:1;stroke-width:0.26458334;stroke-miterlimit:4;stroke-dasharray:none
}

"""
    }
}
    val bytes = style.toCharArray()
        
    var styleObj = allocArray<guint8Var>(bytes.size).apply {
        for(i in 0..bytes.size-1) {
            this[i] = bytes[i].code.toUByte()
        }
    }
    
    rsvg_handle_set_stylesheet (handle, styleObj, 
                            bytes.size.toULong(),
                            error.ptr);
    if(error.value!=null)
        throw Error("unable to process css for svg")

    if( rsvg_handle_render_cairo ( handle, cr ) != 1 )
        throw Error( "Drawing failed" )  
}

fun get_params(keyval: Int = 0): MathParam {
    return MathParam (
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_step_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_sound_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_envelop1_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_envelop2_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_envelop3_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_envelop4_math!!.reinterpret())
            )!!.toKString(),
            gtk_entry_buffer_get_text(
              gtk_entry_get_buffer(ui_final_math!!.reinterpret())
            )!!.toKString(),
            keyval.toInt()
        )
}

fun key_pressed (
                 controller: CPointer<GtkEventController>,
                 keyval: guint,
                 keycode: guint,
                 modifiers: GdkModifierType,
                 entry: CPointer<GtkEntry>
):gboolean {
    val isKey = keyStates.get(keyval)
    
    if(!(isKey==true) 
        && sounds.indexOf(keyval.toInt())!=-1 
        && sounds.indexOf(keyval.toInt())<sounds.size){
        
        mathParamGlobal = get_params(keyval.toInt())
        
        val worker = Worker.start(true, "worker1")
        worker.execute(TransferMode.UNSAFE, { mathParamGlobal }) { data ->
            sound_thread(data!!)
            null
        }
    }
    
    keyStates.put(keyval,true)
    gtk_widget_queue_draw(keys!!.reinterpret())
    return GDK_EVENT_PROPAGATE
}

fun left_mouse_pressed (
                 gesture: CPointer<GtkGestureClick>,
                 n_press: Int,
                 x: Double,
                 y: Double,
                 widget: CPointer<GtkWidget>
):gboolean {
    gtk_widget_grab_focus(widget)
    return GDK_EVENT_STOP
}

fun key_released (
                 controller: CPointer<GtkEventController>,
                 keyval: guint,
                 keycode: guint,
                 modifiers: GdkModifierType,
                 entry: CPointer<GtkEntry>
):gboolean {
    keyStates.put(keyval,false)
    gtk_widget_queue_draw(keys!!.reinterpret())
    return GDK_EVENT_PROPAGATE
}

fun toggle_edit(togglebutton: CPointer<GtkToggleButton>) {
    var button_state = gtk_toggle_button_get_active(togglebutton);
    
    gtk_widget_set_focusable(ui_step_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_step_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_step_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_sound_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_sound_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_sound_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_envelop1_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_envelop1_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_envelop1_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_envelop2_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_envelop2_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_envelop2_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_envelop3_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_envelop3_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_envelop3_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_envelop4_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_envelop4_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_envelop4_math!!.reinterpret(), button_state)
    
    gtk_widget_set_focusable(ui_final_math!!.reinterpret(), button_state)
    gtk_widget_set_can_focus(ui_final_math!!.reinterpret(), button_state)
    gtk_editable_set_editable(ui_final_math!!.reinterpret(), button_state)
    
    mathParamGlobal=get_params()
    
    gtk_widget_queue_draw(graph!!.reinterpret())
}

fun math_edit_toggled(
                 togglebutton: CPointer<GtkToggleButton>, 
                 text_label: CPointer<GtkLabel>
)
{
    toggle_edit(togglebutton)
}

fun activate_callback(app:CPointer<GtkApplication>?) {
    println("activate")
    
    var builder = gtk_builder_new_from_file ("glade/window_main.glade")
    var window = gtk_builder_get_object(builder, "window_main")
    gtk_window_set_application (window!!.reinterpret(), app)

    var provider = gtk_css_provider_new();
    gtk_css_provider_load_from_path(provider, "css/theme.css");
    gtk_style_context_add_provider_for_display(
                               gtk_widget_get_display(window.reinterpret()),
                               provider!!.reinterpret(),
                               GTK_STYLE_PROVIDER_PRIORITY_USER);
    keys = gtk_builder_get_object(builder, "keyboard_keys")
    graph = gtk_builder_get_object(builder, "math_graph")
    ui_step_math = gtk_builder_get_object(builder, "step_math")
    ui_sound_math = gtk_builder_get_object(builder, "sound_math")
    ui_envelop1_math = gtk_builder_get_object(builder, "envelop1_math")
    ui_envelop2_math = gtk_builder_get_object(builder, "envelop2_math")
    ui_envelop3_math = gtk_builder_get_object(builder, "envelop3_math")
    ui_envelop4_math = gtk_builder_get_object(builder, "envelop4_math")
    ui_final_math = gtk_builder_get_object(builder, "final_math")
    var ui_math_toggle = gtk_builder_get_object(builder, "math_toggle")
    toggle_edit(ui_math_toggle!!.reinterpret())
    
    gtk_drawing_area_set_draw_func(
        keys!!.reinterpret(),
        staticCFunction {
            glarea: CPointer<GtkDrawingArea>?, 
            cr: CPointer<cairo_t>?
            -> render_callback ( glarea, cr )
        }.reinterpret(),
        null, 
        null
    )
    
    gtk_drawing_area_set_draw_func(
        graph!!.reinterpret(),
        staticCFunction {
            glarea: CPointer<GtkDrawingArea>?,
            cr: CPointer<cairo_t>?
            -> render_graph_callback ( glarea, cr )
        }.reinterpret(),
        null, 
        null
    )
    
    var keyboard_controller = gtk_event_controller_key_new()
    var focus_controller = gtk_event_controller_focus_new()
    var motion_controller = gtk_gesture_click_new()
    gtk_gesture_single_set_button (motion_controller!!.reinterpret(), 1);
    
    g_signal_connect_data (
        ui_math_toggle!!.reinterpret(), 
        "toggled", 
        staticCFunction {
            togglebutton: CPointer<GtkToggleButton>, 
            text_label: CPointer<GtkLabel>
            -> math_edit_toggled(togglebutton, text_label)
        }.reinterpret(),
        ui_math_toggle!!.reinterpret(), 
        null, 
        0u
    )
    
    g_object_set_data_full (
        window!!.reinterpret(), 
        "controller", 
        g_object_ref(keyboard_controller!!.reinterpret()), 
        staticCFunction {
            obj: gpointer? -> g_object_unref(obj)
        }.reinterpret()
    )
    
    g_object_set_data_full (
        keys!!.reinterpret(), 
        "controller", 
        g_object_ref(focus_controller!!.reinterpret()), 
        staticCFunction {
            obj: gpointer? -> g_object_unref(obj)
        }.reinterpret()
    )
    
    g_object_set_data_full (
        keys!!.reinterpret(), 
        "controller", 
        g_object_ref(motion_controller!!.reinterpret()), 
        staticCFunction {
            obj: gpointer? -> g_object_unref(obj)
        }.reinterpret()
    )
    
    g_signal_connect_data (
        keyboard_controller!!.reinterpret(), 
        "key-pressed", 
        staticCFunction {
             controller: CPointer<GtkEventController>,
             keyval: guint,
             keycode: guint,
             modifiers: GdkModifierType,
             entry: CPointer<GtkEntry>
             -> key_pressed (
                controller, 
                keyval,
                keycode,
                modifiers,
                entry
             )
        }.reinterpret(), 
        keys!!.reinterpret(), 
        null, 
        0u
    )
    
    g_signal_connect_data (
        keyboard_controller!!.reinterpret(), 
        "key-released", 
        staticCFunction {
             controller: CPointer<GtkEventController>,
             keyval: guint,
             keycode: guint,
             modifiers: GdkModifierType,
             entry: CPointer<GtkEntry>
             -> key_released (
                controller, 
                keyval,
                keycode,
                modifiers,
                entry
             )
        }.reinterpret(), 
        keys!!.reinterpret(), 
        null, 
        0u
    )
    
    g_signal_connect_data (
        motion_controller!!.reinterpret(), 
        "pressed", 
        staticCFunction {
             gesture: CPointer<GtkGestureClick>,
             n_press: Int,
             x: Double,
             y: Double,
             widget: CPointer<GtkWidget>
             -> left_mouse_pressed(gesture, n_press, x, y, widget)
        }.reinterpret(), 
        keys!!.reinterpret(), 
        null, 
        0u
    )
    
    gtk_widget_add_controller (
        window!!.reinterpret(), 
        keyboard_controller!!.reinterpret()
    )
    
    gtk_widget_add_controller (
        keys!!.reinterpret(), 
        focus_controller!!.reinterpret()
    )
    
    gtk_widget_add_controller (
        keys!!.reinterpret(), 
        motion_controller!!.reinterpret()
    )
    
    g_object_unref(builder)
    gtk_widget_show (window.reinterpret())
}

fun startup_callback(app:CPointer<GtkApplication>?) {
    println("startup")
    
    var defname = alcGetString(null, ALC_DEFAULT_DEVICE_SPECIFIER)
    dev = alcOpenDevice(defname?.toKString())
    ctx = alcCreateContext(dev, null)
    alcMakeContextCurrent(ctx)

}

fun shutdown_callback(app:CPointer<GtkApplication>?) {
    println("shutdown")
    
    alcMakeContextCurrent(null)
    alcDestroyContext(ctx)
    alcCloseDevice(dev)

}

fun main() {
    app = gtk_application_new ("org.gtk.example", G_APPLICATION_FLAGS_NONE)
    
    g_signal_connect_data(
        app!!.reinterpret(), 
        "activate", 
        staticCFunction {  app:CPointer<GtkApplication>?
            -> activate_callback(app)
        }.reinterpret(), 
        null, 
        null, 
        0u
    )
    
    g_signal_connect_data(
        app!!.reinterpret(), 
        "startup", 
        staticCFunction {  app:CPointer<GtkApplication>?
            -> startup_callback(app)
        }.reinterpret(), 
        null, 
        null, 
        0u
    )
    
    g_signal_connect_data(
        app!!.reinterpret(), 
        "shutdown", 
        staticCFunction {  app:CPointer<GtkApplication>?
            -> shutdown_callback(app)
        }.reinterpret(), 
        null, 
        null, 
        0u
    )
    
    run_app(app, 0, null)
    g_object_unref (app)
    
}