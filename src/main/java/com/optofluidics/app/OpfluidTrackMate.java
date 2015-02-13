package com.optofluidics.app;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.Map;

import javax.swing.SwingUtilities;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import com.optofluidics.Main;
import com.optofluidics.OptofluidicsParameters;
import com.optofluidics.trackmate.visualization.ProfileView;
import com.optofluidics.trackmate.visualization.ProfileViewHorizontalFactory;

public class OpfluidTrackMate< T extends RealType< T > & NativeType< T >> implements PlugIn
{

	/**
	 * Std scale above the mean to compute quality threshold.
	 */
	private static final double THRESHOLD_FACTOR = 2d;

	private static final String PLUGIN_NAME_STR = "OptoFluidics Tracking App";

	private static final double FUDGE_FACTOR = 500;

	protected TrackMate trackmate;

	protected Settings settings;

	protected Model model;

	protected Logger logger = Logger.IJ_LOGGER;

	protected int numThreads;

	private SelectionModel selectionModel;

	public OpfluidTrackMate()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}


	@Override
	public void run( final String imagePath )
	{
		logger.log( PLUGIN_NAME_STR + " v" + Main.OPTOFLUIDICS_LIB_VERSION + " starting.", Color.BLUE );

		/*
		 * 0. Load file and parameters.
		 */

		final OptofluidicsParameters parameters = new OptofluidicsParameters( logger );

		final ImagePlus imp;
		if ( imagePath != null && imagePath.length() > 0 )
		{
			imp = new ImagePlus( imagePath );
			if ( null == imp.getOriginalFileInfo() )
			{
				IJ.error( PLUGIN_NAME_STR + " v" + Main.OPTOFLUIDICS_LIB_VERSION, "Could not load image with path " + imagePath + "." );
				return;
			}
		}
		else
		{
			imp = WindowManager.getCurrentImage();
			if ( null == imp )
			{
				IJ.error( PLUGIN_NAME_STR + " v" + Main.OPTOFLUIDICS_LIB_VERSION, "Please open an image before running this application." );
				return;
			}
		}
		if ( !imp.isVisible() )
		{
			imp.setOpenAsHyperStack( true );
			imp.show();
		}

		/*
		 * 1. Check source image dimension.
		 */

		convertImpDimensions( imp );

		logger.log( "Found image " + imp.getShortTitle() + ", " + imp.getWidth() + 'x' + imp.getHeight() + " with " + imp.getNFrames() + " frames." );

		/*
		 * 2. Instantiate main classes.
		 */

		settings = createSettings( imp );
		model = createModel();
		trackmate = createTrackMate();

		model.setLogger( logger );

		/*
		 * 3. Detection
		 */

		final double threshold = estimateThreshold( imp );

		logger.log( "Spot quality threshold estimated to be " + threshold );

		settings.detectorFactory = new LogDetectorFactory< T >();
		final Map< String, Object > detectionSettings = settings.detectorFactory.getDefaultSettings();
		detectionSettings.put( DetectorKeys.KEY_DO_MEDIAN_FILTERING, false );
		detectionSettings.put( DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true );
		detectionSettings.put( DetectorKeys.KEY_RADIUS, parameters.getParticleDiameter() );
		detectionSettings.put( DetectorKeys.KEY_TARGET_CHANNEL, 0 );
		detectionSettings.put( DetectorKeys.KEY_THRESHOLD, parameters.getQualityThreshold() );
		settings.detectorSettings = detectionSettings;

		final long detectionTStart = System.currentTimeMillis();
		final boolean detectionOK = trackmate.execDetection();
		if ( !detectionOK )
		{
			logger.error( trackmate.getErrorMessage() );
			return;
		}
		final long detectionTEnd = System.currentTimeMillis();
		final double detectionTime = ( detectionTEnd - detectionTStart ) / 1000;
		logger.log( "Detection completed in " + detectionTime + " s." );

		/*
		 * 4. Spot feature calculation.
		 */

		trackmate.computeSpotFeatures( true );
		model.getSpots().setVisible( true );

		/*
		 * 5. Tracking.
		 */

		settings.trackerFactory = new KalmanTrackerFactory();
		final Map< String, Object > trackerSettings = settings.trackerFactory.getDefaultSettings();
		trackerSettings.put( KalmanTrackerFactory.KEY_KALMAN_SEARCH_RADIUS, parameters.getTrackSearchRadius() );
		trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP, parameters.getMaxFrameGap() );
		trackerSettings.put( TrackerKeys.KEY_LINKING_MAX_DISTANCE, parameters.getTrackInitRadius() );

