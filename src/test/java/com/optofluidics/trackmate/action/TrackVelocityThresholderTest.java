package com.optofluidics.trackmate.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import ij.ImageJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

import com.optofluidics.trackmate.features.manual.EdgeSmoothedVelocityAnalyzer;
import com.optofluidics.trackmate.features.manual.MotionTypeEdgeAnalyzer;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.util.ModelTools;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class TrackVelocityThresholderTest
{


	private Model model;

	private List< boolean[] > expectedIsRuns;

	private List< int[] > expectedLengths;

	@Test
	public void testBehavior()
	{
		createModel();
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
		final List< Integer > ids = new ArrayList< Integer >( trackIDs );
		Collections.sort( ids, ModelTools.featureTrackComparator( TrackDurationAnalyzer.TRACK_START, model.getFeatureModel() ) );

		int index = 0;
		for ( final Integer id : ids )
		{
			final Set< DefaultWeightedEdge > trackEdges = model.getTrackModel().trackEdges( id );
			final List< DefaultWeightedEdge > edges = new ArrayList< DefaultWeightedEdge >( trackEdges );
			Collections.sort( edges, ModelTools.featureEdgeComparator( EdgeTimeLocationAnalyzer.TIME, model.getFeatureModel() ) );

			final Iterator< DefaultWeightedEdge > iterator = edges.iterator();
			final boolean[] expectedIsRun = expectedIsRuns.get( index );
			final int[] expectedLength = expectedLengths.get( index );

			for ( int i = 0; i < expectedLength.length; i++ )
			{
				final boolean isRun = expectedIsRun[ i ];
				final int length = expectedLength[ i ];
				for ( int j = 0; j < length; j++ )
				{
					final DefaultWeightedEdge edge = iterator.next();
					final Double val = model.getFeatureModel().getEdgeFeature( edge, MotionTypeEdgeAnalyzer.MOVEMENT_TYPE );
					if ( isRun )
					{
						assertEquals( "Expected motion type RUN for edge " + edge + " but was " + val, MotionTypeEdgeAnalyzer.RUNNING, val );
					}
					else
					{
						assertEquals( "Expected motion type PAUSE for edge " + edge + " but was " + val, MotionTypeEdgeAnalyzer.PAUSING, val );
					}
				}
			}
			index++;
		}
	}

	private Model createModel()
	{
		final double velocityThreshold = 1.2d;
		final int minConsecutiveFrames = 3;
		final int smoothingWindow = 0;

		final List< boolean[] > sectionIsRuns = new ArrayList< boolean[] >();
		final List< int[] > sectionLengths = new ArrayList< int[] >();
		{
			// 1.
			final boolean[] sectionIsRun1 = new boolean[] { true, false, true, false, true, false, true };
			final int[] sectionLength1 = new int[] { 5, 4, 2, 5, 5, 2, 5 };
			sectionIsRuns.add( sectionIsRun1 );
			sectionLengths.add( sectionLength1 );

			// 2.
			final boolean[] sectionIsRun2 = new boolean[] { true, false, true, false, true, false, true };
			final int[] sectionLength2 = new int[] { 2, 4, 2, 5, 5, 2, 5 };
			sectionIsRuns.add( sectionIsRun2 );
			sectionLengths.add( sectionLength2 );

			// 3.
			final boolean[] sectionIsRun3 = new boolean[] { false, true, false, true, false, true };
			final int[] sectionLength3 = new int[] { 2, 4, 2, 5, 5, 2 };
			sectionIsRuns.add( sectionIsRun3 );
			sectionLengths.add( sectionLength3 );

			// 4.
			final boolean[] sectionIsRun4 = new boolean[] { true };
			final int[] sectionLength4 = new int[] { 10 };
			sectionIsRuns.add( sectionIsRun4 );
			sectionLengths.add( sectionLength4 );

			// 5.
			final boolean[] sectionIsRun5 = new boolean[] { true };
			final int[] sectionLength5 = new int[] { 2 };
			sectionIsRuns.add( sectionIsRun5 );
			sectionLengths.add( sectionLength5 );

			// 6.
			final boolean[] sectionIsRun6 = new boolean[] { false };
			final int[] sectionLength6 = new int[] { 10 };
			sectionIsRuns.add( sectionIsRun6 );
			sectionLengths.add( sectionLength6 );

			// 7.
			final boolean[] sectionIsRun7 = new boolean[] { false };
			final int[] sectionLength7 = new int[] { 2 };
			sectionIsRuns.add( sectionIsRun7 );
			sectionLengths.add( sectionLength7 );

//			// 8.
//			final boolean[] sectionIsRun8 = new boolean[] { true, false, true, false, true, false, true };
//			final int[] sectionLength8 = new int[] { 2, 2, 2, 5, 5, 2, 5 };
//			sectionIsRuns.add( sectionIsRun8 );
//			sectionLengths.add( sectionLength8 );

			// 9.
			final boolean[] sectionIsRun9 = new boolean[] { true, false, true, false, true, false, true, false };
			final int[] sectionLength9 = new int[] { 5, 4, 2, 5, 5, 2, 5, 2 };
			sectionIsRuns.add( sectionIsRun9 );
			sectionLengths.add( sectionLength9 );

			// 10.
			final boolean[] sectionIsRun10 = new boolean[] { true, false, true, false, true, false, true, false };
			final int[] sectionLength10 = new int[] { 5, 4, 2, 5, 5, 2, 2, 2 };
			sectionIsRuns.add( sectionIsRun10 );
			sectionLengths.add( sectionLength10 );
		}

		expectedIsRuns = new ArrayList< boolean[] >();
		expectedLengths = new ArrayList< int[] >();

		model = new Model();
		model.beginUpdate();
		try
		{
			int t = 0;

			for ( int k = 0; k < sectionIsRuns.size(); k++ )
			{
				final int[] sectionLength = sectionLengths.get( k );
				final boolean[] sectionIsRun = sectionIsRuns.get( k );

				boolean[] expectedIsRun = new boolean[ sectionIsRun.length ];
				expectedIsRun[ 0 ] = sectionIsRun[ 0 ];
				int[] expectedSectionLength = new int[ sectionLength.length ];

				if ( sectionIsRun.length < 2 )
				{
					expectedIsRun[ 0 ] = sectionIsRun[ 0 ];
					expectedSectionLength[ 0 ] = sectionLength[ 0 ];
				}
				else
				{

					int is = 0;
					int index = 0;
					while ( is < sectionLength.length && sectionLength[ is ] < minConsecutiveFrames )
					{
						expectedSectionLength[ index ] += sectionLength[ is ];
						expectedIsRun[ index ] = !sectionIsRun[ is ];
						is++;
					}

					for ( int i = is; i < sectionIsRun.length; i++ )
					{
						if ( sectionLength[ i ] > minConsecutiveFrames )
						{
							if ( index < 0 || sectionIsRun[ i ] != expectedIsRun[ index ] )
							{
								index++;
							}
							expectedIsRun[ index ] = sectionIsRun[ i ];
							expectedSectionLength[ index ] += sectionLength[ i ];
						}
						else
						{
							if ( index < 0 )
							{
								index++;
							}
							expectedSectionLength[ index ] += sectionLength[ i ];
						}
					}

					expectedSectionLength = Arrays.copyOf( expectedSectionLength, index + 1 );
					expectedIsRun = Arrays.copyOf( expectedIsRun, index + 1 );

				}
				expectedSectionLength[ 0 ]--;
				expectedIsRuns.add( expectedIsRun );
				expectedLengths.add( expectedSectionLength );

				Spot previousSpot = null;
				double x = 0;

				for ( int i = 0; i < sectionLength.length; i++ )
				{

					final boolean isRun = sectionIsRun[ i ];
					double velocity;
					if ( isRun )
					{
						velocity = 1;
					}
					else
					{
						velocity = 0;
					}

					for ( int j = 0; j < sectionLength[ i ]; j++ )
					{
						x += velocity;
						final Spot spot = new Spot( x, t, 0, 1, 1 );
						spot.putFeature( Spot.POSITION_T, Double.valueOf( t ) );
						model.addSpotTo( spot, t );
						if ( previousSpot != null )
						{
							model.addEdge( previousSpot, spot, 1 );
						}
						previousSpot = spot;
						t++;
					}
				}
			}
		}
		finally
		{
			model.endUpdate();
		}

		model.getFeatureModel().declareEdgeFeatures( EdgeTimeLocationAnalyzer.FEATURES, EdgeTimeLocationAnalyzer.FEATURE_NAMES, EdgeTimeLocationAnalyzer.FEATURE_SHORT_NAMES, EdgeTimeLocationAnalyzer.FEATURE_DIMENSIONS, EdgeTimeLocationAnalyzer.IS_INT );
		model.getFeatureModel().declareEdgeFeatures( EdgeVelocityAnalyzer.FEATURES, EdgeVelocityAnalyzer.FEATURE_NAMES, EdgeVelocityAnalyzer.FEATURE_SHORT_NAMES, EdgeVelocityAnalyzer.FEATURE_DIMENSIONS, EdgeVelocityAnalyzer.IS_INT );
		model.getFeatureModel().declareEdgeFeatures( MotionTypeEdgeAnalyzer.FEATURES, MotionTypeEdgeAnalyzer.FEATURE_NAMES, MotionTypeEdgeAnalyzer.FEATURE_SHORT_NAMES, MotionTypeEdgeAnalyzer.FEATURE_DIMENSIONS, MotionTypeEdgeAnalyzer.IS_INT );
		model.getFeatureModel().declareEdgeFeatures( EdgeSmoothedVelocityAnalyzer.FEATURES, EdgeSmoothedVelocityAnalyzer.FEATURE_NAMES, EdgeSmoothedVelocityAnalyzer.FEATURE_SHORT_NAMES, EdgeSmoothedVelocityAnalyzer.FEATURE_DIMENSIONS, EdgeSmoothedVelocityAnalyzer.IS_INT );

		final EdgeTimeLocationAnalyzer timeLocationAnalyzer = new EdgeTimeLocationAnalyzer();
		timeLocationAnalyzer.process( model.getTrackModel().edgeSet(), model );

		final EdgeVelocityAnalyzer velocityAnalyzer = new EdgeVelocityAnalyzer();
		velocityAnalyzer.process( model.getTrackModel().edgeSet(), model );

		final TrackDurationAnalyzer durationAnalyzer = new TrackDurationAnalyzer();
		durationAnalyzer.process( model.getTrackModel().trackIDs( true ), model );

		final TrackVelocityThresholder thresholder = new TrackVelocityThresholder( model, velocityThreshold, minConsecutiveFrames, smoothingWindow );
		if ( !thresholder.checkInput() || !thresholder.process() )
		{
			fail( thresholder.getErrorMessage() );
		}
		return model;
	}


	private void showResults()
	{
		final Model model = createModel();

		ImageJ.main( null );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, new SelectionModel( model ) );
		view.render();

		final PerEdgeFeatureColorGenerator colorer = new PerEdgeFeatureColorGenerator( model, MotionTypeEdgeAnalyzer.MOVEMENT_TYPE );
		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, colorer );
		view.refresh();

		final ExportStatsToIJAction exportStatsToIJAction = new ExportStatsToIJAction();
		exportStatsToIJAction.execute( new TrackMate( model, new Settings() ) );

	}

	public static void main( final String[] args )
	{
		new TrackVelocityThresholderTest().showResults();
	}

}
