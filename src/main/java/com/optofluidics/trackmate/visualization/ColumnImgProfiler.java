package com.optofluidics.trackmate.visualization;

import ij.ImageJ;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * Returns the profile of 1D images.
 * 
 * @author Jean-Yves Tinevez
 */
public class ColumnImgProfiler< T extends RealType< T >>
{

	private static final String TITLE = "Image profiler";

	private final double[] Y;

	private final double[] X;

	private final RandomAccess< T > ra;

	private final int xmin;

	private final int xmax;

	private final String unit;

	private final double ymin;

	private final double ymax;

	private final String title;

	public ColumnImgProfiler( final RandomAccessibleInterval< T > source, final double dx, final String unit, final String title )
	{
		this.unit = unit;
		this.title = title;
		if ( source.numDimensions() != 3 ) { throw new IllegalArgumentException( "ColumnImgProfiler only works for 3D images. N dims was " + source.numDimensions() ); }
		if ( source.dimension( 1 ) != 1 ) { throw new IllegalArgumentException( "ColumnImgProfiler only works for 1D image sequence. Dimensionality was " + Util.printInterval( source ) ); }
		this.ra = source.randomAccess();
		this.xmin = ( int ) source.min( 2 );
		this.xmax = ( int ) source.max( 2 );
		this.Y = new double[ ( int ) source.dimension( 0 ) ];
		this.X = new double[ Y.length ];
		for ( int i = 0; i < X.length; i++ )
		{
			X[ i ] = xmin + i * dx;
		}

		double tmin = Double.POSITIVE_INFINITY;
		double tmax = Double.NEGATIVE_INFINITY;
		for ( final T pixel : Views.iterable( source ) )
		{
			final double val = pixel.getRealDouble();
			if ( val > tmax )
			{
				tmax = val;
			}
			if ( val < tmin )
			{
				tmin = val;
			}
		}
		this.ymin = tmin;
		this.ymax = tmax;

		init();
	}

	public void map( final long t )
	{
		if ( t < xmin || t > ymax ) { return; }
		ra.setPosition( t, 2 );
		ra.setPosition( xmin, 0 );
		for ( int x = 0; x < Y.length; x++ )
		{
			Y[ x ] = ra.get().getRealDouble();
			ra.fwd( 0 );
		}
	}

	private void init()
	{
		/*
		 * Dataset
		 */

		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries( "Spectrum", new double[][] { X, Y } );

		/*
		 * Chart
		 */

		final JFreeChart chart = createChart( dataset );
		final ChartPanel chartPanel = new ChartPanel( chart );
		chartPanel.setPreferredSize( new Dimension( 500, 270 ) );

		/*
		 * Slider
		 */

		final JSlider slider = new JSlider( xmin, xmax );
		final ChangeListener listener = new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				final JSlider slider = ( JSlider ) event.getSource();
				final int value = slider.getValue();
				map( value );
				chart.fireChartChanged();
			}
		};
		slider.addChangeListener( listener );

		/*
		 * Main panel
		 */

		final JPanel panel = new JPanel();
		final BorderLayout layout = new BorderLayout();
		panel.setLayout( layout );
		panel.add( chartPanel, BorderLayout.CENTER );
		panel.add( slider, BorderLayout.SOUTH );

		/*
		 * Frame
		 */

		final JFrame frame = new JFrame( TITLE );
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				System.out.println( "Closing" );// DEBUG
			};
		} );

		frame.setContentPane( panel );
		frame.pack();
		frame.setVisible( true );

		slider.setValue( xmin );
	}

	private JFreeChart createChart( final XYDataset dataset )
	{

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart( title,
				unit, // x axis label
				"#", // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, false, // include legend
				false, // tooltips
				false // urls
				);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint( Color.lightGray );
		plot.setDomainGridlinePaint( Color.white );
		plot.setRangeGridlinePaint( Color.white );
		plot.getRangeAxis().setRange( ymin * 0.95, ymax * 1.05 );

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible( 0, true );
		renderer.setSeriesShapesVisible( 0, false );
		plot.setRenderer( renderer );
		return chart;
	}

	/*
	 * MAIN method
	 */

	public static void main( final String[] args ) throws ImgIOException
	{
		ImageJ.main( args );

		final File file = new File( "samples/SUM_FakeTracks.tif" );
		final List< SCIFIOImgPlus< UnsignedByteType >> imgs = new ImgOpener().openImgs( file.toString(), new UnsignedByteType() );
		final SCIFIOImgPlus< UnsignedByteType > img = imgs.get( 0 );
		final double dx = img.averageScale( 0 );
		final String unit = img.axis( 0 ).unit();

		ImageJFunctions.show( img );
		new ColumnImgProfiler< UnsignedByteType >( img, dx, unit, img.getName() );

	}

}
