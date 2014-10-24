package com.optofluidics.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;

@Plugin( type = TrackAnalyzer.class )
public class TrackLinearVelocityAnalyzer implements TrackAnalyzer, MultiThreaded
{

	public static final String KEY = "TRACK_LINEAR_VELOCITY";

	public static final String TRACK_LINEAR_VELOCITY = "TRACK_LINEAR_VELOCITY";

	public static final List< String > FEATURES = new ArrayList< String >( 1 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 1 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 1 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 1 );

	private static final String INFO_TEXT = "<html>Computes the linear velocity for a track. <p>That is: the displacement " + "between the last and first spot, divided by the time separating these two spots.</html>";

	static
	{
		FEATURES.add( TRACK_LINEAR_VELOCITY );

		FEATURE_NAMES.put( TRACK_LINEAR_VELOCITY, "Track linear velocity" );

		FEATURE_SHORT_NAMES.put( TRACK_LINEAR_VELOCITY, "lin. velocity" );

		FEATURE_DIMENSIONS.put( TRACK_LINEAR_VELOCITY, Dimension.VELOCITY );

		IS_INT.put( TRACK_LINEAR_VELOCITY, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public TrackLinearVelocityAnalyzer()
	{
		setNumThreads();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< Integer >( trackIDs.size(), false, trackIDs );
		final FeatureModel fm = model.getFeatureModel();

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "TrackDurationAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{

						// I love brute force.
						final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
						double minT = Double.POSITIVE_INFINITY;
						double maxT = Double.NEGATIVE_INFINITY;
						Double t;
						Spot startSpot = null;
						Spot endSpot = null;
						for ( final Spot spot : track )
						{
							t = spot.getFeature( Spot.POSITION_T );
							if ( t < minT )
							{
								minT = t;
								startSpot = spot;
							}
							if ( t > maxT )
							{
								maxT = t;
								endSpot = spot;
							}
						}

						final double dt = maxT - minT;
						final double linVel = Math.sqrt( endSpot.squareDistanceTo( startSpot ) ) / dt;
						fm.putTrackFeature( trackID, TRACK_LINEAR_VELOCITY, linVel );
					}
				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
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
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}
}
