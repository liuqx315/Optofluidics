package com.optofluidics.trackmate.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import net.imglib2.algorithm.Algorithm;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.optofluidics.util.MovingAverage;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

public class TrackVelocityThresholder implements Algorithm
{


	private static final String BASE_ERR_MSG = "[TrackVelocityThresholder] ";

	private final Model model;

	private final double velocityThreshold;

	private final int minConsecutiveFrames;

	private final int smoothingWindow;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final Comparator< ? super DefaultWeightedEdge > edgeTimeComparator;


	public TrackVelocityThresholder( final Model model, final double velocityThreshold, final int minConsecutiveFrames, final int smoothingWindow )
	{
		this.model = model;
		this.velocityThreshold = velocityThreshold;
		this.minConsecutiveFrames = minConsecutiveFrames;
		this.smoothingWindow = smoothingWindow;
		this.edgeTimeComparator = new EdgeTimeComparator( model.getFeatureModel() );
	}

	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	@Override
	public boolean checkInput()
	{
		if ( velocityThreshold <= 0 )
		{
			errorMessage = BASE_ERR_MSG + "Velocity threshold is negative or null.";
			return false;
		}
		if ( minConsecutiveFrames <= 0 )
		{
			errorMessage = BASE_ERR_MSG + "Min consecutive frame is negative or null.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{
		final TrackModel trackModel = model.getTrackModel();
		final FeatureModel fm = model.getFeatureModel();

		/*
		 * Declare features
		 * 
		 * Now, this is going to be a bit weird: This action is going to
		 * generate new track features. They were not declared before the first
		 * time this action is run, and they are not in sync against
		 * modification of the model. Let's see how it rolls in the GUI.
		 */

		fm.declareTrackFeatures( FEATURES, FEATURE_NAMES, FEATURE_SHOTRT_NAMES, FEATURE_DIMENSIONS, IS_INT );

		final Set< Integer > trackIDs = trackModel.unsortedTrackIDs( true );
		// final Integer id = trackIDs.iterator().next();
		for ( final Integer id : trackIDs )
		{
			final Set< DefaultWeightedEdge > edges = trackModel.trackEdges( id );
			final List< DefaultWeightedEdge > ledges = new ArrayList< DefaultWeightedEdge >( edges );
			Collections.sort( ledges, edgeTimeComparator );

			// Collect raw velocities
			final double[] velocities = new double[ ledges.size() ];
			for ( int i = 0; i < ledges.size(); i++ )
			{
				final DefaultWeightedEdge edge = ledges.get( i );
				final double vel = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
				velocities[ i ] = vel;
			}

			// Smooth and threshold
			final List< List< DefaultWeightedEdge >> gaps = new ArrayList< List< DefaultWeightedEdge > >();
			final List< List< DefaultWeightedEdge >> runs = new ArrayList< List< DefaultWeightedEdge > >();
			List< DefaultWeightedEdge > gap = null;
			List< DefaultWeightedEdge > run = new ArrayList< DefaultWeightedEdge >();

			boolean inGap = false;
			int nImmobile = 0;

			if ( velocities.length > smoothingWindow )
			{
				// Use moving average
				final MovingAverage avg = new MovingAverage( velocities, smoothingWindow );
				for ( int i = 0; i < velocities.length; i++ )
				{
					final double v = avg.next();
					final DefaultWeightedEdge edge = ledges.get( i );

					if ( v < velocityThreshold )
					{
						if ( inGap )
						{
							// Already in gap.
							nImmobile++;
						}
						else
						{
							// New gap.
							inGap = true;
							nImmobile = 1;
							gap = new ArrayList< DefaultWeightedEdge >();
						}

						gap.add( edge );

					}
					else
					{

						if ( inGap )
						{
							// Was in a gap, and leaving it.
							inGap = false;
							if ( nImmobile > minConsecutiveFrames )
							{
								// Gap was long enough; we can store it.
								gaps.add( gap );
								// We are now also sure that the previous run is
								// over and can store it.
								runs.add( run );
								run = new ArrayList< DefaultWeightedEdge >();
							}
							else
							{
								// Gap was not long enough. We add past edges to
								// the run, and carry on with the run.
								run.addAll( gap );
							}
						}

						run.add( edge );
					}
				}
				if ( null != gap && !gaps.contains( gap ) )
				{
					gaps.add( gap );
				}
				if ( null != run && !runs.contains( run ) )
				{
					runs.add( run );
				}

			}
			else
			{
				// Track too short, make it one segment and don't use moving
				// average.
				double totalVelocity = 0;
				int nVelocity = 0;
				for ( final DefaultWeightedEdge edge : ledges )
				{
					final double v = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
					totalVelocity += v;
					nVelocity++;
				}
				final double meanVelocity = totalVelocity / nVelocity;
				if ( meanVelocity > velocityThreshold )
				{
					runs.add( ledges );
				}
				else
				{
					gaps.add( ledges );
				}
			}

			/*
			 * Assign feature values.
			 */

			// Number of pauses.
			final int nPauses = gaps.size();
			fm.putTrackFeature( id, NUMBER_OF_PAUSES, Double.valueOf( nPauses ) );

			// Mean duration of pauses.
			double totalPauseDuration = 0d;
			for ( final List< DefaultWeightedEdge > gap2 : gaps )
			{
				if ( gap2.size() < 1 )
				{
					continue;
				}
				final DefaultWeightedEdge firstEdge = gap2.get( 0 );
				final DefaultWeightedEdge lastEdge = gap2.get( gap2.size() - 1 );
				final double tf = fm.getEdgeFeature( firstEdge, EdgeTimeLocationAnalyzer.TIME );
				final double tl = fm.getEdgeFeature( lastEdge, EdgeTimeLocationAnalyzer.TIME );
				totalPauseDuration += ( tl - tf );
			}
			final double meanPauseDuration = totalPauseDuration / nPauses;
			fm.putTrackFeature( id, PAUSE_MEAN_DURATION, Double.valueOf( meanPauseDuration ) );

			// Mean velocity without pauses.
			double totalVelocity = 0d;
			int nVelocity = 0;
			for ( final List< DefaultWeightedEdge > run2 : runs )
			{
				if ( run2.size() < 1 )
				{
					continue;
				}
				for ( final DefaultWeightedEdge edge : run2 )
				{
					final double v = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
					totalVelocity += v;
					nVelocity++;
				}
			}
			final double meanVelocity = totalVelocity / nVelocity;
			fm.putTrackFeature( id, MEAN_VELOCITY_NO_PAUSES, Double.valueOf( meanVelocity ) );

			/*
			 * Log
			 */

			String str;
			if ( gaps.size() > 0 && runs.size() > 0 )
			{
				str = "Track " + trackModel.name( id ) + " has ";
				if ( gaps.size() > 1 )
				{
					str += gaps.size() + " pauses and ";
				}
				else
				{
					str += "1 pause and ";
				}
				if ( runs.size() > 1 )
				{
					str += runs.size() + " runs.\n";
				}
				else
				{
					str += "1 run.\n";
				}
			}
			else if ( gaps.size() == 0 )
			{
				str = "Track " + trackModel.name( id ) + " has no pauses and " + runs.size() + " run.\n";
			}
			else
			{
				str = "Track " + trackModel.name( id ) + " has " + gaps.size() + " pause and no runs.\n";
			}
			logger.log( str );
		}
		return true;
	}

	private void debug( final List< DefaultWeightedEdge > ledges, final List< List< DefaultWeightedEdge >> runs, final List< List< DefaultWeightedEdge >> gaps )
	{
		final FeatureModel fm = model.getFeatureModel();

		// Collect raw velocities
		final double[] x = new double[ ledges.size() ];
		final double[] t = new double[ ledges.size() ];
		for ( int i = 0; i < ledges.size(); i++ )
		{
			final DefaultWeightedEdge edge = ledges.get( i );
			final double vel = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
			final double time = fm.getEdgeFeature( edge, EdgeTimeLocationAnalyzer.TIME );
			x[ i ] = vel;
			t[ i ] = time;
		}

		final double[] s = new double[ x.length ];
		final MovingAverage avg = new MovingAverage( x, smoothingWindow );
		for ( int i = 0; i < s.length; i++ )
		{
			s[ i ] = avg.next();
		}

		final DefaultXYDataset dataset = new DefaultXYDataset();
		// dataset.addSeries( "Raw", new double[][] { t, x } );
		dataset.addSeries( "Smooth", new double[][] { t, s } );

		int index = 0;
		for ( final List< DefaultWeightedEdge > run : runs )
		{
			index++;
			final double[] xr = new double[ run.size() ];
			final double[] tr = new double[ run.size() ];
			for ( int i = 0; i < run.size(); i++ )
			{
				final DefaultWeightedEdge edge = run.get( i );
				final double vel = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
				final double time = fm.getEdgeFeature( edge, EdgeTimeLocationAnalyzer.TIME );
				xr[ i ] = vel;
				tr[ i ] = time;
			}
			dataset.addSeries( "Run " + index, new double[][] { tr, xr } );
		}

		index = 0;
		for ( final List< DefaultWeightedEdge > gap : gaps )
		{
			index++;
			final double[] xr = new double[ gap.size() ];
			final double[] tr = new double[ gap.size() ];
			for ( int i = 0; i < gap.size(); i++ )
			{
				final DefaultWeightedEdge edge = gap.get( i );
				final double vel = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
				final double time = fm.getEdgeFeature( edge, EdgeTimeLocationAnalyzer.TIME );
				xr[ i ] = vel;
				tr[ i ] = time;
			}
			dataset.addSeries( "Gap " + index, new double[][] { tr, xr } );
		}

		final JFreeChart chart = ChartFactory.createScatterPlot( "Velocities", "Time", "V", dataset, PlotOrientation.VERTICAL, true,
				false, // tooltips
				false // urls
				);
		final XYPlot plot = ( XYPlot ) chart.getPlot();
		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible( 0, true );
		plot.setRenderer( renderer );

		final ChartPanel chartPanel = new ChartPanel( chart );
		chartPanel.setPreferredSize( new java.awt.Dimension( 500, 270 ) );
		final JFrame frame = new JFrame( "Velocities" );
		frame.setContentPane( chartPanel );
		frame.pack();
		frame.setVisible( true );
	}

	/*
	 * INNER CLASSES
	 */

	private static final class EdgeTimeComparator implements Comparator< DefaultWeightedEdge >
	{
		private final FeatureModel fm;

		public EdgeTimeComparator( final FeatureModel fm )
		{
			this.fm = fm;
		}

		@Override
		public int compare( final DefaultWeightedEdge e1, final DefaultWeightedEdge e2 )
		{
			final double t1 = fm.getEdgeFeature( e1, EdgeTimeLocationAnalyzer.TIME ).doubleValue();
			final double t2 = fm.getEdgeFeature( e2, EdgeTimeLocationAnalyzer.TIME ).doubleValue();

			if ( t1 < t2 ) { return -1; }
			if ( t1 > t2 ) { return 1; }
			return 0;
		}

	}

	/*
	 * FEATURE DECLARATION
	 */

	private static final String NUMBER_OF_PAUSES = "NUMBER_OF_PAUSES";

	private static final String PAUSE_MEAN_DURATION = "PAUSE_MEAN_DURATION";

	private static final String MEAN_VELOCITY_NO_PAUSES = "MEAN_VELOCITY_NO_PAUSES";

	private static final Collection< String > FEATURES;

	private static final Map< String, String > FEATURE_NAMES;

	private static final Map< String, String > FEATURE_SHOTRT_NAMES;

	private static final Map< String, Dimension > FEATURE_DIMENSIONS;

	private static final Map< String, Boolean > IS_INT;

	static
	{
		FEATURES = new ArrayList< String >( 3 );
		FEATURES.add( NUMBER_OF_PAUSES );
		FEATURES.add( PAUSE_MEAN_DURATION );
		FEATURES.add( MEAN_VELOCITY_NO_PAUSES );

		FEATURE_NAMES = new HashMap< String, String >( 3 );
		FEATURE_NAMES.put( NUMBER_OF_PAUSES, "Number of pauses" );
		FEATURE_NAMES.put( PAUSE_MEAN_DURATION, "Mean pause duration" );
		FEATURE_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean velocity w/o pauses" );

		FEATURE_SHOTRT_NAMES = new HashMap< String, String >( 3 );
		FEATURE_SHOTRT_NAMES.put( NUMBER_OF_PAUSES, "N pauses" );
		FEATURE_SHOTRT_NAMES.put( PAUSE_MEAN_DURATION, "Pause duration" );
		FEATURE_SHOTRT_NAMES.put( MEAN_VELOCITY_NO_PAUSES, "Mean V. w/o pauses" );

		FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 3 );
		FEATURE_DIMENSIONS.put( NUMBER_OF_PAUSES, Dimension.NONE );
		FEATURE_DIMENSIONS.put( PAUSE_MEAN_DURATION, Dimension.TIME );
		FEATURE_DIMENSIONS.put( MEAN_VELOCITY_NO_PAUSES, Dimension.VELOCITY );

		IS_INT = new HashMap< String, Boolean >( 3 );
		IS_INT.put( NUMBER_OF_PAUSES, Boolean.TRUE );
		IS_INT.put( PAUSE_MEAN_DURATION, Boolean.FALSE );
		IS_INT.put( MEAN_VELOCITY_NO_PAUSES, Boolean.FALSE );
	}

}
