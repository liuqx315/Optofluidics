package com.optofluidics.plugin;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.Arrays;

import com.optofluidics.trackmate.visualization.KymographGenerator;

public class StillSubtractor_ implements PlugIn
{

	public static enum Method
	{
		MEDIAN,
		MODE;
	}

	@Override
	public void run( final String arg )
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		subtract( imp, Method.MEDIAN );
	}

	public static void subtract( final ImagePlus imp, final Method method )
	{
		/*
		 * Get projection.
		 */

		final ImagePlus p;
		switch ( method )
		{
		case MODE:
			p = doModeProjection( imp );
			break;
		case MEDIAN:
		default:
			p = doMedianProjection( imp );
		}

		/*
		 * Subtract.
		 */

		final ImageStack stack = imp.getStack();
		final int sliceCount = stack.getSize();
		final ImageProcessor proj = p.getProcessor();

		final int width = proj.getWidth();
		final int height = proj.getHeight();

		for ( int slice = 0; slice < sliceCount; slice++ )
		{
			final ImageProcessor processor = imp.getStack().getProcessor( slice + 1 );

			for ( int y = 0; y < height; y++ )
			{
				for ( int x = 0; x < width; x++ )
				{
					final double value = processor.getf( x, y ) - proj.getf( x, y );
					processor.putPixelValue( x, y, value );
				}
			}
		}
	}

	private static ImagePlus doModeProjection( final ImagePlus imp )
	{
		final ImageStack stack = imp.getStack();
		final int sliceCount = stack.getSize();
		final ImageProcessor[] slices = new ImageProcessor[ sliceCount ];
		int index = 0;
		for ( int slice = 1; slice <= sliceCount; slice++ )
		{
			slices[ index++ ] = stack.getProcessor( slice );
		}

		final ImageProcessor ip2 = slices[ 0 ].duplicate();
		final float[] values = new float[ sliceCount ];
		final int width = ip2.getWidth();
		final int height = ip2.getHeight();
		for ( int y = 0; y < height; y++ )
		{
			for ( int x = 0; x < width; x++ )
			{
				for ( int i = 0; i < sliceCount; i++ )
				{
					values[ i ] = slices[ i ].getPixelValue( x, y );
				}
				ip2.putPixelValue( x, y, mode( values ) );
			}
		}
		return new ImagePlus( "Mode projection", ip2 );
	}

	/**
	 * Copied from the {@link ZProjector}.
	 *
	 * @return the median projection.
	 */
	private static final ImagePlus doMedianProjection( final ImagePlus imp )
	{
		final ImageStack stack = imp.getStack();
		final int sliceCount = stack.getSize();
		final ImageProcessor[] slices = new ImageProcessor[ sliceCount ];
		int index = 0;
		for ( int slice = 1; slice <= sliceCount; slice++ )
		{
			slices[ index++ ] = stack.getProcessor( slice );
		}

		final ImageProcessor ip2 = slices[ 0 ].duplicate();
		final float[] values = new float[ sliceCount ];
		final int width = ip2.getWidth();
		final int height = ip2.getHeight();
		for ( int y = 0; y < height; y++ )
		{
			for ( int x = 0; x < width; x++ )
			{
				for ( int i = 0; i < sliceCount; i++ )
				{
					values[ i ] = slices[ i ].getPixelValue( x, y );
				}
				ip2.putPixelValue( x, y, median( values ) );
			}
		}
		return new ImagePlus( "Median projection", ip2 );
	}

	private static final float median( final float[] a )
	{
		Arrays.sort( a );
		final int middle = a.length / 2;
		return a[ middle ];
	}

	private static final float mode( final float a[] )
	{
		float maxValue = 0;
		int maxCount = 0;

		for ( int i = 0; i < a.length; ++i )
		{
			int count = 0;
			for ( int j = 0; j < a.length; ++j )
			{
				if ( a[ j ] == a[ i ] )
					++count;
			}
			if ( count > maxCount )
			{
				maxCount = count;
				maxValue = a[ i ];
			}
		}
		return maxValue;
	}

	/*
	 * MAIN METHOD.
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "D:/Users/Jean-Yves/Desktop/83 - 2015-02-20 095535_ColumnSum.tif" );
		ImageJ.main( args );
		final ImagePlus imp = new ImagePlus( file.getAbsolutePath() );
		imp.show();

		KymographGenerator.fromLineImageVertical( imp ).show();

		System.out.println( "Subtracting." );
		subtract( imp, Method.MEDIAN );
		System.out.println( "Done." );

		KymographGenerator.fromLineImageVertical( imp ).show();

	}
}
