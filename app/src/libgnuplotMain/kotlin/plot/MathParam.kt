package plot

import gtk4.*

data class MathParam(
                 val step: String = "", 
                 val sound: String = "", 
                 val envelop1: String = "",
                 val envelop2: String = "",
                 val envelop3: String = "",
                 val envelop4: String = "",
                 val finalMath: String = "",
                 var key: Int = 0,
                 var sr: Int = 44100,
                 var d: Float = 1.0f,
                 var enable: gboolean = 0
)