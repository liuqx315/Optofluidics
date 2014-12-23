package com.optofluidics.spectrumplayer;

import fiji.plugin.trackmate.TrackMatePlugIn_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.File;

public class ColumnProjector_ implements PlugIn
{

	@Override
	public void run( final String arg )
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( null == imp ) { return; }
		toColumnSum( imp ).show( "Column sum done." );
	}

	public static final ImagePlus toColumnSum( final ImagePlus imp )
	{
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		final int nslices = imp.getStackSize();
		final ImageStack stack = new ImageStack( width, 1, nslices );

		for ( int i = 0; i < nslices; i++ )
		{
			final ImageProcessor ip = imp.getStack().getProcessor( i + 1 );
			final float[] pixels = new float[ width ];
			for ( int j = 0; j < width; j++ )
			{
				float sum = 0;
				for ( int k = 0; k < height; k++ )
				{
					sum += ip.getf( j, k );
				}
				pixels[ j ] = sum;
			}

			final FloatProcessor fp = new FloatProcessor( width, 1, pixels );
			stack.setProcessor( fp, i + 1 );
			IJ.showProgress( i, nslices );
		}

		final ImagePlus target = new ImagePlus( imp.getShortTitle() + "_ColumnSum", stack );
		target.setDimensions( imp.getNChannels(), imp.getNSlices(), imp.getNFrames() );
		target.setCalibration( imp.getCalibration() );

		return target;
	}

	/*
	 * MAIN method
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final File file = new File( "samples/Pos0.tif" );
		final ImagePlus imp = new ImagePlus( file.getAbsolutePath() );
		imp.show();

		new ColumnProjector_().run( null );

		new TrackMatePlugIn_().run( null );
	}

}
