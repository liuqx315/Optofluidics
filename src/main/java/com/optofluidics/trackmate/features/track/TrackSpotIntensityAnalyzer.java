package com.optofluidics.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.util.Util;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;

@Plugin( type = TrackAnalyzer.class )
public class TrackSpotIntensityAnalyzer implements TrackAnalyzer, MultiThreaded, Benchmark
{

	/*
	 * CONSTANTS
	 */
	public static final String KEY = "TRACK_SPOT_INTENSITY";

	public static final String TRACK_MEAN_INTENSITY = "TRACK_MEAN_INTENSITY";

	public static final String TRACK_MEDIAN_INTENSITY = "TRACK_MEDIAN_INTENSITY";

	public static final String TRACK_STD_INTENSITY = "TRACK_STD_INTENSITY";

	public static final List< String > FEATURES = new ArrayList< String >( 3 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 3 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 3 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 3 );

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 3 );

	private static final String INFO_TEXT = "<html>This track analyzer computes track intensity statistics based on averaging the track spots mean intensity feature values.</html>";

	static
	{
		FEATURES.add( TRACK_MEAN_INTENSITY );
		FEATURES.add( TRACK_MEDIAN_INTENSITY );
		FEATURES.add( TRACK_STD_INTENSITY );

		FEATURE_NAMES.put( TRACK_MEAN_INTENSITY, "Track mean intensity" );
		FEATURE_NAMES.put( TRACK_MEDIAN_INTENSITY, "Track median intensity" );
		FEATURE_NAMES.put( TRACK_STD_INTENSITY, "Track intensity std" );

		FEATURE_SHORT_NAMES.put( TRACK_MEAN_INTENSITY, "Mean I" );
		FEATURE_SHORT_NAMES.put( TRACK_MEDIAN_INTENSITY, "Median I" );
		FEATURE_SHORT_NAMES.put( TRACK_STD_INTENSITY, "I std" );

		FEATURE_DIMENSIONS.put( TRACK_MEAN_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( TRACK_MEDIAN_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( TRACK_STD_INTENSITY, Dimension.INTENSITY );

		IS_INT.put( TRACK_MEAN_INTENSITY, Boolean.FALSE );
		IS_INT.put( TRACK_MEDIAN_INTENSITY, Boolean.FALSE );
		IS_INT.put( TRACK_STD_INTENSITY, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public TrackSpotIntensityAnalyzer()
	{
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{
		final String spotFeature = SpotIntensityAnalyzerFactory.MEAN_INTENSITY;

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< Integer >( trackIDs.size(), false, trackIDs );
		final FeatureModel fm = model.getFeatureModel();

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "TrackSpotFeatureAnalyzer thread " + i )
			{

				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{

						final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
						double sum = 0, sum2 = 0;
						// Others
						final double[] values = new double[ track.size() ];
						int n = 0;

						for ( final Spot spot : track )
						{
							final double val = spot.getFeature( spotFeature );

							// For median
							values[ n++ ] = val;
							// For variance and mean
							sum += val;
							sum2 += val * val;
						}

						Util.quicksort( values, 0, track.size() - 1 );
						final double median = values[ track.size() / 2 ];
						final double mean = sum / track.size();
						final double mean2 = sum2 / track.size();
						final double variance = mean2 - mean * mean;

						fm.putTrackFeature( trackID, TRACK_MEDIAN_INTENSITY, median );
						fm.putTrackFeature( trackID, TRACK_MEAN_INTENSITY, mean );
						fm.putTrackFeature( trackID, TRACK_STD_INTENSITY, Math.sqrt( variance ) );

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
	};

	@Override
	public String getKey()
	{
		return KEY;
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
