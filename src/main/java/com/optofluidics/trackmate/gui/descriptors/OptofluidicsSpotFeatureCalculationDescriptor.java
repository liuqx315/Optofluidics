package com.optofluidics.trackmate.gui.descriptors;

import java.awt.Component;

import javax.swing.Icon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.descriptors.ViewChoiceDescriptor;
import fiji.plugin.trackmate.providers.ViewProvider;

public class OptofluidicsSpotFeatureCalculationDescriptor extends ViewChoiceDescriptor
{

	private static final String KEY = "Optofluidics spot feature calculation";

	private final LogPanel logPanel;

	private final TrackMateGUIController controller;

	public OptofluidicsSpotFeatureCalculationDescriptor( final ViewProvider viewProvider, final TrackMateGUIModel guiModel, final TrackMateGUIController controller )
	{
		super( viewProvider, guiModel, controller );
		this.controller = controller;
		this.logPanel = controller.getGUI().getLogPanel();
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public Component getComponent()
	{
		return logPanel;
	}

	@Override
	public void aboutToDisplayPanel()
	{}

	@Override
	public void displayingPanel()
	{
		final String oldText = controller.getGUI().getNextButton().getText();
		final Icon oldIcon = controller.getGUI().getNextButton().getIcon();
		controller.getGUI().getNextButton().setText( "Please wait..." );
		controller.getGUI().getNextButton().setIcon( null );
		new Thread( "TrackMate spot feature calculation thread." )
		{
			@Override
			public void run()
			{
				final TrackMate trackmate = controller.getPlugin();
				final Model model = trackmate.getModel();
				final Logger logger = model.getLogger();
				final String str = "Initial thresholding with a quality threshold above " + String.format( "%.1f", trackmate.getSettings().initialSpotFilterValue ) + " ...\n";
				logger.log( str, Logger.BLUE_COLOR );
				final int ntotal = model.getSpots().getNSpots( false );
				trackmate.execInitialSpotFiltering();
				final int nselected = model.getSpots().getNSpots( false );
				logger.log( String.format( "Retained %d spots out of %d.\n", nselected, ntotal ) );

				/*
				 * We have some spots so we need to compute spot features before
				 * we render them.
				 */
				logger.log( "Calculating spot features...\n", Logger.BLUE_COLOR );
				// Calculate features
				final long start = System.currentTimeMillis();
				trackmate.computeSpotFeatures( true );
				final long end = System.currentTimeMillis();
				logger.log( String.format( "Calculating features done in %.1f s.\n", ( end - start ) / 1e3f ), Logger.BLUE_COLOR );
				controller.getGUI().getNextButton().setText( oldText );
				controller.getGUI().getNextButton().setIcon( oldIcon );
				controller.getGUI().setNextButtonEnabled( true );
			}
		}.start();

	}

	@Override
	public void aboutToHidePanel()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void comingBackToPanel()
	{
		// TODO Auto-generated method stub

	}

}
