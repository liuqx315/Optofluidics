package com.optofluidics;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import fiji.plugin.trackmate.Logger;

public class OptofluidicsParameters
{
	private static final String PROPERTIES_FILE = "optofluidics.properties";

	public static final String KEY_PARTICLE_DIAMETER = "particle_radius";

	public static final double DEFAULT_PARTICLE_DIAMETER = 3.0;

	public static final String KEY_QUALITY_THRESHOLD = "quality_threshold";

	public static final double DEFAULT_QUALITY_THESHOLD = 5d;

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

	static
	{
		DEFAULT_PARAMETERS.setProperty( KEY_PARTICLE_DIAMETER, "" + DEFAULT_PARTICLE_DIAMETER );
		DEFAULT_PARAMETERS.setProperty( KEY_QUALITY_THRESHOLD, "" + DEFAULT_QUALITY_THESHOLD );
		DEFAULT_PARAMETERS.setProperty( KEY_TRACK_INIT_RADIUS, "" + DEFAULT_TRACK_INIT_RADIUS );
		DEFAULT_PARAMETERS.setProperty( KEY_TRACK_SEARCH_RADIUS, "" + DEFAULT_TRACK_SEARCH_RADIUS );
		DEFAULT_PARAMETERS.setProperty( KEY_MAX_FRAME_GAP, "" + DEFAULT_MAX_FRAME_GAP );
	}

	protected final Properties parameters;

	private final Logger logger;

	private double particleDiameter;

	private double qualityThreshold;

	private double trackInitRadius;

	private double trackSearchRadius;

	private int maxFrameGap;

	public OptofluidicsParameters( final Logger logger )
	{
		this.logger = logger;
		this.parameters = new Properties( DEFAULT_PARAMETERS );
		load();
	}

	private void load()
	{
		final String fijiDir = IJ.getDirectory( "imagej" );
		File file = new File( fijiDir, PROPERTIES_FILE );

		try
		{
			if ( !file.exists() )
			{
				// Look in build folder.
				file = new File( Main.class.getResource( "../../../" + PROPERTIES_FILE ).getPath() );
			}
			final InputStream stream = new FileInputStream( file );
			parameters.load( stream );
		}
		catch ( final Exception e )
		{
			logger.log( "Could not find the " + PROPERTIES_FILE + " properties file. Using default parameters.\n" );
		}

		// Unwrap
		this.particleDiameter = readDouble( KEY_PARTICLE_DIAMETER, DEFAULT_PARTICLE_DIAMETER );
		this.qualityThreshold = readDouble( KEY_QUALITY_THRESHOLD, DEFAULT_QUALITY_THESHOLD );
		this.trackInitRadius = readDouble( KEY_TRACK_INIT_RADIUS, DEFAULT_TRACK_INIT_RADIUS );
		this.trackSearchRadius = readDouble( KEY_TRACK_SEARCH_RADIUS, DEFAULT_TRACK_SEARCH_RADIUS );
		this.maxFrameGap = readInt( KEY_MAX_FRAME_GAP, DEFAULT_MAX_FRAME_GAP );
	}
	
	public void write()
	{
		OutputStream output = null;
		final String fijiDir = IJ.getDirectory( "imagej" );
		final File file = new File( fijiDir, PROPERTIES_FILE );

		try
		{

			output = new FileOutputStream( file );

			parameters.setProperty( KEY_PARTICLE_DIAMETER, "" + particleDiameter );
			parameters.setProperty( KEY_QUALITY_THRESHOLD, "" + qualityThreshold );
			parameters.setProperty( KEY_TRACK_INIT_RADIUS, "" + trackInitRadius );
			parameters.setProperty( KEY_TRACK_SEARCH_RADIUS, "" + trackSearchRadius );
			parameters.setProperty( KEY_MAX_FRAME_GAP, "" + maxFrameGap );

			// save properties to project root folder
			parameters.store( output, COMMENTS );

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

	private double readDouble( final String key, final double defaultValue )
	{
		try
		{
			return Double.parseDouble( parameters.getProperty( key ) );
		}
		catch ( final NumberFormatException nfe )
		{
			logger.error( "Could not read the " + key + " parameter. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}
	}

	private int readInt( final String key, final int defaultValue )
	{
		try
		{
			return Integer.parseInt( parameters.getProperty( key ) );
		}
		catch ( final NumberFormatException nfe )
		{
			logger.error( "Could not read the " + key + " parameter. Using default value = " + defaultValue + ".\n" );
			return defaultValue;
		}
	}

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

	public static void main( final String[] args )
	{
		new OptofluidicsParameters( Logger.DEFAULT_LOGGER ).toString();
	}
}