package com.optofluidics.spectrumplayer;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import fiji.util.SplitString;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import au.com.bytecode.opencsv.CSVReader;

public class SpectrumPlayerPlugin_ implements PlugIn
{
	private static final String PLUGIN_TITLE = "Optofluidics spectrum player.";

	private static final Object IMAGE_COMMAND = "image";

	private static final String SPECTRUM_COMMAND = "spectrum";

	/**
	 * The number of metadata lines in the spectrum file.
	 */
	private static final int METADATA_LINES = 8;

	private static File previousPath = null;

	private ArrayList< double[] > spectra;

	private ArrayList< Double > spectraTimeStamps;

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

		if ( dialog.wasCanceled() )
		{
			return;
		}

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

		// Load spectrum in memory.
		loadSpectrum( spectrumFile );

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
			while ( currentSpectrum < spectraTimeStamps.size() && spectraTimeStamps.get( currentSpectrum ) <= frame )
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
				if ( targetSpectrum < 0 )
				{
					System.out.println( "Moved to frame " + frame + " not associated with a spectrum. " );
				}
				else
				{
					System.out.println( "Moved to frame " + frame + " associated with spectrum " + targetSpectrum );
				}
			}
		};
		new SliceObserver( imp, listener );
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
			final double[] X = new double[ nextLine.length ];
			for ( int i = 0; i < X.length; i++ )
			{
				X[ i ] = Double.parseDouble( nextLine[ i ] );
				// TODO check if this change when we have real timestamps.
			}

			// Blank line
			nextLine = reader.readNext();
			line++;

			// Spectra.
			spectra = new ArrayList< double[] >();
			spectraTimeStamps = new ArrayList< Double >();
			while ( ( nextLine = reader.readNext() ) != null )
			{
				line++;
				final double[] spectrum = new double[ nextLine.length ];

				// Timestamps are supposed to be stored at 1st column;
				// Right now, I am making up this.
				spectraTimeStamps.add( Double.valueOf( ( line - 8 ) * 4 ) ); // nextLine[
																				// 0
				// ] ) );
				// FIXME

				for ( int i = 0; i < X.length; i++ )
				{
					try
					{
						spectrum[ i ] = Double.parseDouble( nextLine[ i ] );
						// TODO check if this change when we have real
						// timestamps.
					}
					catch ( final NumberFormatException nfe )
					{
						IJ.error( PLUGIN_TITLE, "Problem reading line " + line + ", column " + ( i + 1 ) + " of the CSV file:\n" + nfe.getMessage() );
						reader.close();
						return;
					}
				}
				spectra.add( spectrum );
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
		IJ.open( "http://imagej.nih.gov/ij/images/bat-cochlea-volume.zip" );
		new SpectrumPlayerPlugin_().run( "image=bat-cochlea-volume.tif spectrum=E:\\Users\\JeanYves\\Documents\\Projects\\Optofluidics\\24-12.csv" );
	}
}
