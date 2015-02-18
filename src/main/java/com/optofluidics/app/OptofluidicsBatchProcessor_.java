package com.optofluidics.app;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jdom2.Element;

import com.optofluidics.Main;
import com.optofluidics.OptofluidicsParameters;
import com.optofluidics.trackmate.action.TrackVelocityThresholder;
import com.optofluidics.trackmate.action.VelocityAnalysisExporter;
import com.optofluidics.trackmate.visualization.ProfileViewHorizontalFactory;
import com.optofluidics.util.IndentLogger;
import com.optofluidics.util.LogRecorder;
import com.optofluidics.util.OptofluidicsUtil;

public class OptofluidicsBatchProcessor_ implements PlugIn
{

	private static final String HELP_TEXT = "<html>Browse to a folder where "
			+ "the data is arranged per files or sub-folders. "
			+ "<p>"
			+ "Each image can be a multi-image tif file of a folder containing several tif "
			+ "files, one file per frame."
			+ "<p>"
			+ "They will be processed in batch, using the parameters stored in the "
			+ "<code>optifluidics.properties</code> file."
			+ "</html>";

	private static String path;

	private LogRecorder logger = new LogRecorder( Logger.DEFAULT_LOGGER );

	@Override
	public void run( final String folder )
	{
		logger = new LogRecorder( Logger.IJ_LOGGER );
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

		final Logger ilogger = new IndentLogger( logger, 4 );
		final TiffFolderOpenerConverter opener = new TiffFolderOpenerConverter( ilogger );
		final List< File > toProcess = new ArrayList< File >( folders );
		toProcess.addAll( tifFiles );

		final ViewFactory viewFactory = new ProfileViewHorizontalFactory();
		final String viewKey = viewFactory.getKey();

		for ( final File file : toProcess )
		{
			logger.log( "\nProcessing " + file + " - " + new Date() + ".\n" );
			final LogRecorder recorder = new LogRecorder( ilogger );
			final ImagePlus imp = opener.open( file, true );
			if ( null == imp )
			{
				recorder.log( "Problem encountered. Skipping.\n" );
			}
			else
			{
				/*
				 * Execute tracking.
				 */

				final OptofluidicsTrackerProcess tracker = new OptofluidicsTrackerProcess( imp, parameters, recorder );
				if ( !tracker.checkInput() || !tracker.process() )
				{
					recorder.log( "Problem encountered during tracking process:\n" + tracker.getErrorMessage() );
				}
				else
				{
					final Model model = tracker.getModel();
					final Settings settings = tracker.getSettings();
					final SelectionModel selectionModel = new SelectionModel( model );

					/*
					 * Velocity macro analysis.
					 */

					final double velocityThreshold = parameters.getVelocityThreshold();
					final int minConsecutiveFrames = parameters.getMinConsecutiveFrames();
					final int smoothingWindow = parameters.getSmoothingWindow();

					final TrackVelocityThresholder thresholder = new TrackVelocityThresholder( model, velocityThreshold, minConsecutiveFrames, smoothingWindow );
					thresholder.setLogger( recorder );
					if ( !thresholder.checkInput() || !thresholder.process() )
					{
						recorder.error( thresholder.getErrorMessage() );
						return;
					}

					recorder.log( "Velocity analysis done.\n" );

					/*
					 * Export velocity analysis.
					 */
					final String title = imp.getTitle();
					final String analysisFilename = title.substring( 0, title.length() - 4 ) + ".csv";;
					final VelocityAnalysisExporter exporter = new VelocityAnalysisExporter( model, selectionModel );
					final String analysisFilePath = new File( folder, analysisFilename ).getAbsolutePath();
					try
					{
						final ResultsTable resultsTable = exporter.getTable();
						resultsTable.showRowNumbers( false );
						resultsTable.saveAs( analysisFilePath );
						recorder.log( "Exporting analysis results to " + analysisFilePath + " done.\n" );
					}
					catch ( final IOException e1 )
					{
						recorder.error( "Could not export analysis results to file " + analysisFilePath + ".\n" );
					}

					/*
					 * Save to XML.
					 */

					final String xmlFilename = title.substring( 0, title.length() - 4 ) + ".xml";
					final File targetFile = new File( folder, xmlFilename );

					final TmXmlWriter writer = new TmXmlWriter( targetFile, recorder ) {
						@Override
						public void appendGUIState( final TrackMateGUIModel guimodel )
						{
							final Element guiel = new Element( GUI_STATE_ELEMENT_KEY );
							guiel.setAttribute( GUI_STATE_ATTRIBUTE, ConfigureViewsDescriptor.KEY );
							final Element viewel = new Element( GUI_VIEW_ELEMENT_KEY );
							viewel.setAttribute( GUI_VIEW_ATTRIBUTE, viewKey );
							guiel.addContent( viewel );

							root.addContent( guiel );
							logger.log( "  Added GUI current state.\n" );
						}

					};
					writer.appendModel( model );
					writer.appendSettings( settings );
					writer.appendLog( recorder.toString() );
					writer.appendGUIState( null );
					try
					{
						writer.writeToFile();
						recorder.log( "Writing to " + targetFile + " done.\n" );
					}
					catch ( final FileNotFoundException e )
					{
						recorder.error( "Culd not find target file: " + e.getMessage() + "\n" );
						e.printStackTrace();
					}
					catch ( final IOException e )
					{
						recorder.error( "Could not write to " + targetFile + ": " + e.getMessage() + ".\n" );
						e.printStackTrace();
					}

				}

				ilogger.log( "Done - " + new Date() + ".\n" );

			}

		}

		logger.log( "\nAll files processed -  " + new Date() + ".\n" );

		final File logFile = new File( folder, "log.txt" );
		try
		{
			writeLog( logger.toString(), logFile );
		}
		catch ( final IOException e )
		{
			logger.error( "Could not write log file: " + e.getMessage() + ".\n" );
			e.printStackTrace();
		}

	}

	private void writeLog( final String string, final File logFile ) throws IOException
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( logFile ) );
		writer.write( string );
		writer.close();
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OptofluidicsBatchProcessor_().run( "D:/Users/Jean-Yves/Development/Optofluidics/samples/Data" );
	}

}
