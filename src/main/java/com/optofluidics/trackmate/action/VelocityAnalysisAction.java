package com.optofluidics.trackmate.action;

import java.io.File;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;

public class VelocityAnalysisAction extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>Perform the velocity macro analysis for tracks derived from Nanotweezers experiments.</html>";

	private static final String NAME = "Nanotweezers velocity macro analysis";

	private static final String KEY = "OPTOFLUIDICS_ANALYSIS";

	private static final ImageIcon ICON = new ImageIcon( VelocityAnalysisAction.class.getResource( "OptofluidicsLogo_16.png" ) );

	private static final double DEFAULT_VELOCITY_THRESHOLD = 0.5d;

	private static final int DEFAULT_MIN_CONS_FRAMES = 2;

	private final TrackMateGUIController controller;

	public VelocityAnalysisAction( final TrackMateGUIController controller )
	{
		this.controller = controller;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Running analysis...\n" );

		final Model model = trackmate.getModel();
		final String velocityUnits = TMUtils.getUnitsFor( Dimension.VELOCITY, model.getSpaceUnits(), model.getTimeUnits() );

		final double velocityThreshold = DEFAULT_VELOCITY_THRESHOLD;
		final int minConsecutiveFrames = DEFAULT_MIN_CONS_FRAMES;

		logger.log( "Tresholding tracks by instantaneous velocity above " + velocityThreshold + " " + velocityUnits + " for at least " + minConsecutiveFrames + " frames.\n" );

		final TrackVelocityThresholder thresholder = new TrackVelocityThresholder( model, velocityThreshold, minConsecutiveFrames );
		thresholder.setLogger( logger );
		if ( !thresholder.checkInput() || !thresholder.process() )
		{
			logger.error( thresholder.getErrorMessage() );
			return;
		}
		logger.log( "Velocity thresholding done.\n" );

		logger.log( "Done.\n" );

	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new VelocityAnalysisAction( controller );
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

	}

	public static void main( final String[] args )
	{
		final File file = new File( "e:/Users/JeanYves/Documents/Projects/Optofluidics/MovieForTradeshow/Movie.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			// return;
		}

		final Model model = reader.getModel();
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			// return;
		}

		final VelocityAnalysisAction action = new VelocityAnalysisAction( null );
		action.setLogger( Logger.DEFAULT_LOGGER );
		final TrackMate trackmate = new TrackMate( model, new Settings() );

		action.execute( trackmate );

		// final LoadTrackMatePlugIn_ plugin = new LoadTrackMatePlugIn_();
		// plugin.run( file.getAbsolutePath() );

	}

}
