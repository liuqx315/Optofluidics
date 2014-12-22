# The collection of Fiji plugins for the Optofluidics devices.

http://www.opfluid.com/

## Content.

This library is meant to be distributed as an extra module of
the [Fiji](htttp://fiji.sc) software. Right now, it ships:

### Optofluidics spectrum player.

A Fiji plugin that can open conjointly an image 
sequence, and a spectrum file (in the shape of a CSV file)
and display both image and spectrum simultaneously.

### Optofluidics velocity macro analysis.

A [TrackMate](http://fiji.sc/TrackMate) action that performs
the analysis of the velocity of moving particles. It allows 
for the detection of pauses and runs in tracks, and assign
a movement type to track segments. Extra velocity features
are added to benefit from this analysis.

### Optofluidics profile viewer.

A [TrackMate](http://fiji.sc/TrackMate) view that specializes
on image sequences made of single line frames. It displays the 
profile of a frame and the kymograph of the image sequence.


## Getting it.

Binaries are distributed through the
[ImageJ updater](http://fiji.sc/How_to_set_up_and_populate_an_update_site).

Go to the menu item *Help > Update*. Update and relaunch Fiji 
until you are completely up to date. 

When done, go one more time to the updater (*Help > Update*)
but this time click on Manage update sites.

On the new window that appear, click on Add. A new line appears 
at the bottom of the sites listed. Update the two first 
columns with `Optofluidics` for name and 
`http://sites.imagej.net/Optofluidics`. 
Then click elsewhere to validate the content. 
Fiji should block a little while it is querying the update site. 

Click on Close. In the main updater window, the View option 
list should show a new site, with a single jar file in it:
`Optofluidics.jar`.

Click on *Apply changes*. Restart Fiji.
In the Plugins menu you should notice a new Optofluidics menu. 
Right now, there is only the Spectrum player.
ckMate modules will appear when running the TrackMate plugin.

