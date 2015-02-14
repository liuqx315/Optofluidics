package com.optofluidics.util;

import ij.IJ;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class OptofluidicsUtil
{

	public static final void setSystemLookAndFeel()
	{
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( final ClassNotFoundException e )
			{
				e.printStackTrace();
			}
			catch ( final InstantiationException e )
			{
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				e.printStackTrace();
			}
			catch ( final UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}
		}

	}
}
