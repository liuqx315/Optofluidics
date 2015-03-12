package com.optofluidics.app;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.MultiLineLabel;

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.optofluidics.Main;
import com.optofluidics.util.OptofluidicsUtil;

public class OptofluidicsParametersChooser
{
	private static final String HELP_TEXT = "<html>"
			+ "This GUI element will look in the Fiji folder ("
			+ IJ.getDirectory( "imagej" ) + ") "
			+ "and list the Optofluidics tracking parameters set files it finds there."
			+ "</html>";

	private final GenericDialogPlus dialog;

	private static String parameterSetName;

	public OptofluidicsParametersChooser()
	{
		dialog = new GenericDialogPlus( "Optofluidics parameters set chooser " + Main.OPTOFLUIDICS_LIB_VERSION );
	}

	public String getUserChoice()
	{
		OptofluidicsUtil.setSystemLookAndFeel();

		dialog.addImage( Main.OPTOFLUIDICS_ORANGE_LOGO );

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
			dialog.addMessage( "A\nB\nC\nD" );
			final MultiLineLabel message = ( MultiLineLabel ) dialog.getMessage();

			@SuppressWarnings( "unchecked" )
			final Vector< Choice > choices = dialog.getChoices();
			final Choice choice = choices.get( 0 );
			choice.addItemListener( new ItemListener()
			{
				@Override
				public void itemStateChanged( final ItemEvent arg0 )
				{
					showComments( choice, message );
				}
			} );
			showComments( choice, message );
		}
		else
		{
			dialog.addMessage( "No parameters set found. Relying on defaults." );
		}

		dialog.addHelp( HELP_TEXT );
		dialog.showDialog();

		if ( !dialog.wasOKed() ) { return null; }

		if ( availableParameters != null && availableParameters.length > 0 )
		{
			parameterSetName = dialog.getNextChoice();
			return parameterSetName;
		}
		else
		{
			return null;
		}
	}

	public boolean wasOKed()
	{
		return dialog.wasOKed();
	}

	private static final void showComments( final Choice choice, final MultiLineLabel message )
	{
		final File tempFile = new File( IJ.getDirectory( "imagej" ), choice.getSelectedItem() );
		final Properties temp = new Properties();
		try
		{
			final InputStream teampStream = new FileInputStream( tempFile );
			temp.load( teampStream );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		final String comments = temp.getProperty( "comments" );
		message.setText( OFAppUtils.wordWrap( comments, 50 ) );
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
		System.out.println( new OptofluidicsParametersChooser().getUserChoice() );
			}
		} );
	}
}
