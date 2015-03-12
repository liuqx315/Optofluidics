package com.optofluidics.app;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.util.SplitString;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	/**
	 * @param arg
	 *            arg string, in the shape of
	 * 
	 *            <pre>
	 * folder=/path/to/master/folder parameters=optofluidics.parameters
	 * </pre>
	 */
	@Override
	public void run( final String arg )
	{
		logger = new LogRecorder( Logger.IJ_LOGGER );

		String folder = null;
		String parameterSetName = null;
		if ( null != arg )
		{
			try
			{
				final Map< String, String > macroOptions = SplitString.splitMacroOptions( arg );
				folder = macroOptions.get( "folder" );
				parameterSetName = macroOptions.get( "parameters" );
			}
			catch ( final ParseException e )
			{
				logger.error( "Could not parse plugin option string: " + e.getMessage() + ".\n" );
				e.printStackTrace();
			}
		}

		if ( null == folder || folder.isEmpty() || null == parameterSetName || parameterSetName.isEmpty() )
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

			final String[] availableParameters = OFAppUtils.getParameterSetList();
			if ( availableParameters != null && availableParameters.length > 0 )
			{
				dialog.addMessage( "Select a parameter set." );
				String defaultChoice;
				if ( parameterSetName != null && !parameterSetName.isEmpty() && Arrays.asList( availableParameters ).contains( parameterSetName ) )
				{
					defaultChoice = parameterSetName;
				}
				else
				{
					defaultChoice = availableParameters[ 0 ];
				}
				dialog.addChoice( "Parameters set", availableParameters, defaultChoice );
			}
			else
			{
				dialog.addMessage( "No parameters set found. Relying on defaults." );
			}

			dialog.addHelp( HELP_TEXT );
			dialog.showDialog();

			if ( !dialog.wasOKed() ) { return; }
			path = dialog.getNextString();
			String chosenParameterSet = dialog.getNextChoice();
			if ( chosenParameterSet.length() < 1 )
			{
				chosenParameterSet = null;
			}
		}
		else
		{
			// Run with plugin argument as input.
			path = folder;
		}

		final File file = new File( path );
		exec( file, parameterSetName );
	}

	public void exec( final File folder, final String parameterSetName )
	{
		logger.log( "Optofluidics batch processor " + Main.OPTOFLUIDICS_LIB_VERSION + " started on " + new Date() + ".\n" );

		/*
		 * Load parameters
		 */

		final OptofluidicsParameters parameters = new OptofluidicsParameters( logger, parameterSetName );
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
					final String title = imp.getTitle();

					/*
					 * Spots in track analysis.
					 */

					recorder.log( "Generating spots statistics.\n" );
					final ResultsTable spotsStatsTable = getSpotsIntTracksStatistics( model );
					spotsStatsTable.showRowNumbers( false );

					final String spotStatsFilename = title.substring( 0, title.length() - 4 ) + "_SpotsStats.csv";
					final String spotStatsFilePath =  new File( folder, spotStatsFilename ).getAbsolutePath();
					try
					{
						spotsStatsTable.saveAs( spotStatsFilePath );
					}
					catch ( final IOException e2 )
					{
						recorder.error( "Could not export spots statistics to file " + spotStatsFilePath + ".\n" );
						e2.printStackTrace();
					}
					recorder.log( "Exporting spots statistics to " + spotStatsFilePath + " done.\n" );

					/*
					 * Velocity macro analysis.
					 */

					recorder.log( "Performing velocity analysis.\n" );
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

					final String velocityAnalysisFilename = title.substring( 0, title.length() - 4 ) + "_VelocityStats.csv";
					final VelocityAnalysisExporter exporter = new VelocityAnalysisExporter( model, selectionModel );
					final String velocityAnalysisFilePath = new File( folder, velocityAnalysisFilename ).getAbsolutePath();
					try
					{
						final ResultsTable velocityAnalysisTable = exporter.getTable();
						velocityAnalysisTable.showRowNumbers( false );
						velocityAnalysisTable.saveAs( velocityAnalysisFilePath );
						recorder.log( "Exporting velocity analysis results to " + velocityAnalysisFilePath + " done.\n" );
					}
					catch ( final IOException e1 )
					{
						recorder.error( "Could not export velocity analysis results to file " + velocityAnalysisFilePath + ".\n" );
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

	private ResultsTable getSpotsIntTracksStatistics( final Model model )
	{

		final FeatureModel fm = model.getFeatureModel();
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
		final Collection< String > spotFeatures = fm.getSpotFeatures();

		// Create table
		final ResultsTable spotTable = new ResultsTable();

		// Parse spots to insert values as objects
		for ( final Integer trackID : trackIDs )
		{
			final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
			// Sort by frame
			final List< Spot > sortedTrack = new ArrayList< Spot >( track );
			Collections.sort( sortedTrack, Spot.frameComparator );

			for ( final Spot spot : sortedTrack )
			{
				spotTable.incrementCounter();
				spotTable.addLabel( spot.getName() );
				spotTable.addValue( "ID", "" + spot.ID() );
				spotTable.addValue( "TRACK_ID", "" + trackID.intValue() );
				for ( final String feature : spotFeatures )
				{
					final Double val = spot.getFeature( feature );
					if ( null == val )
					{
						spotTable.addValue( feature, "None" );
					}
					else
					{
						if ( fm.getSpotFeatureIsInt().get( feature ).booleanValue() )
						{
							spotTable.addValue( feature, "" + val.intValue() );
						}
						else
						{
							spotTable.addValue( feature, val.doubleValue() );
						}
					}
				}
			}
		}
		return spotTable;
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
//		new OptofluidicsBatchProcessor_().run( "folder=[/Users/tinevez/Development/OptoFluidics/Optofluidics/samples/Data] parameters=test.properties" );
		new OptofluidicsBatchProcessor_().run( null );
	}

}
