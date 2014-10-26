package com.optofluidics.trackmate.features.manual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeSmoothedVelocityAnalyzer implements EdgeAnalyzer
{

	public static final String KEY = "SMOOTHED_VELOCITY";

	public static final String SMOOTHED_VELOCITY = "SMOOTHED_VELOCITY";

	private static final List< String > FEATURES;

	private static final Map< String, String > FEATURE_NAMES;

	private static final Map< String, String > FEATURE_SHORT_NAMES;

	private static final Map< String, Dimension > FEATURE_DIMENSIONS;

	private static final Map< String, Boolean > IS_INT;

	private static final String INFO_TEXT = "<html>Return the smoothed velocities over the neighbor edges within the current track.</html>";

	private static final String NAME = "Smoothed velocity";

	static
	{
		FEATURES = new ArrayList< String >( 1 );
		FEATURES.add( SMOOTHED_VELOCITY );

		FEATURE_NAMES = new HashMap< String, String >( 1 );
		FEATURE_NAMES.put( SMOOTHED_VELOCITY, "Smoothed velocity" );

		FEATURE_SHORT_NAMES = new HashMap< String, String >( 3 );
		FEATURE_SHORT_NAMES.put( SMOOTHED_VELOCITY, "Sm. vlocity" );

		FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );
		FEATURE_DIMENSIONS.put( SMOOTHED_VELOCITY, Dimension.VELOCITY );

		IS_INT = new HashMap< String, Boolean >( 1 );
		IS_INT.put( SMOOTHED_VELOCITY, Boolean.FALSE );
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
	public void process( final Collection< DefaultWeightedEdge > edges, final Model model )
	{}

	@Override
	public boolean isLocal()
	{
		return true;
	}
}
