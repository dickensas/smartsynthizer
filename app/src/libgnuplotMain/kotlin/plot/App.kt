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

var dev: CPointer<ALCdevice>? = null
var ctx: CPointer<ALCcontext>? = null
var app:CPointer<GtkApplication>? = null
var keys: CPointer<GObject>? = null

var midi_combo: CPointer<GObject>? = null
val keyStates: HashMap<UInt, Boolean> = HashMap<UInt, Boolean>()
var midiPorts = mutableListOf<String>()
lateinit var midiPtr: RtMidiInPtr

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

fun global_render_graph_callback(
                 glarea: CPointer<GtkDrawingArea>?, 
                 cr: CPointer<cairo_t>?,
                 width: Int,
                 height: Int,
                 track: CPointer<GObject>
)
{
    var trk = gtk_label_get_text(track!!.reinterpret())!!.toKString().toInt()
    listMathUI[trk-1].render_graph(glarea, cr)
}

fun global_math_edit_toggled(track: CPointer<GObject>)
{
    var trk = gtk_label_get_text(g_object_get_data(track!!.reinterpret(), "track")!!.reinterpret())!!.toKString().toInt()
    listMathUI[trk-1].toggle_edit(listMathUI[trk-1].math_toggle!!.reinterpret())
}

fun global_toggle_edit(track: CPointer<GObject>)
{
    var trk = gtk_label_get_text(g_object_get_data(track!!.reinterpret(), "track")!!.reinterpret())!!.toKString().toInt()
    listMathUI[trk-1].toggle_edit(listMathUI[trk-1].enable_toggle!!.reinterpret())
}

val listMathUI = listOf<MathUI>(MathUI(),MathUI(),MathUI())

var WINDOW_WIDTH = 957
var WINDOW_HEIGHT = 124

fun midi_change() {
    val portName = gtk_combo_box_get_active_id(midi_combo!!.reinterpret())
    rtmidi_close_port(midiPtr!!)
    rtmidi_open_port(
       midiPtr!!, 
       portName!!.toKString().toUInt(), 
       midiPorts[portName!!.toKString().toInt()]
    )
}

fun generate_samples(math_param: MathParam): HMDT? {
    var key2 = sounds.indexOf(math_param.key)
    val key = "${key2}".toInt()
    
    var d = math_param.d
    var sr = math_param.sr
    var y = _mgl_create_data_size(sr.toInt(),1)
    
    var step = math_param.step
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    var sound = math_param.sound
    .replace(oldValue= "\${step}", newValue = step)
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    var finalMath = math_param.finalMath
    .replace(oldValue= "\${sound}", newValue = sound)
    .replace(oldValue= "\${envelop1}", newValue = math_param.envelop1)
    .replace(oldValue= "\${envelop2}", newValue = math_param.envelop2)
    .replace(oldValue= "\${envelop3}", newValue = math_param.envelop3)
    .replace(oldValue= "\${envelop4}", newValue = math_param.envelop4)
    .replace(oldValue= "\${step}", newValue = step)
    .replace(oldValue= "\${key}", newValue = key.toString())
    .replace(oldValue= "\${d}", newValue = d.toString())
    .replace(oldValue= "\${sr}", newValue = sr.toString())
    
    mgl_data_modify(y, finalMath ,0);
        
    return y
}

