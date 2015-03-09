package com.optofluidics;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory;
import fiji.plugin.trackmate.tracking.sparselap.SimpleSparseLAPTrackerFactory;

public class OptofluidicsParameters
{
	private static final String PROPERTIES_FILE = "optofluidics.properties";

	public static final String KEY_PARTICLE_DIAMETER = "particle_radius";

	public static final double DEFAULT_PARTICLE_DIAMETER = 10.0;

	public static final String KEY_QUALITY_THRESHOLD = "quality_threshold";

	public static final double DEFAULT_QUALITY_THESHOLD = 1d;

	public static final String KEY_TRACK_SEARCH_RADIUS = "track_search_radius";

	public static final double DEFAULT_TRACK_SEARCH_RADIUS = 4d;

	public static final String KEY_TRACK_INIT_RADIUS = "track_init_radius";

	public static final double DEFAULT_TRACK_INIT_RADIUS = 8d;

	public static final String KEY_MAX_FRAME_GAP = "max_frame_gap";

	public static final int DEFAULT_MAX_FRAME_GAP = 50;

	public static final Properties DEFAULT_PARAMETERS = new Properties();

	private static final String COMMENTS = "# Parameters for the Optofluidics applications.\n"
			+ "#\n"
			+ "# No space around '='!\n\n";

	private static final String KEY_VELOCITY_THRESHOLD = "velocity_threshold";

	private static final double DEFAULT_VELOCITY_THRESHOLD = 1e-2;

	private static final String KEY_MIN_CONSECUTIVE_FRAMES = "min_consecutive_frames";

	private static final int DEFAULT_MIN_CONSECUTIVE_FRAMES = 5;

	private static final String KEY_SMOOTHING_WINDOW = "smoothing_window";

	private static final int DEFAULT_SMOOTHING_WINDOW = 20;

	private static final String KEY_TRACKER = "tracker";

	private static final String LAP_TRACKER_KEY = "lap_tracker";

	private static final String LINEAR_MOTION_TRACKER_KEY = "linear_motion_tracker";

	private static final String DEFAULT_TRACKER = LINEAR_MOTION_TRACKER_KEY;

	private static final String KEY_FILTER_MIN_NSPOTS = "min_nspots_per_track";

	private static final int DEFAULT_FILTER_MIN_NSPOTS = 50;

	private static final String KEY_FILTER_TRACK_DISPLACEMENT = "min_track_displacement";

	private static final double DEFAULT_FILTER_TRACK_DISPLACEMENT = 5.0;

	private static final String[] FIELDS = new String[] {
			"particleDiameter",
			"qualityThreshold",
			"trackerChoice",
			"trackInitRadius",
			"trackSearchRadius",
			"maxFrameGap",
			"filterMinNSpots",
			"filterTrackDisplacement",
			"smoothingWindow",
			"velocityThreshold",
			"minConsecutiveFrames"
			};

	static
	{
		// Detection.
		DEFAULT_PARAMETERS.setProperty( KEY_PARTICLE_DIAMETER, "" + DEFAULT_PARTICLE_DIAMETER );
		DEFAULT_PARAMETERS.setProperty( KEY_QUALITY_THRESHOLD, "" + DEFAULT_QUALITY_THESHOLD );

		// Tracking.
		DEFAULT_PARAMETERS.setProperty( KEY_TRACKER, DEFAULT_TRACKER );
		DEFAULT_PARAMETERS.setProperty( KEY_TRACK_INIT_RADIUS, "" + DEFAULT_TRACK_INIT_RADIUS );
		DEFAULT_PARAMETERS.setProperty( KEY_TRACK_SEARCH_RADIUS, "" + DEFAULT_TRACK_SEARCH_RADIUS );
		DEFAULT_PARAMETERS.setProperty( KEY_MAX_FRAME_GAP, "" + DEFAULT_MAX_FRAME_GAP );

		// Track filtering.
		DEFAULT_PARAMETERS.setProperty( KEY_FILTER_MIN_NSPOTS, "" + DEFAULT_FILTER_MIN_NSPOTS);
		DEFAULT_PARAMETERS.setProperty( KEY_FILTER_TRACK_DISPLACEMENT, "" + DEFAULT_FILTER_TRACK_DISPLACEMENT );

		// Velocity analysis.
		DEFAULT_PARAMETERS.setProperty( KEY_MIN_CONSECUTIVE_FRAMES, "" + DEFAULT_MIN_CONSECUTIVE_FRAMES );
		DEFAULT_PARAMETERS.setProperty( KEY_SMOOTHING_WINDOW, "" + DEFAULT_SMOOTHING_WINDOW );
		DEFAULT_PARAMETERS.setProperty( KEY_VELOCITY_THRESHOLD, "" + DEFAULT_VELOCITY_THRESHOLD );
	}

