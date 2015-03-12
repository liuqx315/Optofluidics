package com.optofluidics.app;

import ij.IJ;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.optofluidics.OptofluidicsParameters;

public class OFAppUtils
{

	/**
	 * To be accepted as an Optofluidics parameter set, the specified file must:
	 * <ol>
	 * <li>be a file and not a directory.
	 * <li>have a file name that ends in '.properties'.
	 * <li>be present and readable.
	 * <li>be a properties file that can be read by the {@link Properties}
	 * class.
	 * <li>have a property called
	 * {@value OptofluidicsParameters#KEY_OPTOFLUIDICS_PARAMETERS} with a value
	 * equals to <code>true</code>.
	 * </ol>
	 * 
	 */
	private static final FileFilter OF_PARAMETERS_SET_FILTER = new FileFilter()
	{
		@Override
		public boolean accept( final File pathname )
		{
			if ( pathname.isDirectory() ) { return false; }
			if ( !pathname.getAbsolutePath().endsWith( ".properties" ) ) { return false; }
			InputStream stream;
			try
			{
				stream = new FileInputStream( pathname );
			}
			catch ( final FileNotFoundException e )
			{
				return false;
			}
			final Properties parameters = new Properties();
			try
			{
				parameters.load( stream );
			}
			catch ( final IOException e )
			{
				return false;
			}
			final String val = parameters.getProperty( OptofluidicsParameters.KEY_OPTOFLUIDICS_PARAMETERS );
			if ( null == val ) { return false; }
			return Boolean.parseBoolean( val );
		}
	};

	/**
	 * Returns the list of tracking parameter set files that can be found in the
	 * fiji folder.
	 * <p>
	 * To be accepted as an Optofluidics parameter set, the specified file must:
	 * <ol>
	 * <li>be a file and not a directory.
	 * <li>have a file name that ends in '.properties'.
	 * <li>be present and readable.
	 * <li>be a properties file that can be read by the {@link Properties}
	 * class.
	 * <li>have a property called
	 * {@value OptofluidicsParameters#KEY_OPTOFLUIDICS_PARAMETERS} with a value
	 * equals to <code>true</code>.
	 * </ol>
	 * 
	 * @return a new String array containing the list of parameter set files.
	 */
	public static final String[] getParameterSetList()
	{
		final File fijiDir = new File( IJ.getDirectory( "imagej" ) );
		final File[] files = fijiDir.listFiles( OF_PARAMETERS_SET_FILTER );
		final String[] names = new String[ files.length ];
		for ( int i = 0; i < names.length; i++ )
		{
			names[ i ] = files[ i ].getName();
		}
		return names;
	}

	public static final String wordWrap( final String input, final int width )
	{
		final StringBuilder sb = new StringBuilder( input );
		int i = 0;
		while ( i + width < sb.length() && ( i = sb.lastIndexOf( " ", i + width ) ) != -1 )
		{
			sb.replace( i, i + 1, "\n" );
		}
		return sb.toString();
	}

	public static final String getCommentsForParameterSet( final String parameterSetName )
	{
		final File tempFile = new File( IJ.getDirectory( "imagej" ), parameterSetName );
		final Properties temp = new Properties();
		try
		{
			final InputStream teampStream = new FileInputStream( tempFile );
			temp.load( teampStream );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			return "";
		}
		final String comments = temp.getProperty( "comments" );
		if ( null == comments ) { return ""; }
		return comments;
	}

	private OFAppUtils()
	{}
}
