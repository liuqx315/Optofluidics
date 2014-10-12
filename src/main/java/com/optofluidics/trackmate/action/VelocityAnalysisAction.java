package com.optofluidics.trackmate.action;

import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.LoadTrackMatePlugIn_;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.util.TMUtils;

public class VelocityAnalysisAction extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>Perform the velocity macro analysis for tracks derived from Nanotweezers experiments.</html>";

	private static final String NAME = "Nanotweezers velocity macro analysis";

	private static final String KEY = "OPTOFLUIDICS_ANALYSIS";

	private static final ImageIcon ICON = new ImageIcon( VelocityAnalysisAction.class.getResource( "OptofluidicsLogo_16.png" ) );

	private static final double DEFAULT_VELOCITY_THRESHOLD = 0.5d;

	private static final int DEFAULT_MIN_CONS_FRAMES = 2;

	private static final int DEFAULT_SMOOTHING_WINDOW = 5;

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

		/*
		 * Show dialog
		 */

		Frame frame;
		if ( null == controller )
		{
			frame = null;
		}
		else
		{
			frame = controller.getGUI();
		}

		final VelocityThresholdDialog dialog = new VelocityThresholdDialog( frame, DEFAULT_VELOCITY_THRESHOLD, DEFAULT_MIN_CONS_FRAMES, DEFAULT_SMOOTHING_WINDOW, velocityUnits );
		dialog.setVisible( true );

		if ( dialog.wasCanceled() )
		{
			logger.log( "Canceled.\n" );
			return;
		}

		final double velocityThreshold = dialog.getVelocityThreshold();
		final int minConsecutiveFrames = dialog.getMinFrames();
		final int smoothingWindow = dialog.getSmoothWindow();

		/*
		 * Threshold
		 */

		logger.log( "Tresholding tracks by instantaneous velocity above " + velocityThreshold + " " + velocityUnits + " for at least " + minConsecutiveFrames + " frames, with a smoothing window of " + smoothingWindow + " frames.\n" );

		final TrackVelocityThresholder thresholder = new TrackVelocityThresholder( model, velocityThreshold, minConsecutiveFrames, smoothingWindow );
		thresholder.setLogger( logger );
		if ( !thresholder.checkInput() || !thresholder.process() )
		{
			logger.error( thresholder.getErrorMessage() );
			return;
		}

		logger.log( "Velocity thresholding done.\n" );

		/*
		 * Declare features
		 * 
		 * Now, this is going to be a bit weird: This action is going to
		 * generate new track features. They were not declared before the first
		 * time this action is run, and they are not in sync against
		 * modification of the model. Let's see how it rolls in the GUI.
		 */

		final FeatureModel fm = model.getFeatureModel();
		fm.declareTrackFeatures( FEATURES, FEATURE_NAMES, FEATURE_SHOTRT_NAMES, FEATURE_DIMENSIONS, IS_INT );

		/*
		 * Assign feature values.
		 */

		final Map< Integer, List< List< DefaultWeightedEdge >>> trackGaps = thresholder.getTrackGaps();
		final Map< Integer, List< List< DefaultWeightedEdge >>> trackRuns = thresholder.getTrackRuns();
		final Set< Integer > trackIDs = trackRuns.keySet();


		for ( final Integer id : trackIDs )
		{
			final List< List< DefaultWeightedEdge >> gaps = trackGaps.get( id );
			final List< List< DefaultWeightedEdge >> runs = trackRuns.get( id );

			// Number of pauses.
			final int nPauses = gaps.size();
			fm.putTrackFeature( id, NUMBER_OF_PAUSES, Double.valueOf( nPauses ) );

			// Mean duration of pauses.
			double totalPauseDuration = 0d;
			for ( final List< DefaultWeightedEdge > gap : gaps )
			{
				if ( gap.size() < 1 )
				{
					continue;
				}
				final DefaultWeightedEdge firstEdge = gap.get( 0 );
				final DefaultWeightedEdge lastEdge = gap.get( gap.size() - 1 );
				final double tf = fm.getEdgeFeature( firstEdge, EdgeTimeLocationAnalyzer.TIME );
				final double tl = fm.getEdgeFeature( lastEdge, EdgeTimeLocationAnalyzer.TIME );
				totalPauseDuration += ( tl - tf );
			}
			final double meanPauseDuration = totalPauseDuration / nPauses;
			fm.putTrackFeature( id, PAUSE_MEAN_DURATION, Double.valueOf( meanPauseDuration ) );

			// Mean velocity without pauses.
			double totalVelocity = 0d;
			int nVelocity = 0;
			for ( final List< DefaultWeightedEdge > run : runs )
			{
				if ( run.size() < 1 )
				{
					continue;
				}
				for ( final DefaultWeightedEdge edge : run )
				{
					final double v = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
					totalVelocity += v;
					nVelocity++;
				}
			}
			final double meanVelocity = totalVelocity / nVelocity;
			fm.putTrackFeature( id, MEAN_VELOCITY_NO_PAUSES, Double.valueOf( meanVelocity ) );
		}

		logger.log( "Additional features calculated.\n" );

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
		// final TmXmlReader reader = new TmXmlReader( file );
		// if ( !reader.isReadingOk() )
		// {
		// System.err.println( reader.getErrorMessage() );
		// // return;
		// }
		//
		// final Model model = reader.getModel();
		// if ( !reader.isReadingOk() )
		// {
		// System.err.println( reader.getErrorMessage() );
		// // return;
		// }
		//
		// final VelocityAnalysisAction action = new VelocityAnalysisAction(
		// null );
		// action.setLogger( Logger.DEFAULT_LOGGER );
		// final TrackMate trackmate = new TrackMate( model, new Settings() );
		//
		// action.execute( trackmate );

		final LoadTrackMatePlugIn_ plugin = new LoadTrackMatePlugIn_();
		plugin.run( file.getAbsolutePath() );

	}

	/*
	 * FEATURE DECLARATION
	 */

	private static final String NUMBER_OF_PAUSES = "NUMBER_OF_PAUSES";

	private static final String PAUSE_MEAN_DURATION = "PAUSE_MEAN_DURATION";

	private static final String MEAN_VELOCITY_NO_PAUSES = "MEAN_VELOCITY_NO_PAUSES";

	private static final Collection< String > FEATURES;

	private static final Map< String, String > FEATURE_NAMES;

	private static final Map< String, String > FEATURE_SHOTRT_NAMES;

	private static final Map< String, Dimension > FEATURE_DIMENSIONS;

	private static final Map< String, Boolean > IS_INT;

	static
	{
		FEATURES = new ArrayList< String >( 3 );
		FEATURES.add( NUMBER_OF_PAUSES );
		FEATURES.add( PAUSE_MEAN_DURATION );
		FEATURES.add( MEAN_VELOCITY_NO_PAUSES );

		FEATURE_NAMES = new HashMap< String, String >( 3 );
		FEATURE_NAMES.put( NUMBER_OF_PAUSES, "Number of pauses" );
		FEATURE_NAMES.put( PAUSE_MEAN_DURATION, "Mean pause duration" );
		FEATURE_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean velocity w/o pauses" );

		FEATURE_SHOTRT_NAMES = new HashMap< String, String >( 3 );
		FEATURE_SHOTRT_NAMES.put( NUMBER_OF_PAUSES, "N pauses" );
		FEATURE_SHOTRT_NAMES.put( PAUSE_MEAN_DURATION, "Pause duration" );
		FEATURE_SHOTRT_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean V. w/o pauses" );

		FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 3 );
		FEATURE_DIMENSIONS.put( NUMBER_OF_PAUSES, Dimension.NONE );
		FEATURE_DIMENSIONS.put( PAUSE_MEAN_DURATION, Dimension.TIME );
		FEATURE_DIMENSIONS.put( MEAN_VELOCITY_NO_PAUSES, Dimension.VELOCITY );

		IS_INT = new HashMap< String, Boolean >( 3 );
		IS_INT.put( NUMBER_OF_PAUSES, Boolean.TRUE );
		IS_INT.put( PAUSE_MEAN_DURATION, Boolean.FALSE );
		IS_INT.put( MEAN_VELOCITY_NO_PAUSES, Boolean.FALSE );
	}

}