	protected final Properties parameters;

	private final Logger logger;

	private double particleDiameter;

	private double qualityThreshold;

	private double trackInitRadius;

	private double trackSearchRadius;

	private int maxFrameGap;

	private double velocityThreshold;

	private int minConsecutiveFrames;

	private int smoothingWindow;

	private TrackerChoice trackerChoice;

	private int filterMinNSpots;

	private double filterTrackDisplacement;

	public OptofluidicsParameters( final Logger logger )
	{
		this.logger = logger;
		this.parameters = new Properties( DEFAULT_PARAMETERS );
		load();
	}

	public void load( final String parametersSetName )
	{
		final String fijiDir = IJ.getDirectory( "imagej" );
		File file = new File( fijiDir, parametersSetName );

		try
		{
			if ( !file.exists() )
			{
				// Look in build folder.
				file = new File( Main.class.getResource( "../../../" + parametersSetName ).getPath() );
			}
			final InputStream stream = new FileInputStream( file );
			parameters.load( stream );
			logger.log( "Loaded parameters from file " + file + ".\n" );
		}
		catch ( final Exception e )
		{
			logger.log( "Could not find the " + parametersSetName + " properties file. Using default parameters.\n" );
		}

		/*
		 * Unwrap
		 */

		// Particle detection
		this.particleDiameter = readDouble( KEY_PARTICLE_DIAMETER, DEFAULT_PARTICLE_DIAMETER );
		this.qualityThreshold = readDouble( KEY_QUALITY_THRESHOLD, DEFAULT_QUALITY_THESHOLD );

		// Tracking
		this.trackerChoice = TrackerChoice.fromName( parameters.getProperty( KEY_TRACKER ) );
		this.trackInitRadius = readDouble( KEY_TRACK_INIT_RADIUS, DEFAULT_TRACK_INIT_RADIUS );
		this.trackSearchRadius = readDouble( KEY_TRACK_SEARCH_RADIUS, DEFAULT_TRACK_SEARCH_RADIUS );
		this.maxFrameGap = readInt( KEY_MAX_FRAME_GAP, DEFAULT_MAX_FRAME_GAP );

		// Track filtering.
		this.filterMinNSpots = readInt( KEY_FILTER_MIN_NSPOTS, DEFAULT_FILTER_MIN_NSPOTS );
		this.filterTrackDisplacement = readDouble( KEY_FILTER_TRACK_DISPLACEMENT, DEFAULT_FILTER_TRACK_DISPLACEMENT );

		// Velocity macro analysis
		this.velocityThreshold = readDouble( KEY_VELOCITY_THRESHOLD, DEFAULT_VELOCITY_THRESHOLD );
		this.minConsecutiveFrames = readInt( KEY_MIN_CONSECUTIVE_FRAMES, DEFAULT_MIN_CONSECUTIVE_FRAMES );
		this.smoothingWindow = readInt( KEY_SMOOTHING_WINDOW, DEFAULT_SMOOTHING_WINDOW );
	}

	public void load()
	{
		load( PROPERTIES_FILE );
	}