fun sound_thread(math_param: MathParam) = memScoped {
    initRuntimeIfNeeded()
    if(math_param.enable == 0) return
  
    var buf = allocArray<ALuintVar>(1)
    alGenBuffers(1, buf)
  
    val y = generate_samples(math_param)
    
    var sr = math_param.sr
    
    var samples = ShortArray(sr.toInt())
    for(i in 0..sr-1) {
       samples[i] = _mgl_data_get_value(y,i,0,0).toInt().toShort()
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
    
    var d = math_param.d
    g_usleep((1000000 * d).toInt().toUInt())
  
    alSourcei(src[0], AL_BUFFER, 0)
    alDeleteSources(1, src)
    alDeleteBuffers(1, buf)
}

fun realize_callback(
                 widget:CPointer<GtkWidget>?
) {
    
}

fun render_about_page_callback(
                 glarea:CPointer<GtkDrawingArea>?, 
                 cr: CPointer<cairo_t>?
) = memScoped {
    var error = alloc<CPointerVar<GError>>()
    var handle = rsvg_handle_new_from_file ("svg/about.svg", error.ptr);
    if(error.value!=null)
        throw Error("unable to process about.svg " + error.value)
    if(handle==null)
        throw Error("unable to load about.svg")
    if( rsvg_handle_render_cairo ( handle, cr ) != 1 )
        throw Error( "Drawing about.svg failed" ) 
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
        for(i in 0..listMathUI.size-1) {
            listMathUI[i].start_sound_thread(keyval.toInt())
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

fun activate_callback(app:CPointer<GtkApplication>?) {
    
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
    for(i in 1..listMathUI.size) {
       listMathUI[i-1].track = i
       listMathUI[i-1].init(builder!!.reinterpret())
    }
    
    midi_combo = gtk_builder_get_object(builder, "midi_combo")
    var about_page = gtk_builder_get_object(builder, "about_page")
    
    gtk_drawing_area_set_draw_func(
        about_page!!.reinterpret(),
        staticCFunction {
            glarea: CPointer<GtkDrawingArea>?, 
            cr: CPointer<cairo_t>?
            -> render_about_page_callback ( glarea, cr )
        }.reinterpret(),
        null, 
        null
    )
    
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
    
    var keyboard_controller = gtk_event_controller_key_new()
    var focus_controller = gtk_event_controller_focus_new()
    var motion_controller = gtk_gesture_click_new()
    gtk_gesture_single_set_button (motion_controller!!.reinterpret(), 1);
    
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
    
    midiPtr = rtmidi_in_create_default()!!
    
    rtmidi_in_set_callback(midiPtr, staticCFunction {  
            timeStamp: Double, 
            message: CArrayPointer<UByteVar>,
            messageSize: size_t, 
            userData: COpaquePointerVar
            -> midi_callback( timeStamp, message, messageSize, userData  )
        }.reinterpret(),
        null
    )

    var c = rtmidi_get_port_count(midiPtr)
    
    for(i in 0..c.toInt()-1){
        var portName = rtmidi_get_port_name(midiPtr, i.toUInt())!!.toKString()
        gtk_combo_box_text_append(
           midi_combo!!.reinterpret(),
           i.toString(),
           portName
        )
        midiPorts.add(portName)
    }
    
    g_signal_connect_data (
    midi_combo!!.reinterpret(), 
    "notify::active", 
    staticCFunction<Unit> {
      midi_change()
    }, 
    null,
    null, 
    0u);

    g_object_unref(builder)
    gtk_widget_show (window.reinterpret())

}

fun startup_callback(app:CPointer<GtkApplication>?) {
    
    var defname = alcGetString(null, ALC_DEFAULT_DEVICE_SPECIFIER)
    dev = alcOpenDevice(defname?.toKString())
    ctx = alcCreateContext(dev, null)
    alcMakeContextCurrent(ctx)

}

fun shutdown_callback(app:CPointer<GtkApplication>?) {
    
    alcMakeContextCurrent(null)
    alcDestroyContext(ctx)
    alcCloseDevice(dev)
    
    rtmidi_in_cancel_callback(midiPtr)
    rtmidi_close_port(midiPtr)
    rtmidi_in_free(midiPtr)
}

fun midi_idle (data: gpointer): gboolean {
    var key: CPointer<IntVar> = data!!.reinterpret()
    for(i in 0..listMathUI.size) {
        listMathUI[i].start_sound_thread(key[0].toInt())
    }

    return G_SOURCE_REMOVE
}

fun midi_callback(
                 timeStamp: Double, 
                 message: CArrayPointer<UByteVar>,
                 messageSize: size_t, 
                 userData: COpaquePointerVar
) = memScoped {
    initRuntimeIfNeeded()
    
    var key = message[1].toString()
    
    if( ! (message[0].toInt() != 144 || key.toInt() > 35) ) {
        var keyval = sounds[key.toInt()-21]
        
        g_idle_add(
        staticCFunction {  
            obj: CPointer<IntVar> -> midi_idle(obj)
        }.reinterpret(),
        alloc<IntVar>().apply { this.value = keyval }.reinterpret()
        )
    }

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