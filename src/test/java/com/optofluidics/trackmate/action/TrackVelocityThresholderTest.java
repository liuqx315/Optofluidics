package com.optofluidics.trackmate.action;

import static org.junit.Assert.fail;
import ij.ImageJ;

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
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class TrackVelocityThresholderTest
{

	@Test
	public void testBehavior()
	{
		makeModel();
		fail( "Not yet implemented" );
	}

	private void makeModel()
	{
		final boolean[] sectionIsRun = new boolean[] {
				true,
				false,
				true,
				false,
				true,
				false,
				true };
		final int[] sectionLength = new int[] {
				5, 4, 2, 5, 5, 3, 5 };

		final Model model = new Model();
		model.beginUpdate();
		try
		{
			int t = 0;
			double x = 0;
			Spot previousSpot = null;
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

		ImageJ.main( null );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, new SelectionModel( model ) );
		view.render();

		final double velocityThreshold = 1;
		final int minConsecutiveFrames = 3;
		final int smoothingWindow = 0;
		final TrackVelocityThresholder thresholder = new TrackVelocityThresholder( model, velocityThreshold, minConsecutiveFrames, smoothingWindow );
		if ( !thresholder.checkInput() || !thresholder.process() )
		{
			System.out.println( thresholder.getErrorMessage() );
			return;
		}

		final PerEdgeFeatureColorGenerator colorer = new PerEdgeFeatureColorGenerator( model, MotionTypeEdgeAnalyzer.MOVEMENT_TYPE );
		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, colorer );
		view.refresh();

		final ExportStatsToIJAction exportStatsToIJAction = new ExportStatsToIJAction();
		exportStatsToIJAction.execute( new TrackMate( model, new Settings() ) );

	}

	public static void main( final String[] args )
	{
		new TrackVelocityThresholderTest().makeModel();
	}

}