	public void write( final String parametersSetName )
	{
		OutputStream output = null;
		String fijiDir = IJ.getDirectory( "imagej" );
		if ( null == fijiDir || !( new File( fijiDir ).exists() ) )
		{
			fijiDir = System.getProperty( "user.home" );
		}
		final File file = new File( fijiDir, parametersSetName );

		try
		{

			output = new FileOutputStream( file );

			// Particle detection.
			parameters.setProperty( KEY_PARTICLE_DIAMETER, "" + particleDiameter );
			parameters.setProperty( KEY_QUALITY_THRESHOLD, "" + qualityThreshold );

			// Tracking.
			parameters.setProperty( KEY_TRACKER, trackerChoice.toString() );
			parameters.setProperty( KEY_TRACK_INIT_RADIUS, "" + trackInitRadius );
			parameters.setProperty( KEY_TRACK_SEARCH_RADIUS, "" + trackSearchRadius );
			parameters.setProperty( KEY_MAX_FRAME_GAP, "" + maxFrameGap );

			// Track filtering.
			parameters.setProperty( KEY_FILTER_MIN_NSPOTS, "" + filterMinNSpots );
			parameters.setProperty( KEY_FILTER_TRACK_DISPLACEMENT, "" + filterTrackDisplacement );

			// Velocity macro analysis.
			parameters.setProperty( KEY_VELOCITY_THRESHOLD, "" + velocityThreshold );
			parameters.setProperty( KEY_MIN_CONSECUTIVE_FRAMES, "" + minConsecutiveFrames );
			parameters.setProperty( KEY_SMOOTHING_WINDOW, "" + smoothingWindow );

			// Save properties to project root folder.
			parameters.store( output, COMMENTS );

			logger.log( "Saved parameters to file " + file + ".\n" );
		}
		catch ( final IOException io )
		{
			io.printStackTrace();
		}
		finally
		{
			if ( output != null )
			{
				try
				{
					output.close();
				}
				catch ( final IOException e )
				{
					e.printStackTrace();
				}
			}

		}
	}

	public void write()
	{
		write( PROPERTIES_FILE );
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( "Optofluidics parameters " + super.toString() + ":\n" );
		for ( final String name : FIELDS )
		{
			Field field;
			try
			{
				field = this.getClass().getDeclaredField( name );
				field.setAccessible( true );
				final Object value = field.get( this );
				str.append( String.format( "  - %1$-25s = %2$-15s\n", name, value ) );
			}
			catch ( final SecurityException e1 )
			{
				e1.printStackTrace();
			}
			catch ( final NoSuchFieldException e1 )
			{
				e1.printStackTrace();
			}
			catch ( final IllegalArgumentException e )
			{
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				e.printStackTrace();
			}
		}
		return str.toString();
	}

