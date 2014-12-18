package com.optofluidics.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.scif.img.ImgIOException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
public class ColumnImgProfiler
{

	private static final String TITLE = "Image profiler";

	private final double[] Y;

	private final double[] X;

	private final String unit;

	private final double ymin;

	private final double ymax;

	private final String title;

	private final int tmax;

	private final ImagePlus imp;

	private final int width;

	private final ImagePlus kymograph;

	public ColumnImgProfiler( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		this.imp = imp;
		if ( imp.getHeight() != 1 ) { throw new IllegalArgumentException( "ColumnImgProfiler only works for 1D image sequence. Dimensionality was " + imp.getWidth() + " x " + imp.getHeight() ); }

		this.kymograph = KymographGenerator.fromLineImage( imp );
		this.unit = imp.getCalibration().getUnits();
		this.title = imp.getShortTitle();
		this.Y = new double[ imp.getWidth() ];
		this.X = new double[ Y.length ];
		for ( int i = 0; i < X.length; i++ )
		{
			X[ i ] = i * imp.getCalibration().pixelWidth;
		}

		this.tmax = imp.getStackSize();
		this.width = imp.getWidth();
		double tempmin = Double.POSITIVE_INFINITY;
		double tempmax = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < tmax; i++ )
		{
			final double max = imp.getStack().getProcessor( i + 1 ).getMax();
			if ( max > tempmax )
			{
				tempmax = max;
			}
			final double min = imp.getStack().getProcessor( i + 1 ).getMin();
			if ( min < tempmin )
			{
				tempmin = min;
			}
		}
		this.ymin = tempmin;
		this.ymax = tempmax;


		init();
	}

	public void map( final int t )
	{
		if ( t < 0 || t >= tmax ) { return; }
		final ImageProcessor ip = imp.getStack().getProcessor( t + 1 );
		for ( int i = 0; i < width; i++ )
		{
			Y[ i ] = ip.getf( i );
		}
	}

	private void init()
	{
		/*
		 * Kymograph
		 */

		kymograph.show();

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

		final JSlider slider = new JSlider( 0, tmax );
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

		slider.setValue( 0 );
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
		try
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch ( final ClassNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( final InstantiationException e )
		{
			e.printStackTrace();
		}
		catch ( final IllegalAccessException e )
		{
			e.printStackTrace();
		}
		catch ( final UnsupportedLookAndFeelException e )
		{
			e.printStackTrace();
		}

		ImageJ.main( args );

		final File file = new File( "samples/SUM_FakeTracks.tif" );
		final ImagePlus imp = new ImagePlus( file.toString() );

		final Model model = new Model();
		final SelectionModel selectionModel = new SelectionModel( model );
		new ColumnImgProfiler( model, selectionModel, imp );

	}

}
