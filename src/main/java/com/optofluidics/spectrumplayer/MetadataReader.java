package com.optofluidics.spectrumplayer;

import ij.ImagePlus;
import ij.io.FileInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imglib2.algorithm.OutputAlgorithm;

public class MetadataReader implements OutputAlgorithm< List< Date > >
{

	private static final String BASE_ERR_MSG = "[MetadataReader] ";

	private final ImagePlus imp;

	private String errorMessage;

	private List< Date > frameTimestamps;

	public MetadataReader( final ImagePlus imp )
	{
		this.imp = imp;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{

		final FileInfo finfo = imp.getOriginalFileInfo();

		// Build file name
		final int dotIndex = finfo.fileName.lastIndexOf( ".ome.tif" );
		if ( dotIndex < 0 )
		{
			errorMessage = BASE_ERR_MSG + "The image file is not an .ome.tiff file.";
			return false;
		}
		final String mdFileName = finfo.fileName.substring( 0, dotIndex ) + "_metadata.txt";
		final File md = new File( finfo.directory, mdFileName );

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
						errorMessage = BASE_ERR_MSG + "Date found, but in incorrect format:\nGot " + dateStr + " and expected something like " + dateFormat;
						return false;
					}
					break;
				}
			}
		}
		catch ( final FileNotFoundException e )
		{
			errorMessage = BASE_ERR_MSG + "Could not find metadata file: " + md;
			return false;
		}
		finally
		{
			scanner.close();
		}

		if ( null == date )
		{
			errorMessage = BASE_ERR_MSG + "Could not find a date in the metadate file.";
			return false;
		}

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
			errorMessage = BASE_ERR_MSG + "Could not find metadata file: " + md;
			return false;
		}
		finally
		{
			scanner.close();
		}

		/*
		 * Build absolute date
		 */

		frameTimestamps = new ArrayList< Date >( elapsedTimes.size() );
		for ( final Integer dt : elapsedTimes )
		{
			final Date frameDate = new Date( date.getTime() + dt.longValue() );
			frameTimestamps.add( frameDate );
		}

		return true;
	}

	@Override
	public List< Date > getResult()
	{
		return frameTimestamps;
	}

}
