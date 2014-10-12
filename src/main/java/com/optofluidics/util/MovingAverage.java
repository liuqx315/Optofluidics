package com.optofluidics.util;

import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

public class MovingAverage
{
	private final double[] source;

	private int index;

	private final FixedSizeCircularBuffer queue;

	private final int smoothingWindow;

	public MovingAverage( final double[] source, final int smoothingWindow )
	{
		this.source = source;
		this.smoothingWindow = smoothingWindow;
		this.index = 0;
		this.queue = new FixedSizeCircularBuffer( smoothingWindow );
		for ( int i = 0; i < smoothingWindow / 2; i++ )
		{
			queue.add( source[ index ] );
			index++;
		}
	}

	public boolean hasNext()
	{
		return index < source.length + smoothingWindow;
	}

	public double next()
	{
		index++;
		if ( index < source.length )
		{
			final double x = source[ index ];
			queue.add( x );
		}
		else
		{
			final double x = source[ source.length - 1 ];
			queue.add( x );
		}
		return queue.mean();
	}

	public static class FixedSizeCircularBuffer
	{
		private final double[] buffer;

		private int size;

		private int index;

		private double total;

		public FixedSizeCircularBuffer( final int maxSize )
		{
			this.buffer = new double[ maxSize ];
			this.index = 0;
			this.size = 0;
			this.total = 0d;
		}

		public void add( final double x )
		{
			total += x;

			if ( size < buffer.length )
			{
				size++;
			}
			else
			{
				total -= buffer[ index ];
			}

			buffer[ index ] = x;
			index++;
			if ( index >= buffer.length )
			{
				index = 0;
			}
		}

		public double mean()
		{
			return total / size;
		}

		public double total()
		{
			return total;
		}

		public int size()
		{
			return size;
		}

		@Override
		public String toString()
		{
			return Arrays.toString( Arrays.copyOf( buffer, size ) );
		}
	}

	public static void main( final String[] args )
	{
		final Random ran = new Random( 1l );
		final int NPOINTS = 100;
		final int SMOOTH = 5;

		final double[] x = new double[ NPOINTS ];
		final double[] t = new double[ NPOINTS ];
		for ( int i = 0; i < x.length; i++ )
		{
			x[ i ] = Math.cos( 3 * 2d * Math.PI * i / x.length ) + 0.1d * ran.nextGaussian();
			t[i] = i;
		}

		final MovingAverage avg = new MovingAverage( x, SMOOTH );
		final double[] s = new double[x.length];
		for ( int i = 0; i < s.length; i++ )
		{
			s[ i ] = avg.next();

			if ( i < 10 )
			{
				System.out.println( "" + i + ";\n  raw = " + Arrays.toString( Arrays.copyOf( x, i + 1 ) ) );
				System.out.println( "  sth = " + Arrays.toString( Arrays.copyOf( s, i + 1 ) ) );
				System.out.println( "  buffer = " + avg.queue.toString() );
			}
		}

		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries( "Raw", new double[][] { t, x } );
		dataset.addSeries( "Smooth", new double[][] { t, s } );

		final JFreeChart chart = ChartFactory.createScatterPlot( "Title", // chart
				// title
				"X", // x axis label
				"Y", // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				false, // tooltips
				false // urls
				);
		final XYPlot plot = ( XYPlot ) chart.getPlot();
		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible( 0, true );
		plot.setRenderer( renderer );

		final ChartPanel chartPanel = new ChartPanel( chart );
		chartPanel.setPreferredSize( new java.awt.Dimension( 500, 270 ) );
		final JFrame frame = new JFrame( "Title" );
		frame.setContentPane( chartPanel );
		frame.pack();
		frame.setVisible( true );
	}
}
