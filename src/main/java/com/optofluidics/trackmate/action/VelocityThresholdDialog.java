package com.optofluidics.trackmate.action;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Font;
import java.awt.Frame;

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

	public VelocityThresholdDialog( final Frame frame, final double velocityThreshold, final int minConsecutiveFrames, final String velocityUnits )
	{
		super( frame, DIALOG_TITLE, true );

		final Font font = FONT.deriveFont( 12f );
		
		final JLabel lblThresholdTracksAbove = new JLabel( "Threshold tracks above velocity:" );
		lblThresholdTracksAbove.setFont( font );

		tfVelocityThreshold = new JNumericTextField( velocityThreshold );
		tfVelocityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		tfVelocityThreshold.setFont( font );
		tfVelocityThreshold.setColumns( 10 );
		tfVelocityThreshold.setFormat( "%.2f" );

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

		final JButton btnOk = new JButton( "OK" );

		final GroupLayout groupLayout = new GroupLayout( getContentPane() );
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addContainerGap()
								.addGroup( groupLayout.createParallelGroup( Alignment.TRAILING )
										.addComponent( lblMinConsecutiveFrames, GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE )
										.addComponent( lblThresholdTracksAbove, GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE )
										.addGroup( groupLayout.createSequentialGroup()
												.addComponent( tfVelocityThreshold, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE )
												.addPreferredGap( ComponentPlacement.UNRELATED )
												.addComponent( label, GroupLayout.PREFERRED_SIZE, 135, GroupLayout.PREFERRED_SIZE ) )
										.addGroup( groupLayout.createSequentialGroup()
												.addComponent( tfMinFrames, 95, 95, 95 )
												.addPreferredGap( ComponentPlacement.RELATED )
												.addComponent( lblFrames )
												.addGap( 98 ) )
										.addGroup( Alignment.LEADING, groupLayout.createSequentialGroup()
												.addComponent( btnCancel, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE )
												.addPreferredGap( ComponentPlacement.RELATED, 31, Short.MAX_VALUE )
												.addComponent( btnOk, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE )
												.addPreferredGap( ComponentPlacement.RELATED ) ) )
								.addGap( 1 ) )
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addGap( 23 )
								.addComponent( lblThresholdTracksAbove )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( tfVelocityThreshold, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
										.addComponent( label ) )
								.addGap( 32 )
								.addComponent( lblMinConsecutiveFrames )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( tfMinFrames, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
										.addComponent( lblFrames ) )
								.addPreferredGap( ComponentPlacement.RELATED, 50, Short.MAX_VALUE )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( btnCancel )
										.addComponent( btnOk ) )
								.addContainerGap() )
				);
		getContentPane().setLayout( groupLayout );
	}
}