//		settings.trackerFactory = new SimpleLAPTrackerFactory();
//		final Map< String, Object > trackerSettings = settings.trackerFactory.getDefaultSettings();
//		trackerSettings.put( TrackerKeys.KEY_LINKING_MAX_DISTANCE, parameters.getTrackSearchRadius() );
//		trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP, parameters.getMaxFrameGap() );
//		trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE, parameters.getTrackInitRadius() );

		settings.trackerSettings = trackerSettings;

		final long trackingTStart = System.currentTimeMillis();
		final boolean trackingOK = trackmate.execTracking();
		if ( !trackingOK )
		{
			logger.error( trackmate.getErrorMessage() );
			return;
		}
		final long trackingTEnd = System.currentTimeMillis();
		final double trackingTime = ( trackingTEnd - trackingTStart ) / 1000;
		logger.log( "Found " + model.getTrackModel().nTracks( true ) );
		logger.log( "Track building completed in " + trackingTime + " s." );

		/*
		 * 6. Track features calculation
		 */

		trackmate.computeTrackFeatures( true );

		/*
		 * 7. Visualization
		 */

		selectionModel = new SelectionModel( model );
		final ProfileView view = new ProfileViewHorizontalFactory().create( model, settings, selectionModel );

		final PerTrackFeatureColorGenerator trackColorGenerator = new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_INDEX );
		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, trackColorGenerator );


		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				view.render();
				view.getProfileOverlay().setDisplayTracks( false );
				view.getProfileOverlay().setTrackColorGenerator( trackColorGenerator );
			}
		} );

	}



	protected double estimateThreshold( final ImagePlus imp )
	{
		/*
		 * Compensated algorithm for variance estimate. Two-passes for stability.
		 */

		int n = 0;
		double sum1 = 0d;

		final int size = imp.getStackSize();
		for ( int i = 0; i < size; i++ )
		{
			final ImageProcessor processor = imp.getStack().getProcessor( i + 1 );
			for ( int j = 0; j < processor.getPixelCount(); j++ )
			{
				final float val = processor.getf( j );
				sum1 += val;
				n++;
			}
		}

		final double mean = sum1/n;

		double sum2 = 0d;
		double sum3 = 0d;
		for ( int i = 0; i < size; i++ )
		{
			final ImageProcessor processor = imp.getStack().getProcessor( i + 1 );
			for ( int j = 0; j < processor.getPixelCount(); j++ )
			{
				final float val = processor.getf( j );
				final double dx = val - mean;
				sum2 += dx * dx;
				sum3 += dx;
			}
		}
		final double variance = ( sum2 - sum3 * sum3 / n ) / ( n - 1 );
		final double std = Math.sqrt( variance );

		return ( mean + THRESHOLD_FACTOR * std ) / FUDGE_FACTOR;
	}

	protected final void convertImpDimensions( final ImagePlus imp )
	{
		final int[] dims = imp.getDimensions();
		if ( dims[ 4 ] == 1 && dims[ 3 ] > 1 )
		{

			imp.setDimensions( dims[ 2 ], dims[ 4 ], dims[ 3 ] );
			final Calibration calibration = imp.getCalibration();
			if ( calibration.frameInterval == 0 )
			{
				calibration.frameInterval = 1;
			}

		}
	}

	/*
	 * HOOKS
	 */

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Model} instance that will be used to store data in the
	 * {@link TrackMate} instance.
	 *
	 * @return a new {@link Model} instance.
	 */

	protected Model createModel()
	{
		return new Model();
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 * {@link TrackMate} instance. It is initialized by default with values
	 * taken from the current {@link ImagePlus}.
	 *
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings settings = new Settings();
		settings.setFrom( imp );

		/*
		 * The minimal set of analyzers required.
		 */

		// Could rewrite this one to make it smaller.
		settings.addSpotAnalyzerFactory( new SpotIntensityAnalyzerFactory< T >() );
		settings.addEdgeAnalyzer( new EdgeTargetAnalyzer() );
		settings.addTrackAnalyzer( new TrackIndexAnalyzer() );

		return settings;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 *
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate()
	{
		/*
		 * Since we are now sure that we will be working on this model with this
		 * settings, we need to pass to the model the units from the settings.
		 */
		final String spaceUnits = settings.imp.getCalibration().getXUnit();
		final String timeUnits = settings.imp.getCalibration().getTimeUnit();
		model.setPhysicalUnits( spaceUnits, timeUnits );

		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( numThreads );
		return trackmate;
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
		new OpfluidTrackMate().run( "samples/Pos0_ColumnSum.tif" );
	}

}
