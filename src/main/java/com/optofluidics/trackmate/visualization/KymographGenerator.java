package com.optofluidics.trackmate.visualization;


import ij.ImagePlus;
import ij.process.ImageProcessor;

public class KymographGenerator
{
	private KymographGenerator()
	{}

	public static final ImagePlus fromLineImageVertical( final ImagePlus imp )
	{
		if ( imp.getHeight() != 1 ) { throw new IllegalArgumentException( "KymographGenerator only accepts line image squences. Got a " + imp.getWidth() + " x " + imp.getHeight() + " image sequence." ); }
		final int stackSize = imp.getStackSize();
		final int width = imp.getWidth();

		final ImageProcessor targetProcessor = imp.getProcessor().createProcessor( width, stackSize );
		final Object targetPixels = targetProcessor.getPixels();
		for ( int i = 0; i < stackSize; i++ )
		{
			final Object sourcePixels = imp.getStack().getPixels( i + 1 );
			System.arraycopy( sourcePixels, 0, targetPixels, i * width, width );
		}
		return new ImagePlus( imp.getShortTitle() + "Kymograph", targetProcessor );
	}

	public static final ImagePlus fromLineImageHorizontal( final ImagePlus imp )
	{
		if ( imp.getHeight() != 1 ) { throw new IllegalArgumentException( "KymographGenerator only accepts line image squences. Got a " + imp.getWidth() + " x " + imp.getHeight() + " image sequence." ); }
		final int stackSize = imp.getStackSize();
		final int width = imp.getWidth();

		final ImageProcessor targetProcessor = imp.getProcessor().createProcessor( stackSize, width );
		for ( int i = 0; i < stackSize; i++ )
		{
			final ImageProcessor ip = imp.getStack().getProcessor( i + 1 );
			for ( int x = 0; x < width; x++ )
			{
				final int val = ip.get( x, 0 );
				targetProcessor.set( i, x, val );
			}
		}
		return new ImagePlus( imp.getShortTitle() + "Kymograph", targetProcessor );
	}
}
