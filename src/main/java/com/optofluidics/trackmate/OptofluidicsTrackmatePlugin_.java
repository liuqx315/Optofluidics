package com.optofluidics.trackmate;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.util.Collection;

import com.optofluidics.trackmate.gui.descriptors.OptofluidicsSpotFeatureCalculationDescriptor;
import com.optofluidics.trackmate.gui.descriptors.OptofluidicsStartDialogDescriptor;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMatePlugIn_;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;

public class OptofluidicsTrackmatePlugin_ extends TrackMatePlugIn_
{

	@Override
	public void run( final String imagePath )
	{
		final ImagePlus imp;
		if ( imagePath != null && imagePath.length() > 0 )
		{
			imp = new ImagePlus( imagePath );
			if ( null == imp.getOriginalFileInfo() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not load image with path " + imagePath + "." );
				return;
			}
		}
		else
		{
			imp = WindowManager.getCurrentImage();
			if ( null == imp )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Please open an image before running TrackMate." );
				return;
			}
		}
		if ( !imp.isVisible() )
		{
			imp.setOpenAsHyperStack( true );
			imp.show();
		}
		GuiUtils.userCheckImpDimensions( imp );

		settings = createSettings( imp );
		model = createModel();
		trackmate = createTrackMate();

		/*
		 * Launch GUI.
		 */

		final TrackMateGUIController controller = new TrackMateGUIController( trackmate )
		{

			@Override
			protected Collection< WizardPanelDescriptor > createDescriptors()
			{
				final Collection< WizardPanelDescriptor > descriptors = super.createDescriptors();

				/*
				 * Start dialog. Start with a profile view.
				 */
				descriptors.remove( descriptors );
				startDialoDescriptor = new OptofluidicsStartDialogDescriptor( this );
				descriptors.add( startDialoDescriptor );

				/*
				 * View choice. Skip choice, just compute features.
				 */
				descriptors.remove( viewChoiceDescriptor );
				viewChoiceDescriptor = new OptofluidicsSpotFeatureCalculationDescriptor( getViewProvider(), getGuimodel(), this );
				descriptors.add( viewChoiceDescriptor );

				// return.
				return descriptors;
			}



		};

		;
		if ( imp != null )
		{
			GuiUtils.positionWindow( controller.getGUI(), imp.getWindow() );
		}
	}

	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OptofluidicsTrackmatePlugin_().run( "samples/Data/101.0-2015-02-13 163957_ColumnSum.tif" );
	}

}
