package com.optofluidics.app;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.FolderOpener;

import java.io.File;

import fiji.plugin.trackmate.Logger;

public class TiffFolderOpenerConverter
{
	private static final String CONVERTED_FOLDER = "Converted";

	private final Logger logger;

	public TiffFolderOpenerConverter( final Logger logger )
	{
		this.logger = logger;
	}

	public ImagePlus open( final File file, final boolean convertAndSave )
	{
		if ( file.isDirectory() )
		{
			logger.log( "Source " + file + " is a folder.\n" );
			final File parent = file.getParentFile();
			final File target = new File( parent, makeTargetName( file ) );
			final boolean exist = target.exists();
			if ( exist )
			{
				logger.log( "Matching tif file found in parent folder.\n" );
				if ( convertAndSave )
				{
					logger.log( "Loading from folder and overwriting existing tif file.\n" );
					return convertAndSave( file, target );
				}
				else
				{
					logger.log( "Loading from existing tif file.\n" );
					return readFile( target );
				}
			}
			else
			{
				if ( convertAndSave )
				{
					logger.log( "Converting and saving to multi-page tiff.\n" );
					return convertAndSave( file, target );
				}
				else
				{
					logger.log( "Reading source folder.\n" );
					return readFolder( file );
				}
			}
		}
		else
		{
			logger.log( "Loading from existing tif file.\n" );
			return readFile( file );
		}
	}

	private ImagePlus readFile( final File sourceFile )
	{
		final ImagePlus imp = IJ.openImage( sourceFile.getAbsolutePath() );
		if ( null == imp )
		{
			logger.error( "Could not load from " + sourceFile + ".\n" );
			return null;
		}
		tuneCalibration( imp );
		return imp;
	}

	private ImagePlus readFolder( final File sourceFolder )
	{
		final ImagePlus imp = FolderOpener.open( sourceFolder.getAbsolutePath() );
		if ( null == imp )
		{
			logger.error( "Could not load from " + sourceFolder + ".\n" );
			return null;
		}
		tuneCalibration( imp );
		return imp;
	}

	private ImagePlus convertAndSave( final File sourceFolder, final File targetPathName )
	{
		final ImagePlus imp = readFolder( sourceFolder );
		final boolean saveOk = IJ.saveAsTiff( imp, targetPathName.getAbsolutePath() );
		if ( !saveOk )
		{
			logger.error( "Could not save to file " + targetPathName + ".\n" );
		}
		else
		{
			final File parent = sourceFolder.getParentFile();
			final File target = new File( parent, CONVERTED_FOLDER );
			logger.log( "Saved to " + targetPathName + ". Moving folder to " + target + "\n" );

			if ( !target.exists() )
			{
				final boolean mkdirOK = target.mkdirs();
				if ( !mkdirOK )
				{
					logger.error( "Could not create target move directory " + target + ". Not moving.\n" );
					return imp;
				}
			}
			final String name = sourceFolder.getName();
			final File moved = new File( target, name );
			final boolean moveOK = sourceFolder.renameTo( moved );
			if ( !moveOK )
			{
				logger.error( "Could not move " + sourceFolder + " to " + moved + ".\n" );
			}
		}
		return imp;
	}

	private void tuneCalibration( final ImagePlus imp )
	{
		final int[] dims = imp.getDimensions();
		if ( dims[ 4 ] == 1 && dims[ 3 ] > 1 )
		{
			imp.setDimensions( dims[ 2 ], dims[ 4 ], dims[ 3 ] );
			final Calibration calibration = imp.getCalibration();
			calibration.frameInterval = 1;
			calibration.setTimeUnit( "frame" );
		}
	}

	private String makeTargetName( final File folder )
	{
		final String last = folder.getName();
		return last + ".tif";

	}

}
