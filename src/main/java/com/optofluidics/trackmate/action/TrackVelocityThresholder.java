package com.optofluidics.trackmate.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.optofluidics.trackmate.features.manual.EdgeSmoothedVelocityAnalyzer;
import com.optofluidics.trackmate.features.manual.MotionTypeEdgeAnalyzer;
import com.optofluidics.trackmate.features.manual.TrackPausingAnalyzer;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
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

		final Set< Integer > trackIDs = trackModel.unsortedTrackIDs( true );
		// final Integer id = trackIDs.iterator().next();
		for ( final Integer id : trackIDs )
		{
			final Set< DefaultWeightedEdge > edges = trackModel.trackEdges( id );
			final List< DefaultWeightedEdge > ledges = new ArrayList< DefaultWeightedEdge >( edges );
			Collections.sort( ledges, edgeTimeComparator );

			// Collect displacements
			final double[] dxs = new double[ ledges.size() ];
			final double[] dys = new double[ ledges.size() ];
			final double[] dzs = new double[ ledges.size() ];
			final double[] dts = new double[ledges.size()];
			for ( int i = 0; i < ledges.size(); i++ )
			{
				final DefaultWeightedEdge edge = ledges.get( i );
				Spot source = trackModel.getEdgeSource( edge );
				Spot target = trackModel.getEdgeTarget( edge );

				if ( source.diffTo( target, Spot.FRAME ) > 0 )
				{ // Switch order if needed
					final Spot tmp = source;
					source = target;
					target = tmp;
				}
				final double dx = target.diffTo( source, Spot.POSITION_X );
				final double dy = target.diffTo( source, Spot.POSITION_Y );
				final double dz = target.diffTo( source, Spot.POSITION_Z );
				final double dt = target.diffTo( source, Spot.POSITION_T );

				dxs[ i ] = dx;
				dys[ i ] = dy;
				dzs[ i ] = dz;
				dts[i] = dt;
			}

			// Smooth displacements
			final double[] sdxs = gaussianSmooth( smoothingWindow / 2.0d, dxs );
			final double[] sdys = gaussianSmooth( smoothingWindow / 2.0d, dys );
			final double[] sdzs = gaussianSmooth( smoothingWindow / 2.0d, dzs );

			// Compute and assign smoothed velocities
			final double[] velocities = new double[ ledges.size() ];
			for ( int i = 0; i < ledges.size(); i++ )
			{
				final double dx = sdxs[ i ];
				final double dy = sdys[ i ];
				final double dz = sdzs[ i ];
				final double dt = dts[i];
				final double v = Math.sqrt( dx * dx + dy * dy + dz * dx ) / dt;
				velocities[ i ] = v;

				fm.putEdgeFeature( ledges.get( i ), EdgeSmoothedVelocityAnalyzer.SMOOTHED_VELOCITY, Double.valueOf( v ) );
			}

			// Smooth and threshold
			final List< List< DefaultWeightedEdge >> gaps = new ArrayList< List< DefaultWeightedEdge > >();
			final List< List< DefaultWeightedEdge >> runs = new ArrayList< List< DefaultWeightedEdge > >();
			List< DefaultWeightedEdge > gap = new ArrayList< DefaultWeightedEdge >();
			List< DefaultWeightedEdge > run = new ArrayList< DefaultWeightedEdge >();

			boolean inPause = false;
			int nSection = 0;

			for ( int i = 0; i < velocities.length; i++ )
			{
				final double v = velocities[ i ];
				final DefaultWeightedEdge edge = ledges.get( i );

				if ( v < velocityThreshold )
				{
					if ( inPause )
					{
						// Already in gap.
						nSection++;
					}
					else
					{
						// New gap.
						inPause = true;
						// First check if the preceding run was long enough.
						if ( nSection > minConsecutiveFrames )
						{
							// Yes, it was long enough, so we can consider it a
							// run.
							if ( !runs.contains( run ) )
							{
								runs.add( run );
							}

							// Start a new gap
							nSection = 1;
							gap = new ArrayList< DefaultWeightedEdge >();
						}
						else
						{
							// No, not long enough. So we simply add the
							// previous edges to the current gap.
							gap.addAll( run );
						}

					}

					gap.add( edge );

				}
				else
				{

					if ( inPause )
					{
						// Was in a gap, and leaving it.
						inPause = false;
						if ( nSection > minConsecutiveFrames )
						{
							// Gap was long enough; we can store it.
							if ( !gaps.contains( gap ) )
							{
								gaps.add( gap );
							}
							// Start a new run
							nSection = 1;
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


			/*
			 * Assign feature values.
			 */

			// Number of pauses.
			final int nPauses = gaps.size();
			fm.putTrackFeature( id, TrackPausingAnalyzer.NUMBER_OF_PAUSES, Double.valueOf( nPauses ) );

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

				// Movement type = pausing
				for ( final DefaultWeightedEdge edge : gap2 )
				{
					fm.putEdgeFeature( edge, MotionTypeEdgeAnalyzer.MOVEMENT_TYPE, MotionTypeEdgeAnalyzer.PAUSING );
				}

			}
			final double meanPauseDuration = totalPauseDuration / nPauses;
			fm.putTrackFeature( id, TrackPausingAnalyzer.PAUSE_MEAN_DURATION, Double.valueOf( meanPauseDuration ) );

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

					// Movement type = running
					fm.putEdgeFeature( edge, MotionTypeEdgeAnalyzer.MOVEMENT_TYPE, MotionTypeEdgeAnalyzer.RUNNING );
				}
			}
			final double meanVelocity = totalVelocity / nVelocity;
			fm.putTrackFeature( id, TrackPausingAnalyzer.MEAN_VELOCITY_NO_PAUSES, Double.valueOf( meanVelocity ) );

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
		final double[] xs = new double[ ledges.size() ];
		final double[] t = new double[ ledges.size() ];
		for ( int i = 0; i < ledges.size(); i++ )
		{
			final DefaultWeightedEdge edge = ledges.get( i );
			final double vel = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );
			final double svel = fm.getEdgeFeature( edge, EdgeSmoothedVelocityAnalyzer.SMOOTHED_VELOCITY );
			final double time = fm.getEdgeFeature( edge, EdgeTimeLocationAnalyzer.TIME );
			x[ i ] = vel;
			xs[ i ] = svel;
			t[ i ] = time;
		}

		final DefaultXYDataset dataset = new DefaultXYDataset();
		// dataset.addSeries( "Raw", new double[][] { t, x } );
		dataset.addSeries( "Smooth", new double[][] { t, xs } );

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

	private static final double[] gaussianSmooth( final double sigma, final double[] source )
	{
		final ArrayImg< DoubleType, DoubleArray > img = ArrayImgs.doubles( source, new long[] { source.length } );
		final ExtendedRandomAccessibleInterval< DoubleType, ArrayImg< DoubleType, DoubleArray >> extdImg = Views.extendMirrorDouble( img );

		final double[] target = new double[ source.length ];
		final ArrayImg< DoubleType, DoubleArray > targetImg = ArrayImgs.doubles( target, new long[] { source.length } );

		try
		{
			Gauss3.gauss( sigma, extdImg, targetImg );
		}
		catch ( final IncompatibleTypeException e )
		{
			e.printStackTrace();
		}

		return target;
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

}
