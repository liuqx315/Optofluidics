package com.optofluidics.app;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.optofluidics.Main;
import com.optofluidics.OptofluidicsParameters;
import com.optofluidics.util.IndentLogger;
import com.optofluidics.util.OptofluidicsUtil;

public class OptofluidicsBatchProcessor implements PlugIn
{

	private static final String HELP_TEXT = "<html>Browse to a folder where "
			+ "the data is arranged per files or sub-folders. "
			+ "<p>"
			+ "Each image can be a multi-image tif file of a folder containing several tif "
			+ "files, one file per frame."
			+ "<p>"
			+ "They will be processed in batch, using the parameters stored in the "
			+ "<code>optifluidics.parameter</code> file."
			+ "</html>";

	private String path;

	private Logger logger = Logger.DEFAULT_LOGGER;

	@Override
	public void run( final String folder )
	{
		logger = Logger.IJ_LOGGER;
		if ( null == folder || folder.isEmpty() )
		{
			// Use dialog.
			OptofluidicsUtil.setSystemLookAndFeel();
			if ( null == path || path.length() == 0 )
			{
				final File home = new File( System.getProperty( "user.dir" ) );
				final File parent = home.getParentFile();
				final File parent2 = parent == null ? null : parent.getParentFile();
				path = new File( parent2 != null ? parent2 : parent != null ? parent : home, "" ).getAbsolutePath();
			}

			final GenericDialogPlus dialog = new GenericDialogPlus( "Optofluidics batch processor " + Main.OPTOFLUIDICS_LIB_VERSION );
			dialog.addImage( Main.OPTOFLUIDICS_ORANGE_LOGO );
			dialog.addMessage( "Browse to the folder containing the data." );
			dialog.addDirectoryField( "Folder", path );
			dialog.addHelp( HELP_TEXT );
			dialog.showDialog();

			if ( !dialog.wasOKed() ) { return; }
			path = dialog.getNextString();
		}
		else
		{
			// Run with plugin argument as input.
			path = folder;
		}

		final File file = new File( path );
		exec( file );
	}

	public void exec( final File folder )
	{
		logger.log( "Optofluidics batch processor " + Main.OPTOFLUIDICS_LIB_VERSION + " started on " + new Date() + ".\n" );

		/*
		 * Load parameters
		 */

		final OptofluidicsParameters parameters = new OptofluidicsParameters( logger );
		logger.log( "Using tracking parameters:\n" + parameters.toString() );
		logger.log( "\n" );

		/*
		 * Test folder.
		 */

		if ( !folder.exists() )
		{
			logger.error( "Master folder " + folder + " does not exist. Aborting.\n" );
			return;
		}
		if ( !folder.canRead() )
		{
			logger.error( "Could not read the content of folder " + folder + ". Aborting.\n" );
			return;
		}
		if ( !folder.isDirectory() )
		{
			logger.error( "File " + folder + " is not a folder. Aborting.\n" );
			return;
		}

		/*
		 * Get master folder content.
		 */

		logger.log( "Inspecting master folder " + folder + ".\n" );

		final FilenameFilter fileFilter = new FilenameFilter()
		{

			@Override
			public boolean accept( final File dir, final String name )
			{
				final File path = new File( dir, name );
				if ( path.isDirectory() )
				{
					final String[] subFolderContent = path.list( new FilenameFilter()
					{
						@Override
						public boolean accept( final File dir, final String name )
						{
							if ( name.endsWith( ".tif" ) || name.endsWith( ".tiff" ) ) { return true; }
							return false;
						}
					} );
					return subFolderContent.length > 0;
				}
				if ( name.endsWith( ".tif" ) || name.endsWith( ".tiff" ) ) { return true; }
				return false;
			}
		};

		final File[] content = folder.listFiles( fileFilter );
		final List< File > tifFiles = new ArrayList< File >();
		final List< File > folders = new ArrayList< File >();
		for ( final File file : content )
		{
			if ( file.isDirectory() )
			{
				folders.add( file );
			}
			else
			{
				tifFiles.add( file );
			}
		}

		String str1;
		switch ( folders.size() )
		{
		case 1:
			str1 = "Found 1 folder and ";
			break;
		default:
			str1 = "Found " + folders.size() + " folders and ";
		}

		String str2;
		switch ( tifFiles.size() )
		{
		case 1:
			str2 = "1 tiff file to process.\n";
			break;
		default:
			str2 = tifFiles.size() + " files to process.\n";
		}
		logger.log( str1 + str2 );

		if ( tifFiles.isEmpty() && folders.isEmpty() )
		{
			logger.log( "Master folder does not contain any elligible files. Aborting.\n" );
			return;
		}

		/*
		 * If needed, open and convert tif folders.
		 */

		final Logger ilogge = new IndentLogger( logger, 4 );
		final TiffFolderOpenerConverter opener = new TiffFolderOpenerConverter( ilogge );
		final List< File > toProcess = new ArrayList< File >( folders );
		toProcess.addAll( tifFiles );

		for ( final File file : toProcess )
		{
			logger.log( "\nProcessing " + file + " - " + new Date() + ".\n" );
			final ImagePlus imp = opener.open( file, true );
			if ( null == imp )
			{
				logger.log( "Problem encountered. Skipping.\n" );
			}
			else
			{
				final OptofluidicsTrackerProcess tracker = new OptofluidicsTrackerProcess( imp, parameters, ilogge );
				if ( !tracker.checkInput() || !tracker.process() )
				{
					logger.log( "Problem encountered during tracking process:\n" + tracker.getErrorMessage() );
				}
				else
				{
					final File targetFile = new File( folder, imp.getShortTitle() + ".xml" );
					final TmXmlWriter writer = new TmXmlWriter( targetFile, ilogge );
					writer.appendModel( tracker.getModel() );
					writer.appendSettings( tracker.getSettings() );
					writer.appendLog( logRecorder );
					writer.appendGUIState( guiState );
				}

				logger.log( "Done - " + new Date() + ".\n" );

			}

		}

	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OptofluidicsBatchProcessor().run( "D:/Users/Jean-Yves/Development/Optofluidics/samples/Data" );
	}

}
