package com.optofluidics.trackmate.action;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;

public class VelocityThresholdDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private static final String DIALOG_TITLE = "Enter velocity threshold";

	private final JNumericTextField tfVelocityThreshold;

	private final JNumericTextField tfMinFrames;

	private final JNumericTextField tfSmoothWindow;

	private boolean wasCanceled;

	public VelocityThresholdDialog( final Frame frame, final double velocityThreshold, final int minConsecutiveFrames, final int smoothingWindow, final String velocityUnits )
	{
		super( frame, DIALOG_TITLE, true );
		setSize( new Dimension( 250, 260 ) );
		setResizable( false );
		super.setLocationRelativeTo( frame );

		setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final java.awt.event.WindowEvent e )
			{
				wasCanceled = true;
				close();
			};
		} );

		final Font font = FONT.deriveFont( 12f );

		final JLabel lblThresholdTracksAbove = new JLabel( "Threshold tracks above velocity:" );
		lblThresholdTracksAbove.setFont( font );

		tfVelocityThreshold = new JNumericTextField( velocityThreshold );
		tfVelocityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		tfVelocityThreshold.setFont( font );
		tfVelocityThreshold.setColumns( 10 );
		tfVelocityThreshold.setFormat( "%.4f" );

		final JLabel label = new JLabel( velocityUnits );
		label.setFont( font );

		final JLabel lblMinConsecutiveFrames = new JLabel( "Min. consecutive frames for splitting:" );
		lblMinConsecutiveFrames.setFont( font );

		tfMinFrames = new JNumericTextField( (double) minConsecutiveFrames );
		tfMinFrames.setHorizontalAlignment( SwingConstants.CENTER );
		tfMinFrames.setFont( font );
		tfMinFrames.setColumns( 10 );
		tfMinFrames.setFormat( "%.0f" );

		final JLabel lblFrames = new JLabel( "frames" );
		lblFrames.setFont( font );

		final JButton btnCancel = new JButton( "Cancel" );
		btnCancel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				wasCanceled = true;
				close();
			}
		} );

		final JButton btnOk = new JButton( "OK" );
		btnOk.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				wasCanceled = false;
				close();
			}
		} );

		final JLabel lblSmoothingWindow = new JLabel( "Smoothing window:" );
		lblSmoothingWindow.setFont( font );

		tfSmoothWindow = new JNumericTextField( ( double ) smoothingWindow );
		tfSmoothWindow.setHorizontalAlignment( SwingConstants.CENTER );
		tfSmoothWindow.setFont( font );
		tfSmoothWindow.setColumns( 10 );
		tfSmoothWindow.setFormat( "%.0f" );

		final JLabel lblFrames1 = new JLabel( "frames" );
		lblFrames1.setFont( font );

		final GroupLayout groupLayout = new GroupLayout( getContentPane() );
		groupLayout.setHorizontalGroup( groupLayout.createParallelGroup( Alignment.LEADING ).addGroup( groupLayout.createSequentialGroup().addContainerGap().addGroup( groupLayout.createParallelGroup( Alignment.LEADING ).addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup().addGroup( groupLayout.createParallelGroup( Alignment.LEADING ).addComponent( lblThresholdTracksAbove, GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE ).addGroup( groupLayout.createSequentialGroup().addComponent( tfVelocityThreshold, GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE ).addPreferredGap( ComponentPlacement.UNRELATED ).addComponent( label, GroupLayout.PREFERRED_SIZE, 135, GroupLayout.PREFERRED_SIZE ) ).addGroup( groupLayout.createSequentialGroup().addComponent( btnCancel, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE ).addPreferredGap( ComponentPlacement.RELATED, 50, Short.MAX_VALUE ).addComponent( btnOk, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE ).addGap( 23 ) ) ).addGap( 1 ) ).addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup().addGroup( groupLayout.createParallelGroup( Alignment.TRAILING ).addComponent( lblMinConsecutiveFrames, GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE ).addGroup( groupLayout.createSequentialGroup().addComponent( tfMinFrames, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE ).addPreferredGap( ComponentPlacement.UNRELATED ).addComponent( lblFrames ).addGap( 135 ) ) ).addGap( 1 ) ).addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup().addGroup( groupLayout.createParallelGroup( Alignment.TRAILING ).addComponent( lblSmoothingWindow, GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE ).addGroup( groupLayout.createSequentialGroup().addComponent( tfSmoothWindow, GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE ).addPreferredGap( ComponentPlacement.UNRELATED ).addComponent( lblFrames1, GroupLayout.PREFERRED_SIZE, 161, GroupLayout.PREFERRED_SIZE ).addGap( 6 ) ) ).addContainerGap() ) ) ) );
		groupLayout.setVerticalGroup( groupLayout.createParallelGroup( Alignment.LEADING ).addGroup( groupLayout.createSequentialGroup().addGap( 23 ).addComponent( lblThresholdTracksAbove ).addPreferredGap( ComponentPlacement.RELATED ).addGroup( groupLayout.createParallelGroup( Alignment.BASELINE ).addComponent( tfVelocityThreshold, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ).addComponent( label ) ).addGap( 18 ).addComponent( lblMinConsecutiveFrames ).addPreferredGap( ComponentPlacement.RELATED ).addGroup( groupLayout.createParallelGroup( Alignment.BASELINE ).addComponent( tfMinFrames, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ).addComponent( lblFrames ) ).addGap( 18 ).addComponent( lblSmoothingWindow ).addPreferredGap( ComponentPlacement.RELATED ).addGroup( groupLayout.createParallelGroup( Alignment.BASELINE ).addComponent( tfSmoothWindow, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ).addComponent( lblFrames1 ) ).addPreferredGap( ComponentPlacement.RELATED, 20, Short.MAX_VALUE ).addGroup( groupLayout.createParallelGroup( Alignment.BASELINE ).addComponent( btnCancel ).addComponent( btnOk ) ).addContainerGap() ) );
		getContentPane().setLayout( groupLayout );
	}

	private void close()
	{
		setVisible( false );
	}

	public int getSmoothWindow()
	{
		return ( int ) tfSmoothWindow.getValue();
	}

	public int getMinFrames()
	{
		return ( int ) tfMinFrames.getValue();
	}

	public double getVelocityThreshold()
	{
		return tfVelocityThreshold.getValue();
	}

	public boolean wasCanceled()
	{
		return wasCanceled;
	}

}

