package com.optofluidics.trackmate.features.manual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;

@Plugin( type = TrackAnalyzer.class )
public class TrackPausingAnalyzer implements TrackAnalyzer
{

	public static final String NUMBER_OF_PAUSES = "NUMBER_OF_PAUSES";

	public static final String PAUSE_MEAN_DURATION = "PAUSE_MEAN_DURATION";

	public static final String MEAN_VELOCITY_NO_PAUSES = "MEAN_VELOCITY_NO_PAUSES";

	public static final String LINEAR_VELOCITY_NO_PAUSES = "LINEAR_VELOCITY_NO_PAUSES";

	public static final String N_SPOTS_IN_RUNS = "N_SPOTS_IN_RUNS";

	private static final List< String > FEATURES;

	private static final Map< String, String > FEATURE_NAMES;

	private static final Map< String, String > FEATURE_SHORT_NAMES;

	private static final Map< String, Dimension > FEATURE_DIMENSIONS;

	private static final Map< String, Boolean > IS_INT;

	private static final String INFO_TEXT = "<html>Defines and stores the pauses within a track. Calculation is done elsewhere.</html>";

	private static final String KEY = "TRACK_PAUSING_ANALYZER";

	private static final String NAME = "Track pausing analyzer";


	static
	{
		FEATURES = new ArrayList< String >( 5 );
		FEATURES.add( NUMBER_OF_PAUSES );
		FEATURES.add( PAUSE_MEAN_DURATION );
		FEATURES.add( MEAN_VELOCITY_NO_PAUSES );
		FEATURES.add( LINEAR_VELOCITY_NO_PAUSES );
		FEATURES.add( N_SPOTS_IN_RUNS );

		FEATURE_NAMES = new HashMap< String, String >( 4 );
		FEATURE_NAMES.put( NUMBER_OF_PAUSES, "Number of pauses" );
		FEATURE_NAMES.put( PAUSE_MEAN_DURATION, "Mean pause duration" );
		FEATURE_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean velocity w/o pauses" );
		FEATURE_NAMES.put( LINEAR_VELOCITY_NO_PAUSES, "Linear velocity w/o pauses" );
		FEATURE_NAMES.put( N_SPOTS_IN_RUNS, "N spots in run segments" );

		FEATURE_SHORT_NAMES = new HashMap< String, String >( 4 );
		FEATURE_SHORT_NAMES.put( NUMBER_OF_PAUSES, "N pauses" );
		FEATURE_SHORT_NAMES.put( PAUSE_MEAN_DURATION, "Pause duration" );
		FEATURE_SHORT_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean V. w/o pauses" );
		FEATURE_SHORT_NAMES.put( LINEAR_VELOCITY_NO_PAUSES, "Linear V. w/o pauses" );
		FEATURE_SHORT_NAMES.put( N_SPOTS_IN_RUNS, "N spots in runs" );

		FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 4 );
		FEATURE_DIMENSIONS.put( NUMBER_OF_PAUSES, Dimension.NONE );
		FEATURE_DIMENSIONS.put( PAUSE_MEAN_DURATION, Dimension.TIME );
		FEATURE_DIMENSIONS.put( MEAN_VELOCITY_NO_PAUSES, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( LINEAR_VELOCITY_NO_PAUSES, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( N_SPOTS_IN_RUNS, Dimension.NONE );

		IS_INT = new HashMap< String, Boolean >( 4 );
		IS_INT.put( NUMBER_OF_PAUSES, Boolean.TRUE );
		IS_INT.put( PAUSE_MEAN_DURATION, Boolean.FALSE );
		IS_INT.put( MEAN_VELOCITY_NO_PAUSES, Boolean.FALSE );
		IS_INT.put( LINEAR_VELOCITY_NO_PAUSES, Boolean.FALSE );
		IS_INT.put( N_SPOTS_IN_RUNS, Boolean.TRUE );
	}

	private long processingTime;

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
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return true;
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
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public int getNumThreads()
	{
		return 1;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{}

	@Override
	public boolean isLocal()
	{
		return true;
	}

}
