package com.optofluidics.spectrumplayer;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import fiji.util.SplitString;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

public class SpectrumPlayerPlugin_ implements PlugIn
{
	private static final String PLUGIN_TITLE = "Optofluidics spectrum player.";

	private static final Object IMAGE_COMMAND = "image";

	private static final String SPECTRUM_COMMAND = "spectrum";

	/**
	 * The number of metadata lines in the spectrum file.
	 */
	private static final int METADATA_LINES = 8;

	private static final String XLABEL = "lambda (nm)";

	private static final String YLABEL = "#";

	private static File previousPath = null;

	private List< double[] > spectra;

	private List< Date > spectraTimestamps;

	private List< Date > frameTimestamps;

	private double[] X;

	private DefaultXYDataset dataset;

	private SliceObserver sliceObserver;


	@Override
	public void run( final String command )
	{
		if ( WindowManager.getImageCount() <= 0 )
		{
			IJ.error( PLUGIN_TITLE, "Please open an image first." );
		}

		// I can't stand the metal look. If this is a problem, contact me
		// (jeanyves.tinevez@gmail.com)
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}

		if ( null == command || command.length() == 0 )
		{
			showDialog();
		}
		else
		{
			processCommand( command );
		}
	}

	private void processCommand( final String command )
	{
		final Map< String, String > map;
		try
		{
			map = SplitString.splitMacroOptions( command );
		}
		catch ( final ParseException e )
		{
			IJ.error( PLUGIN_TITLE, e.getMessage() );
			return;
		}

		final Set< String > keys = map.keySet();

		if ( !keys.contains( IMAGE_COMMAND ) )
		{
			IJ.error( PLUGIN_TITLE, "Missing the command for image: '" + IMAGE_COMMAND + "'." );
			return;
		}
		if ( !keys.contains( SPECTRUM_COMMAND ) )
		{
			IJ.error( PLUGIN_TITLE, "Missing the command for spectrum file: '" + SPECTRUM_COMMAND + "'." );
			return;
		}

		final String imageName = map.get( IMAGE_COMMAND );
		final String spectrumPath = map.get( SPECTRUM_COMMAND );
		processInputs( imageName, spectrumPath );
	}

	private void showDialog()
	{
		final GenericDialogPlus dialog = new GenericDialogPlus( PLUGIN_TITLE );

		if ( null == previousPath )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			previousPath = parent == null ? null : parent.getParentFile();
		}
		final String defaultPath = previousPath.getAbsolutePath();

		dialog.addMessage( "Select the image data." );
		dialog.addImageChoice( "Image data", null );

		dialog.addMessage( "Select the spectrum file." );
		dialog.addFileField( "Spectrum data", defaultPath, 30 );

		dialog.showDialog();

		/*
		 * Process inputs
		 */

		if ( dialog.wasCanceled() ) { return; }

		final String imageName = dialog.getNextImage().getTitle();
		final String spectrumPath = dialog.getNextString();
		processInputs( imageName, spectrumPath );
	}

	private void processInputs( final String imageName, final String spectrumPath )
	{
		final ImagePlus imp = WindowManager.getImage( imageName );
		if ( null == imp )
		{
			IJ.error( PLUGIN_TITLE, "Image named " + imageName + " is not open." );
		}
		userCheckImpDimensions( imp );

		/*
		 * Retrirve the metadata file for the image
		 */

		loadMetadata( imp );

		/*
		 * Load spectrum in memory.
		 */

		final File spectrumFile = new File( spectrumPath );
		if ( !spectrumFile.exists() || !spectrumFile.canRead() )
		{
			IJ.error( PLUGIN_TITLE, "Could not read or find file " + spectrumFile );
			return;
		}
		if ( !spectrumPath.endsWith( ".csv" ) && !spectrumPath.endsWith( ".CSV" ) )
		{
			IJ.error( PLUGIN_TITLE, "Please specify a CSV file. Got " + spectrumPath );
			return;
		}

		loadSpectrum( spectrumFile );

		// Make time relative
		final long[] time = new long[ spectraTimestamps.size() ];
		final long t0 = spectraTimestamps.get( 0 ).getTime();
		for ( int i = 0; i < time.length; i++ )
		{
			time[ i ] = spectraTimestamps.get( i ).getTime() - t0;
		}

		// Prepare time stamp "map".
		final int nFrames = imp.getNFrames();
		final int[] targetSpectra = new int[ nFrames ];
		int currentSpectrum = 0;
		for ( int frame = 0; frame < nFrames; frame++ )
		{
			/*
			 * IMPORTANT! We suppose that the spectrum timestamps are sorted in
			 * INCREASING order.
			 */
			while ( currentSpectrum < spectraTimestamps.size() && spectraTimestamps.get( currentSpectrum ).before( frameTimestamps.get( frame ) ) )
			{
				currentSpectrum++;
			}
			targetSpectra[ frame ] = currentSpectrum - 1;
		}

		final SliceListener listener = new SliceListener()
		{
			@Override
			public void sliceChanged( final ImagePlus image )
			{
				final int frame = image.getT() - 1;
				final int targetSpectrum = targetSpectra[ frame ];
				displaySpectrum( targetSpectrum );
			}
		};
		sliceObserver = new SliceObserver( imp, listener );
	}

	private void loadMetadata( final ImagePlus imp )
	{
		final FileInfo finfo = imp.getOriginalFileInfo();

		// Build file name
		final int dotIndex = finfo.fileName.lastIndexOf( ".ome.tif" );
		if ( dotIndex < 0 )
		{
			IJ.log( "The image file is not an .ome.tiff file." );
		}
		final String mdFileName = finfo.fileName.substring( 0, dotIndex ) + "_metadata.txt";
		final File md = new File( finfo.directory, mdFileName );

		IJ.log( "Parsing metadata file: " + md );

		/*
		 * Find first date
		 */

		Date date = null;
		Scanner scanner = null;
		try
		{
			scanner = new Scanner( md );
			final Pattern datePattern = Pattern.compile( "\"Time\": \"(.+)\"," );
			scanner.useDelimiter( "\"FrameKey-(\\d)-(\\d)-(\\d)\":" );
			while ( scanner.hasNext() )
			{
				final String str = scanner.next();
				final Matcher matcher = datePattern.matcher( str );
				if ( matcher.find() )
				{
					final String dateStr = matcher.group( 1 );
					final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
					try
					{
						date = dateFormat.parse( dateStr );
					}
					catch ( final ParseException e )
					{
						IJ.log( "Date found, but in incorrect format:\nGot " + dateStr + " and expected something like " + dateFormat );
						e.printStackTrace();
					}
					break;
				}
			}
		}
		catch ( final FileNotFoundException e )
		{
			IJ.log( "Could not find metadata file: " + md );
		}
		finally
		{
			scanner.close();
		}

		if ( null == date )
		{
			IJ.log( "Could not find a date in the metadate file." );
		}
		IJ.log( "Found a starting date for imaging: " + date );

		/*
		 * Find all deltaT
		 * 
		 * Important!! We suppose the metadata does not miss a single frame.
		 */

		final Pattern deltaTPattern = Pattern.compile( "\"ElapsedTime-ms\": (\\d+)," );
		final List< Integer > elapsedTimes = new ArrayList< Integer >();
		try
		{
			scanner = new Scanner( md );
			scanner.useDelimiter( "\"FrameKey-(\\d)-(\\d)-(\\d)\":" );
			while ( scanner.hasNext() )
			{
				final String str = scanner.next();
				final Matcher matcher = deltaTPattern.matcher( str );
				while ( matcher.find() )
				{
					final String dtStr = matcher.group( 1 );
					final int dt = Integer.parseInt( dtStr );
					elapsedTimes.add( Integer.valueOf( dt ) );
				}

			}
		}
		catch ( final FileNotFoundException e )
		{
			IJ.log( "Could not find metadata file: " + md );
		}
		finally
		{
			scanner.close();
		}

		IJ.log( "Found " + elapsedTimes.size() + " time-stamps registered in the metadata file." );

		/*
		 * Build absolute date
		 */

		frameTimestamps = new ArrayList< Date >( elapsedTimes.size() );
		for ( final Integer dt : elapsedTimes )
		{
			final Date frameDate = new Date( date.getTime() + dt.longValue() );
			frameTimestamps.add( frameDate );
		}
	}

	private void displaySpectrum( final int targetSpectrum )
	{

		if ( targetSpectrum < 0 )
		{
			return;
		}
		else
		{
			final double[] spectrum = spectra.get( targetSpectrum );
			if ( null == dataset )
			{
				dataset = new DefaultXYDataset();
				dataset.addSeries( "Spectrum", new double[][] { X, spectrum } );

				final JFreeChart chart = createChart( dataset );
				final ChartPanel chartPanel = new ChartPanel( chart );
				chartPanel.setPreferredSize( new Dimension( 500, 270 ) );

				final JFrame frame = new JFrame( PLUGIN_TITLE );
				frame.addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosing( final java.awt.event.WindowEvent e )
					{
						dataset = null;
						sliceObserver.unregister();
					};
				} );
				frame.setContentPane( chartPanel );
				frame.pack();
				frame.setVisible( true );
			}
			else
			{
				dataset.addSeries( "Spectrum", new double[][] { X, spectrum } );
			}
		}
	}

	private JFreeChart createChart( final XYDataset dataset )
	{

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart( PLUGIN_TITLE, // chart
				// title
				XLABEL, // x axis label
				YLABEL, // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, false, // include legend
				false, // tooltips
				false // urls
				);

		chart.setBackgroundPaint( Color.white );

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint( Color.lightGray );
		plot.setDomainGridlinePaint( Color.white );
		plot.setRangeGridlinePaint( Color.white );

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible( 0, true );
		renderer.setSeriesShapesVisible( 0, false );
		plot.setRenderer( renderer );

		return chart;

	}

	private void loadSpectrum( final File spectrumFile )
	{

		final CSVReader reader;
		try
		{
			reader = new CSVReader( new FileReader( spectrumFile ) );
		}
		catch ( final FileNotFoundException e )
		{
			IJ.error( PLUGIN_TITLE, e.getMessage() );
			return;
		}

		// Pattern for time stamps
		final SimpleDateFormat tsFormat = new SimpleDateFormat( "'Timestamp: 'yyyy-MM-dd HH:mm:ss.SSS" );

		try
		{
			String[] nextLine;
			int line = 0;

			// Read and log metadata
			IJ.log( "For spectrum file " + spectrumFile );
			for ( int i = 0; i < METADATA_LINES; i++ )
			{
				line++;
				nextLine = reader.readNext();
				if ( null == nextLine )
				{
					IJ.error( PLUGIN_TITLE, "Unexpected end of file for " + spectrumFile );
					reader.close();
					return;
				}
				IJ.log( nextLine[ 0 ] + " = " + nextLine[ 1 ] );
			}

			// Read X
			nextLine = reader.readNext();
			line++;
			if ( null == nextLine )
			{
				IJ.error( PLUGIN_TITLE, "Unexpected end of file for " + spectrumFile );
				reader.close();
				return;
			}
			// Reverse order
			X = new double[ nextLine.length ];
			for ( int i = 0; i < X.length; i++ )
			{
				X[ i ] = Double.parseDouble( nextLine[ i ] );
			}

			// Spectra * timestamps.
			spectra = new ArrayList< double[] >();
			spectraTimestamps = new ArrayList< Date >();
			while ( ( nextLine = reader.readNext() ) != null )
			{
				line++;

				if ( nextLine.length < X.length )
				{
					IJ.log( "Warning: Spectrum on line " + line + " does not have enough data points. Expected at least " + X.length + " but found " + nextLine.length + ". Skipping." );
					continue;
				}

				final double[] spectrum = new double[ X.length ];
				for ( int i = 0; i < X.length; i++ )
				{
					try
					{
						spectrum[ i ] = Double.parseDouble( nextLine[ i ] );
					}
					catch ( final NumberFormatException nfe )
					{
						IJ.error( PLUGIN_TITLE, "Problem reading line " + line + ", column " + ( i + 1 ) + " of the CSV file:\n" + nfe.getMessage() );
						reader.close();
						return;
					}
				}
				spectra.add( spectrum );

				/*
				 * Time-stamps
				 */

				// Read one more line
				nextLine = reader.readNext();
				if ( nextLine == null )
				{
					IJ.log( "Warning: Spectrum on line " + line + " is missing its time-stamp." );
					spectraTimestamps.add( spectraTimestamps.get( spectraTimestamps.size() - 1 ) );
					// Add last one instead.
					break;
				}
				line++;

				final String ts = nextLine[ 0 ];
				try
				{
					final Date date = tsFormat.parse( ts );
					spectraTimestamps.add( date );
				}
				catch ( final ParseException e )
				{
					IJ.log( "Warning: Time-stamp on line " + line + " cound not be interpreted." );
					spectraTimestamps.add( spectraTimestamps.get( spectraTimestamps.size() - 1 ) );
					// Add last one instead.
					continue;
				}

			}

			// Log.
			IJ.log( "Found " + spectra.size() + " spectra made of " + X.length + " data points." );
			reader.close();
		}
		catch ( final IOException e )
		{
			IJ.error( PLUGIN_TITLE, e.getMessage() );
			return;
		}
	}

	public static final void userCheckImpDimensions( final ImagePlus imp )
	{
		final int[] dims = imp.getDimensions();
		if ( dims[ 4 ] == 1 && dims[ 3 ] > 1 )
		{
			switch ( JOptionPane.showConfirmDialog( null, "It appears this image has 1 timepoint but " + dims[ 3 ] + " slices.\n" + "Do you want to swap Z and T?", "Z/T swapped?", JOptionPane.YES_NO_CANCEL_OPTION ) )
			{
			case JOptionPane.YES_OPTION:
				imp.setDimensions( dims[ 2 ], dims[ 4 ], dims[ 3 ] );
				final Calibration calibration = imp.getCalibration();
				if ( calibration.frameInterval == 0 )
				{
					calibration.frameInterval = 1;
				}
				break;
			case JOptionPane.CANCEL_OPTION:
				return;
			}
		}
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		IJ.open( "/Users/JeanYves/Documents/Development/Optofluidics/test2_stack_100ms-rate_MMStack.ome.tif" );
		// new SpectrumPlayerPlugin_().run(
		// "image=bat-cochlea-volume.tif spectrum=../24-12.csv" );
		new SpectrumPlayerPlugin_().run( "image=test2_stack_100ms-rate_MMStack.ome.tif spectrum=../t6_76-5.csv" );
		// new SpectrumPlayerPlugin_().run( "" );
	}

}
