package com.optofluidics.trackmate.action;

import static com.optofluidics.Main.OPTOFLUIDICS_ICON;
import static com.optofluidics.Main.OPTOFLUIDICS_LIB_VERSION;
import ij.ImageJ;

import java.awt.Frame;
import java.io.File;
import java.util.Iterator;

import javax.swing.ImageIcon;

import net.imglib2.util.Util;

import org.scijava.plugin.Plugin;

import com.optofluidics.trackmate.features.manual.EdgeSmoothedVelocityAnalyzer;
import com.optofluidics.trackmate.features.manual.MotionTypeEdgeAnalyzer;
import com.optofluidics.trackmate.features.manual.TrackPausingAnalyzer;
import com.optofluidics.trackmate.features.track.TrackLinearVelocityAnalyzer;
import com.optofluidics.trackmate.features.track.TrackSpotIntensityAnalyzer;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.LoadTrackMatePlugIn_;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.TrackFeatureCalculator;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.util.TMUtils;

public class VelocityAnalysisAction extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>" +
			"Perform the velocity macro analysis for tracks "
			+ "derived from Nanotweezers experiments."
			+ "<p>"
			+ "Library version: " + OPTOFLUIDICS_LIB_VERSION + "</html>";

	private static final String NAME = "Nanotweezers velocity macro analysis";

	private static final String KEY = "OPTOFLUIDICS_ANALYSIS";

	private static final double DEFAULT_VELOCITY_THRESHOLD = 0.5d;

	private static final int DEFAULT_MIN_CONS_FRAMES = 2;

	private static final int DEFAULT_SMOOTHING_WINDOW = 5;

	private final TrackMateGUIController controller;

	private static int smoothingWindow = DEFAULT_SMOOTHING_WINDOW;

	private static int minConsecutiveFrames = DEFAULT_MIN_CONS_FRAMES;

	private static double velocityThreshold = DEFAULT_VELOCITY_THRESHOLD;

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
		 * Determine best threshold.
		 */

		logger.log( "Determining velocity threshold estimate...\n" );
		final double[] linVels = new double[ model.getTrackModel().nTracks( true ) ];
		final Iterator< Integer > iterator = model.getTrackModel().trackIDs( true ).iterator();
		for ( int i = 0; i < linVels.length; i++ )
		{
			final Integer id = iterator.next();
			final double val = model.getFeatureModel().getTrackFeature( id, TrackLinearVelocityAnalyzer.TRACK_LINEAR_VELOCITY ).doubleValue();
			linVels[ i ] = val;
		}
		final double estimate = Util.median( linVels );
		logger.log( "Found threshold estimate = " + estimate + " " + velocityUnits + '\n' );
		velocityThreshold = estimate;

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

		final VelocityThresholdDialog dialog = new VelocityThresholdDialog( frame, velocityThreshold, minConsecutiveFrames, smoothingWindow, velocityUnits );
		dialog.setVisible( true );

		if ( dialog.wasCanceled() )
		{
			logger.log( "Canceled.\n" );
			return;
		}

		velocityThreshold = dialog.getVelocityThreshold();
		minConsecutiveFrames = dialog.getMinFrames();
		smoothingWindow = dialog.getSmoothWindow();

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
		 * Recalculate features
		 */

		logger.log( "Recalculating features.\n" );
		controller.getPlugin().computeEdgeFeatures( true );
		controller.getPlugin().computeTrackFeatures( true );
		logger.log( "Recalculating features done.\n" );

		/*
		 * Export
		 */

		final VelocityAnalysisExporter exporter = new VelocityAnalysisExporter( model, controller.getSelectionModel() );
		exporter.exportToImageJTable();

		logger.log( "Created results table.\n" );

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
			return OPTOFLUIDICS_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

	}

	public static void main( final String[] args )
	{
		// final File file = new File( "../Pos0.xml" );
		final File file = new File( "/Users/JeanYves/Google Drive/Optofluidics/TestVelocityAnalysis.xml" );
		//		final File file = new File( "e:/Users/JeanYves/Documents/Projects/Optofluidics/MovieForTradeshow/Movie.xml" );

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

		ImageJ.main( args );

		final LoadTrackMatePlugIn_ plugin = new LoadTrackMatePlugIn_()
		{
			/*
			 * Ensure in this snippet that custom track analyzer are triggered.
			 */

			@Override
			protected void postRead( final TrackMate trackmate )
			{
				final Settings settings = trackmate.getSettings();
				settings.addTrackAnalyzer( new TrackLinearVelocityAnalyzer() );
				settings.addTrackAnalyzer( new TrackSpotIntensityAnalyzer() );
				settings.addTrackAnalyzer( new TrackPausingAnalyzer() );
				new TrackFeatureCalculator( trackmate.getModel(), settings ).process();

				settings.addEdgeAnalyzer( new MotionTypeEdgeAnalyzer() );
				settings.addEdgeAnalyzer( new EdgeSmoothedVelocityAnalyzer() );
				new EdgeFeatureCalculator( trackmate.getModel(), settings ).process();
			}

		};

		//		final File file = new File(
		//				"/Users/tinevez/Google Drive/Optofluidics/TestVelocityAnalysis.xml"
		//				);
		plugin.run( file.getAbsolutePath() );

		//		final File imfile = new File( "../Pos0_full.tif" );
		////		final File imfile = new File( "/Users/JeanYves/Google Drive/Optofluidics/TestVelocityAnalysis.tif" );
		//		new TrackMatePlugIn_().run( imfile.getAbsolutePath() );

	}

}
