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
public class MotionTypeEdgeAnalyzer implements EdgeAnalyzer
{

	public static final String KEY = "EDGE_MOTION_TYPE";

	public static final String MOVEMENT_TYPE = "MOVEMENT_TYPE";

	public static final Integer PAUSING = 0;

	public static final Integer RUNNING = 1;

	private static final List< String > FEATURES;

	private static final Map< String, String > FEATURE_NAMES;

	private static final Map< String, String > FEATURE_SHORT_NAMES;

	private static final Map< String, Dimension > FEATURE_DIMENSIONS;

	private static final Map< String, Boolean > IS_INT;

	private static final String INFO_TEXT = "<html>Determines and stores the motion type at the liml level. A value of 0 stands for edges that pause; of 1 for edges that run.</html>";

	private static final String NAME = "Edge motion type";

	static
	{
		FEATURES = new ArrayList< String >( 1 );
		FEATURES.add( MOVEMENT_TYPE );

		FEATURE_NAMES = new HashMap< String, String >( 1 );
		FEATURE_NAMES.put( MOVEMENT_TYPE, "Movement type" );

		FEATURE_SHORT_NAMES = new HashMap< String, String >( 3 );
		FEATURE_SHORT_NAMES.put( MOVEMENT_TYPE, "Mov. type" );

		FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );
		FEATURE_DIMENSIONS.put( MOVEMENT_TYPE, Dimension.NONE );

		IS_INT = new HashMap< String, Boolean >( 1 );
		IS_INT.put( MOVEMENT_TYPE, Boolean.TRUE );
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