	private double readDouble( final String key, final double defaultValue )
	{
		final String val = parameters.getProperty( key );
		if ( null == val )
		{
			logger.error( "The parameter " + key + " is not present in the property file. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}

		try
		{
			return Double.parseDouble( val );
		}
		catch ( final NumberFormatException nfe )
		{
			logger.error( "Could not convert the value " + val + " of the " + key + " parameter to a number. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}
	}

	private int readInt( final String key, final int defaultValue )
	{
		final String val = parameters.getProperty( key );
		if ( null == val )
		{
			logger.error( "The parameter " + key + " is not present in the property file. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}
		try
		{
			return Integer.parseInt( parameters.getProperty( key ) );
		}
		catch ( final NumberFormatException nfe )
		{
			logger.error( "Could not convert the value " + val + " of the " + key + " parameter to an integer. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}
	}

	/*
	 * Particle detection.
	 */

	public double getParticleDiameter()
	{
		return particleDiameter;
	}

	public void setParticleDiameter( final double particleDiameter )
	{
		this.particleDiameter = particleDiameter;
	}

	public double getQualityThreshold()
	{
		return qualityThreshold;
	}

	public void setQualityThreshold( final double qualityThreshold )
	{
		this.qualityThreshold = qualityThreshold;
	}

	/*
	 * Tracking.
	 */

	public TrackerChoice getTrackerChoice()
	{
		return trackerChoice;
	}

	public void setTrackerChoice( final TrackerChoice trackerChoice )
	{
		this.trackerChoice = trackerChoice;
	}

	public double getTrackInitRadius()
	{
		return trackInitRadius;
	}

	public void setTrackInitRadius( final double trackInitRadius )
	{
		this.trackInitRadius = trackInitRadius;
	}

	public double getTrackSearchRadius()
	{
		return trackSearchRadius;
	}

	public void setTrackSearchRadius( final double trackSearchRadius )
	{
		this.trackSearchRadius = trackSearchRadius;
	}

	public int getMaxFrameGap()
	{
		return maxFrameGap;
	}

	public void setMaxFrameGap( final int maxFrameGap )
	{
		this.maxFrameGap = maxFrameGap;
	}

	/*
	 * Track filtering.
	 */

	public void setFilterMinNSpots( final int filterMinNSpots )
	{
		this.filterMinNSpots = filterMinNSpots;
	}

	public int getFilterMinNSpots()
	{
		return filterMinNSpots;
	}

	public void setFilterTrackDisplacement( final double filterTrackDisplacement )
	{
		this.filterTrackDisplacement = filterTrackDisplacement;
	}

	public double getFilterTrackDisplacement()
	{
		return filterTrackDisplacement;
	}

	/*
	 * Velocity macro analysis.
	 */

	public double getVelocityThreshold()
	{
		return velocityThreshold;
	}

	public void setVelocityThreshold( final double velocityThreshold )
	{
		this.velocityThreshold = velocityThreshold;
	}

	public int getMinConsecutiveFrames()
	{
		return minConsecutiveFrames;
	}

	public void setMinConsecutiveFrames( final int minConsecutiveFrames )
	{
		this.minConsecutiveFrames = minConsecutiveFrames;
	}

	public int getSmoothingWindow()
	{
		return smoothingWindow;
	}

	public void setSmoothingWindow( final int smoothingWindow )
	{
		this.smoothingWindow = smoothingWindow;
	}

	/*
	 * INNER CLASSES.
	 */

	public static enum TrackerChoice
	{
		LAP_TRACKER( OptofluidicsParameters.LAP_TRACKER_KEY ),
		LINEAR_MOTION_TRACKER( OptofluidicsParameters.LINEAR_MOTION_TRACKER_KEY );

		private String name;

		private TrackerChoice( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public static TrackerChoice fromName( final String name )
		{
			for ( final TrackerChoice el : values() )
			{
				if ( el.toString().equalsIgnoreCase( name ) ) { return el; }
			}
			return LAP_TRACKER;
		}

		public static TrackerChoice fromFactoryKey( final String factoryKey )
		{
			for ( final TrackerChoice el : values() )
			{
				if ( el.getFactory().getKey().equals( factoryKey ) ) { return el; }
			}
			return LAP_TRACKER;
		}

		public SpotTrackerFactory getFactory()
		{
			switch ( this )
			{
			case LINEAR_MOTION_TRACKER:
				return new KalmanTrackerFactory();
			default:
			case LAP_TRACKER:
				return new SimpleSparseLAPTrackerFactory();

			}
		}

		public Map< String, Object > getTrackerSettingsFrom( final OptofluidicsParameters parameters )
		{
			final Map< String, Object > trackerSettings = this.getFactory().getDefaultSettings();
			switch ( this )
			{
			case LINEAR_MOTION_TRACKER:
				trackerSettings.put( KalmanTrackerFactory.KEY_KALMAN_SEARCH_RADIUS, parameters.getTrackSearchRadius() );
				trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP, parameters.getMaxFrameGap() );
				trackerSettings.put( TrackerKeys.KEY_LINKING_MAX_DISTANCE, parameters.getTrackInitRadius() );
				break;
			default:
			case LAP_TRACKER:
				trackerSettings.put( TrackerKeys.KEY_LINKING_MAX_DISTANCE, parameters.getTrackSearchRadius() );
				trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP, parameters.getMaxFrameGap() );
				trackerSettings.put( TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE, parameters.getTrackInitRadius() );
				break;
			}
			return trackerSettings;
		}
	}

	/*
	 * MAIN METHOD.
	 */

	public static void main( final String[] args )
	{
		System.out.println( new OptofluidicsParameters( Logger.DEFAULT_LOGGER ).toString() );
	}
}