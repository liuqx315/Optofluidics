package com.optofluidics.app;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import ij.ImagePlus;

import java.util.Map;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.MultiThreaded;

import com.optofluidics.OptofluidicsParameters;
import com.optofluidics.OptofluidicsParameters.TrackerChoice;
import com.optofluidics.plugin.StillSubtractor_;
import com.optofluidics.plugin.StillSubtractor_.Method;
import com.optofluidics.trackmate.features.manual.EdgeSmoothedVelocityAnalyzer;
import com.optofluidics.trackmate.features.manual.MotionTypeEdgeAnalyzer;
import com.optofluidics.trackmate.features.manual.TrackPausingAnalyzer;
import com.optofluidics.trackmate.features.track.TrackLinearVelocityAnalyzer;
import com.optofluidics.trackmate.features.track.TrackSpotIntensityAnalyzer;

public class OptofluidicsTrackerProcess implements MultiThreaded, Algorithm
{

	private final ImagePlus imp;

	private final OptofluidicsParameters parameters;

	private final Logger logger;

	private Settings settings;

	private Model model;

	private TrackMate trackmate;

	private int numThreads;

	private String errorMessage;

	public OptofluidicsTrackerProcess( final ImagePlus imp, final OptofluidicsParameters parameters, final Logger logger )
	{
		this.imp = imp;
		this.parameters = parameters;
		this.logger = logger;
		setNumThreads();
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public boolean process()
	{
		logger.log( "Source image " + imp.getTitle() + ", " + imp.getWidth() + 'x' + imp.getHeight() + " with " + imp.getNFrames() + " frames.\n" );

		/*
		 * 1. Still defects subtraction.
		 */

		logger.log( "Still defects subtraction.\n" );
		final long sStart = System.currentTimeMillis();
		StillSubtractor_.subtract( imp, Method.MODE );
		final long sEnd = System.currentTimeMillis();
		logger.log( "Still defects subtraction done in " + ( sEnd - sStart ) / 1000 + " s.\n" );

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

		final double threshold = parameters.getQualityThreshold();

		logger.log( "Spot quality threshold set to be " + threshold + ".\n" );

		settings.detectorFactory = new LogDetectorFactory();
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
			errorMessage = trackmate.getErrorMessage();
			return false;
		}
		final long detectionTEnd = System.currentTimeMillis();
		final double detectionTime = ( detectionTEnd - detectionTStart ) / 1000;
		logger.log( "Detection completed in " + detectionTime + " s.\n" );

		/*
		 * 4. Spot feature calculation.
		 */

		trackmate.computeSpotFeatures( true );
		model.getSpots().setVisible( true );

		/*
		 * 5. Tracking.
		 */

		final TrackerChoice trackerChoice = parameters.getTrackerChoice();
		settings.trackerFactory = trackerChoice.getFactory();
		settings.trackerSettings = trackerChoice.getTrackerSettingsFrom( parameters );

		final long trackingTStart = System.currentTimeMillis();
		final boolean trackingOK = trackmate.execTracking();
		if ( !trackingOK )
		{
			errorMessage = trackmate.getErrorMessage();
			return false;
		}
		final long trackingTEnd = System.currentTimeMillis();
		final double trackingTime = ( trackingTEnd - trackingTStart ) / 1000;
		logger.log( "Found " + model.getTrackModel().nTracks( true ) + " tracks.\n" );
		logger.log( "Track building completed in " + trackingTime + " s.\n" );

		/*
		 * 6. Track & edge features calculation
		 */

		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		/*
		 * Track filtering.
		 */

		trackmate.execTrackFiltering( true );
		logger.log( "Kept " + model.getTrackModel().nTracks( true ) + " tracks out of " + model.getTrackModel().nTracks( false ) + ".\n" );

		return true;
	}

	public Model getModel()
	{
		return model;
	}

	public Settings getSettings()
	{
		return settings;
	}

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
	@SuppressWarnings( "rawtypes" )
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings settings = new Settings();
		settings.setFrom( imp );

		/*
		 * The minimal set of analyzers required.
		 */

		// Could rewrite this one to make it smaller.
		settings.addSpotAnalyzerFactory( new SpotIntensityAnalyzerFactory() );
		settings.addEdgeAnalyzer( new EdgeTargetAnalyzer() );
		settings.addTrackAnalyzer( new TrackIndexAnalyzer() );

		/*
		 * Optofluidics specials.
		 */

		settings.addEdgeAnalyzer( new EdgeVelocityAnalyzer() );
		settings.addEdgeAnalyzer( new EdgeTimeLocationAnalyzer() );
		settings.addEdgeAnalyzer( new EdgeSmoothedVelocityAnalyzer() );
		settings.addEdgeAnalyzer( new MotionTypeEdgeAnalyzer() );

		settings.addTrackAnalyzer( new TrackBranchingAnalyzer() );
		settings.addTrackAnalyzer( new TrackSpeedStatisticsAnalyzer() );
		settings.addTrackAnalyzer( new TrackDurationAnalyzer() );
		settings.addTrackAnalyzer( new TrackLinearVelocityAnalyzer() );
		settings.addTrackAnalyzer( new TrackSpotIntensityAnalyzer() );
		settings.addTrackAnalyzer( new TrackPausingAnalyzer() );

		/*
		 * Track filtering.
		 */

		final FeatureFilter nSpotsFilter = new FeatureFilter( TrackBranchingAnalyzer.NUMBER_SPOTS, parameters.getFilterMinNSpots(), true );
		final FeatureFilter trackDisplacementFilter = new FeatureFilter( TrackDurationAnalyzer.TRACK_DISPLACEMENT, parameters.getFilterTrackDisplacement(), true );
		settings.addTrackFilter( nSpotsFilter );
		settings.addTrackFilter( trackDisplacementFilter );

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

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}
}
