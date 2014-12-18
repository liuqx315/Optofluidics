package com.optofluidics.trackmate.visualization;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.process.ImageProcessor;
import io.scif.img.ImgIOException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * Returns the profile of 1D images.
 * 
 * @author Jean-Yves Tinevez
 */
public class ColumnImgProfiler extends AbstractTrackMateModelView
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

	private KymographOverlay kymographOverlay;

	private JFreeChart chart;

	private int frame;

	private ProfileOverlay profileOverlay;

	public ColumnImgProfiler( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		super( model, selectionModel );
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

	@Override
	public void render()
	{

		/*
		 * Kymograph
		 */

		kymograph.show();
		kymograph.setOverlay( new Overlay() );
		kymographOverlay = new KymographOverlay( model, kymograph, displaySettings, imp.getCalibration().pixelWidth );
		kymograph.getOverlay().add( kymographOverlay );


		/*
		 * Dataset
		 */

		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries( "Spectrum", new double[][] { X, Y } );

		/*
		 * Chart
		 */

		chart = createChart( dataset );
		final ChartPanel chartPanel = new ChartPanel( chart );
		chartPanel.setPreferredSize( new Dimension( 500, 270 ) );
		profileOverlay = new ProfileOverlay( model, displaySettings );
		chartPanel.addOverlay( profileOverlay );

		/*
		 * Slider
		 */

		final JSlider slider = new JSlider( 0, tmax - 1 );
		final ChangeListener listener = new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				final JSlider slider = ( JSlider ) event.getSource();
				final int frame = slider.getValue();
				displayFrame( frame );
			}
		};
		slider.addChangeListener( listener );

		/*
		 * MouseWheel listener
		 */

		final MouseWheelListener mlListener = new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved( final MouseWheelEvent e )
			{
				final int rotation = e.getWheelRotation();
				frame += rotation;
				if ( frame < 0 )
				{
					frame = 0;
				}
				if ( frame >= tmax )
				{
					frame = tmax - 1;
				}
				slider.setValue( frame );
				// Will trigger refresh.
			}
		};
		kymograph.getWindow().addMouseWheelListener( mlListener );

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
		frame.addMouseWheelListener( mlListener );
		frame.setContentPane( panel );
		frame.pack();
		GuiUtils.positionWindow( frame, kymograph.getWindow() );
		frame.setVisible( true );

		slider.setValue( 0 );
	}

	public void displayFrame(final int frame)
	{
		this.frame = frame;
		refresh();
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

		final NumberAxis yAxis = ( NumberAxis ) plot.getRangeAxis();
		final Range range = new Range( ymin * 0.95, ymax * 1.05 );
		yAxis.setAutoRangeIncludesZero( false );
		yAxis.setRange( range );
		yAxis.setDefaultAutoRange( range );

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible( 0, true );
		renderer.setSeriesShapesVisible( 0, false );
		renderer.setSeriesPaint( 0, Color.BLACK );
		plot.setRenderer( renderer );
		return chart;
	}

	@Override
	public void refresh()
	{
		map( frame );
		if ( chart != null && kymographOverlay != null )
		{
			kymographOverlay.setFrame( frame );
			kymograph.updateAndDraw();
			profileOverlay.setFrame( frame );
			chart.fireChartChanged();
		}
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getKey()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		// TODO Auto-generated method stub

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

		//		final File file = new File( "samples/SUM_FakeTracks.tif" );
		//		final ImagePlus imp = new ImagePlus( file.toString() );
		//
		//		final Model model = new Model();

		final File file = new File( "samples/SUM_FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final Settings settings = new Settings();
		reader.readSettings( settings, null, null, null, null, null );
		final SelectionModel selectionModel = new SelectionModel( model );

		final ColumnImgProfiler profiler = new ColumnImgProfiler( model, selectionModel, settings.imp );
		profiler.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_ID ) );
		final SpotColorGenerator scg = new SpotColorGenerator( model );
		scg.setFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY );
		profiler.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, scg );
		profiler.setDisplaySettings( TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true );
		profiler.setDisplaySettings( TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD );

		profiler.render();
	}
}
