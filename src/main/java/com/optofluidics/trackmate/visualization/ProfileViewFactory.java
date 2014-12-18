package com.optofluidics.trackmate.visualization;

import ij.ImageJ;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import com.optofluidics.Main;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMatePlugIn_;
import fiji.plugin.trackmate.visualization.ViewFactory;

@Plugin( type = ViewFactory.class )
public class ProfileViewFactory implements ViewFactory
{

	private static final String INFO_TEXT = "<html>"
			+ "This is a specialized view for line images: Image sequences "
			+ "with all frames made of just 1 line. The frame raw data is displayed "
			+ "as an intensity profile, and a kymograph is displayed next. Spots and "
			+ "track are shown on the profile window, and tracks on the kymograph."
			+ "<p>"
			+ "The view does not allow interacting with the data. "
			+ "<p>"
			+ "Launching this view with a source image data that is ont made"
			+ "of single line frames will generate an error."
			+ "</html>";

	private static final String NAME = "Profile viewer";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return Main.OPTOFLUIDICS_ICON;
	}

	@Override
	public String getKey()
	{
		return ProfileView.KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public ProfileView create( final Model model, final Settings settings, final SelectionModel selectionModel )
	{
		return new ProfileView( model, selectionModel, settings.imp );
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new TrackMatePlugIn_().run( "samples/SUM_FakeTracks.tif" );
	}
}
