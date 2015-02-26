package com.optofluidics.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import com.optofluidics.trackmate.visualization.KymographGenerator;

public class Kymograph_ implements PlugIn
{

	@Override
	public void run( final String arg )
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp.getHeight() != 1 )
		{
			IJ.error( "Kymograph_ plugin only accepts line image squences. Got a " + imp.getWidth() + " x " + imp.getHeight() + " image sequence." );
			return;
		}

		if ( arg == null || arg.length() == 0 || arg.equalsIgnoreCase( "horizontal" ) )
		{
			KymographGenerator.fromLineImageHorizontal( imp ).show();
		}
		else
		{
			KymographGenerator.fromLineImageVertical( imp ).show();
		}
	}

}
