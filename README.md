# SmartSynthizer

SmartSynthizer is an initiative to do low level mathematical interface for music instruments synthesizer

# Libraries

it uses MathGL, OpenAL, Glade, RSVG, RtMIDI and GTK4

The piano key buttons are made from SVG only, if you modify the SVG take care of keeping the ID of the tags intact otherwise the buttons will not hightlight

# Work In Progress

Please read ChangeLog.txt

# Formula Explanation

${key} <br/>
pressing 'z' in computer keyboard puts zero '0'<br/>
pressing 's' in computer keyboard puts one '1'<br/>
and so on <br/>

${step} is the value in "Note Offset Math"<br/>
${sound} is the value in "Main Sound Math"<br/>
${envelop1} is the value in "Envelop1 Math"<br/>
${envelop2} is the value in "Envelop2 Math"<br/>
${envelop3} is the value in "Envelop3 Math"<br/>
${envelop4} is the value in "Envelop4 Math"<br/>

All MathGL functions will work,<br/>

x -- is the values in x axis<br/>
y -- is the values in y axis<br/>

current frequency is fixed to 440 for a piano sound<br/>
current sound quality is fixed to 44100<br/>
current sound duration is fixed to 1 second<br/>
the "1.0" in the default formula is for 1 second<br/>
current amplification value is given in formula 10000.0 which can be changed via right click copy paste<br/>

## Usage

The code is ready to execute within msys2, you have install all the libraries using `pacman`

If you have gradle in path, then invoke gradle as

     gradle assemble

If you have wrapper for linux

     ./gradlew assemble

If you have wrapper for windows

     .\gradlew assemble

Then execute bellow task to start the OpenGL application

     .\gradlew runReleaseExecutableLibgnuplot

The UI will appear with piano buttons

Press you keyboard buttons and enjoy the tones
