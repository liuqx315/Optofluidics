package com.optofluidics.app;

import fiji.plugin.trackmate.Logger;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import com.optofluidics.Main;
import com.optofluidics.OptofluidicsParameters;
import com.optofluidics.OptofluidicsParameters.TrackerChoice;
import com.optofluidics.plugin.StillSubtractor_;
import com.optofluidics.plugin.StillSubtractor_.Method;
import com.optofluidics.util.OptofluidicsUtil;

public class OptofluidicsParametersEditorFrame extends JFrame implements PlugIn
{
	private static final long serialVersionUID = 1L;

	private static final ImageIcon RELOAD_ICON = new ImageIcon( Main.class.getResource( "icons/page_refresh.png" ) );

	private static final ImageIcon SAVE_ICON = new ImageIcon( Main.class.getResource( "icons/page_save.png" ) );

	private static final ImageIcon CANCEL_ICON = new ImageIcon( Main.class.getResource( "icons/cancel.png" ) );

	private static final Font MAIN_FONT = new Font( "Calibri", Font.PLAIN, 11 );

	private static final Font BOLD_FONT = new Font( "Calibri", Font.BOLD, 12 );

	private static final Font BIG_FONT = new Font( "Calibri", Font.PLAIN, 12 );

	private static final Font BIG_BOLD_FONT = new Font( "Calibri", Font.BOLD, 13 );

	private static final NumberFormat DECIMAL_FORMAT = NumberFormat.getNumberInstance();

	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance();

	private JFormattedTextField ftfParticleSize;

	private JFormattedTextField ftfQualityThreshold;

	private JComboBox comboBoxTracker;

	private JFormattedTextField tftInitialRadius;

	private JFormattedTextField tftSearchRadius;

	private JFormattedTextField tftMaxFrameGap;

	private JFormattedTextField tftFilterTrackDisplacement;

	private JFormattedTextField tftFilterNSpots;

	private JFormattedTextField tftVelocityThreshold;

	private JFormattedTextField tftMinConsFrames;

	private JFormattedTextField tftSmoothingWindow;

	private OptofluidicsParameters parameters;

	private JTextArea commentsTextArea;

	private JComboBox parameterSetsComboBox;

	private JComboBox comboBoxStillSub;

	/*
	 * CONSTRUCTOR
	 */


	public OptofluidicsParametersEditorFrame()
	{
		OptofluidicsUtil.setSystemLookAndFeel();
		setIconImage( Main.OPTOFLUIDICS_LARGE_ICON.getImage() );
		setTitle( "Optofluidics parameters editor " + Main.OPTOFLUIDICS_LIB_VERSION );
		setupGUI();
	}

	/*
	 * METHODS
	 */

	private void save()
	{
		// Comments
		parameters.setComments( commentsTextArea.getText() );

		// Detection.
		parameters.setStillSubtractionMethod( ( Method ) comboBoxStillSub.getSelectedItem() );
		parameters.setParticleDiameter( ( ( Number ) ftfParticleSize.getValue() ).doubleValue() );
		parameters.setQualityThreshold( ( ( Number ) ftfQualityThreshold.getValue() ).doubleValue() );

		// Tracking.
		parameters.setTrackerChoice( ( ( TrackerChoice ) comboBoxTracker.getSelectedItem() ) );
		parameters.setTrackInitRadius( ( ( Number ) tftInitialRadius.getValue() ).doubleValue() );
		parameters.setTrackSearchRadius( ( ( Number ) tftSearchRadius.getValue() ).doubleValue() );

		// Track filtering.
		parameters.setFilterMinNSpots( ( ( Number ) tftFilterNSpots.getValue() ).intValue() );
		parameters.setFilterTrackDisplacement( ( ( Number ) tftFilterTrackDisplacement.getValue() ).doubleValue() );

		// Velocity analysis.
		parameters.setSmoothingWindow( ( ( Number ) tftSmoothingWindow.getValue() ).intValue() );
		parameters.setVelocityThreshold( ( ( Number ) tftVelocityThreshold.getValue() ).doubleValue() );
		parameters.setMinConsecutiveFrames( ( ( Number ) tftMinConsFrames.getValue() ).intValue() );

		// Write this.
		parameters.write();
	}

	private void reload()
	{
		parameters.load();
		updateGUI();
	}

	private void updateGUI()
	{
		// Title and comments
		commentsTextArea.setText( parameters.getComments() );

		// Detection.
		comboBoxStillSub.setSelectedItem( parameters.getStillSubtractionMethod() );
		ftfParticleSize.setValue( Double.valueOf( parameters.getParticleDiameter() ) );
		ftfQualityThreshold.setValue( Double.valueOf( parameters.getQualityThreshold() ) );

		// Tracking.
		comboBoxTracker.setSelectedItem( parameters.getTrackerChoice() );
		tftInitialRadius.setValue( Double.valueOf( parameters.getTrackInitRadius() ) );
		tftSearchRadius.setValue( Double.valueOf( parameters.getTrackSearchRadius() ) );
		tftMaxFrameGap.setValue( Integer.valueOf( parameters.getMaxFrameGap() ) );

		// Track filtering.
		tftFilterNSpots.setValue( Integer.valueOf( parameters.getFilterMinNSpots() ) );
		tftFilterTrackDisplacement.setValue( Double.valueOf( parameters.getFilterTrackDisplacement() ) );

		// Velocity analysis.
		tftSmoothingWindow.setValue( Integer.valueOf( parameters.getSmoothingWindow() ) );
		tftVelocityThreshold.setValue( Double.valueOf( parameters.getVelocityThreshold() ) );
		tftMinConsFrames.setValue( Integer.valueOf( parameters.getMinConsecutiveFrames() ) );

	}

	private void setupGUI()
	{
		final PropertyChangeListener positiveChecker = new PositiveCheckPropertyListener();

		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		setBounds( 100, 100, 597, 431 );
		setResizable( false );

		final JPanel mainPanel = new JPanel();
		mainPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		mainPanel.setLayout( null );

		setContentPane( mainPanel );

		final JButton btnReloadFromFile = new JButton( "Reload from file", RELOAD_ICON );
		btnReloadFromFile.setBounds( 11, 359, 127, 32 );
		btnReloadFromFile.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				reload();
			}
		} );

		final JButton btnSaveToFile = new JButton( "Save to file", SAVE_ICON );
		btnSaveToFile.setBounds( 466, 359, 107, 32 );
		btnSaveToFile.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				save();
			}
		} );

		final JButton btnCancel = new JButton( "Cancel", CANCEL_ICON );
		btnCancel.setBounds( 361, 359, 95, 32 );
		btnCancel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				OptofluidicsParametersEditorFrame.this.dispatchEvent( new WindowEvent( OptofluidicsParametersEditorFrame.this, WindowEvent.WINDOW_CLOSING ) );
			}
		} );


		/*
		 * Detection panel
		 */

		final JPanel detectionPanel = new JPanel();
		detectionPanel.setLayout( null );
		detectionPanel.setBounds( 11, 113, 276, 115 );
		detectionPanel.setBorder( new LineBorder( new Color( 0, 0, 0 ) ) );

		final JLabel lblDetection = new JLabel( "Detection." );
		lblDetection.setSize(240, 16);
		lblDetection.setLocation(6, 6);
		lblDetection.setFont( BOLD_FONT );

		final JLabel lblStillSubtractionMethod = new JLabel( "Still sub. method:" );
		lblStillSubtractionMethod.setBounds( 6, 31, 81, 16 );
		lblStillSubtractionMethod.setFont( MAIN_FONT );

		comboBoxStillSub = new JComboBox( StillSubtractor_.Method.values() );
		comboBoxStillSub.setFont( MAIN_FONT );
		comboBoxStillSub.setBounds( 99, 26, 171, 27 );
		detectionPanel.add( comboBoxStillSub );


		final JLabel lblParticleSize = new JLabel( "Particle size:" );
		lblParticleSize.setSize( 110, 16 );
		lblParticleSize.setLocation( 6, 58 );
		lblParticleSize.setFont( MAIN_FONT );

		ftfParticleSize = new JFormattedTextField( DECIMAL_FORMAT );
		ftfParticleSize.setBounds( 126, 58, 60, 16 );
		ftfParticleSize.setFont( MAIN_FONT );
		ftfParticleSize.addPropertyChangeListener( "value", positiveChecker );

		final JLabel lblQualityThreshold = new JLabel( "Quality threshold:" );
		lblQualityThreshold.setSize( 110, 16 );
		lblQualityThreshold.setLocation( 6, 85 );
		lblQualityThreshold.setFont( MAIN_FONT );

		ftfQualityThreshold = new JFormattedTextField( DECIMAL_FORMAT );
		ftfQualityThreshold.setSize( 60, 16 );
		ftfQualityThreshold.setLocation( 126, 85 );
		ftfQualityThreshold.addPropertyChangeListener( "value", positiveChecker );
		ftfQualityThreshold.setFont( MAIN_FONT );

		detectionPanel.add( lblDetection );
		detectionPanel.add( lblStillSubtractionMethod );
		detectionPanel.add( lblParticleSize );
		detectionPanel.add( ftfParticleSize );
		detectionPanel.add( lblQualityThreshold );
		detectionPanel.add( ftfQualityThreshold );

		/*
		 * Tracking panel.
		 */

		final JPanel trackingPanel = new JPanel();
		trackingPanel.setBorder( new LineBorder( new Color( 0, 0, 0 ) ) );
		trackingPanel.setBounds( 297, 113, 276, 135 );
		mainPanel.add( trackingPanel );
		trackingPanel.setLayout( null );

		final JLabel lblTracking = new JLabel( "Tracking." );
		lblTracking.setBounds( 6, 6, 256, 16 );
		lblTracking.setFont( new Font( "Calibri", Font.BOLD, 12 ) );
		trackingPanel.add( lblTracking );

		final JLabel lblTracker = new JLabel( "Tracker:" );
		lblTracker.setFont( MAIN_FONT );
		lblTracker.setBounds( 6, 33, 106, 14 );
		trackingPanel.add( lblTracker );

		comboBoxTracker = new JComboBox( TrackerChoice.values() );
		comboBoxTracker.setFont( MAIN_FONT );
		comboBoxTracker.setBounds( 100, 26, 160, 26 );
		trackingPanel.add( comboBoxTracker );


		final JLabel lblInitialRadius = new JLabel( "Initial radius:" );
		lblInitialRadius.setFont( MAIN_FONT );
		lblInitialRadius.setBounds( 6, 58, 106, 14 );
		trackingPanel.add( lblInitialRadius );

		final JLabel lblSearchRadius = new JLabel( "Search radius:" );
		lblSearchRadius.setFont( MAIN_FONT );
		lblSearchRadius.setBounds( 6, 83, 106, 14 );
		trackingPanel.add( lblSearchRadius );

		final JLabel lblMaxFrameGap = new JLabel( "Max. frame gap:" );
		lblMaxFrameGap.setFont( MAIN_FONT );
		lblMaxFrameGap.setBounds( 6, 108, 106, 14 );
		trackingPanel.add( lblMaxFrameGap );

		tftInitialRadius = new JFormattedTextField( DECIMAL_FORMAT );
		tftInitialRadius.setBounds( 126, 58, 60, 16 );
		tftInitialRadius.setFont( MAIN_FONT );
		tftInitialRadius.addPropertyChangeListener( positiveChecker );
		trackingPanel.add( tftInitialRadius );

		tftSearchRadius = new JFormattedTextField( DECIMAL_FORMAT );
		tftSearchRadius.setBounds( 126, 83, 60, 16 );
		tftSearchRadius.setFont( MAIN_FONT );
		tftSearchRadius.addPropertyChangeListener( positiveChecker );
		trackingPanel.add( tftSearchRadius );

		tftMaxFrameGap = new JFormattedTextField( INTEGER_FORMAT );
		tftMaxFrameGap.setBounds( 126, 108, 60, 16 );
		tftMaxFrameGap.setFont( MAIN_FONT );
		tftMaxFrameGap.addPropertyChangeListener( positiveChecker );
		trackingPanel.add( tftMaxFrameGap );

		/*
		 * Track filtering.
		 */

		final JPanel trackFiltersPanel = new JPanel();
		trackFiltersPanel.setBorder( new LineBorder( new Color( 0, 0, 0 ) ) );
		trackFiltersPanel.setBounds( 297, 260, 276, 89 );
		mainPanel.add( trackFiltersPanel );
		trackFiltersPanel.setLayout( null );

		final JLabel lblTrackFilters = new JLabel( "Track filtering." );
		lblTrackFilters.setFont( BOLD_FONT );
		lblTrackFilters.setBounds( 6, 6, 240, 16 );
		trackFiltersPanel.add( lblTrackFilters );

		final JLabel lblMinSpots = new JLabel( "Min. N spots in tracks:" );
		lblMinSpots.setFont( MAIN_FONT );
		lblMinSpots.setBounds( 6, 33, 110, 16 );
		trackFiltersPanel.add( lblMinSpots );

		final JLabel lblTrackDisplacement = new JLabel( "Min. track displ.:" );
		lblTrackDisplacement.setFont( MAIN_FONT );
		lblTrackDisplacement.setBounds( 6, 60, 110, 16 );
		trackFiltersPanel.add( lblTrackDisplacement );

		tftFilterTrackDisplacement = new JFormattedTextField( DECIMAL_FORMAT );
		tftFilterTrackDisplacement.addPropertyChangeListener( positiveChecker );
		tftFilterTrackDisplacement.setFont( MAIN_FONT );
		tftFilterTrackDisplacement.setBounds( 126, 60, 60, 16 );
		trackFiltersPanel.add( tftFilterTrackDisplacement );

		tftFilterNSpots = new JFormattedTextField( INTEGER_FORMAT );
		tftFilterNSpots.addPropertyChangeListener( positiveChecker );
		tftFilterNSpots.setFont( MAIN_FONT );
		tftFilterNSpots.setBounds( 126, 33, 60, 16 );
		trackFiltersPanel.add( tftFilterNSpots );

		/*
		 * Velocity analysis.
		 */

		final JPanel panelVelocityAnalysis = new JPanel();
		panelVelocityAnalysis.setBorder( new LineBorder( new Color( 0, 0, 0 ) ) );
		panelVelocityAnalysis.setBounds( 11, 240, 276, 109 );
		mainPanel.add( panelVelocityAnalysis );
		panelVelocityAnalysis.setLayout( null );

		final JLabel lblVelocityAnalysis = new JLabel( "Velocity analysis." );
		lblVelocityAnalysis.setFont( BOLD_FONT );
		lblVelocityAnalysis.setBounds( 6, 6, 256, 14 );
		panelVelocityAnalysis.add( lblVelocityAnalysis );

		final JLabel lblSmoothingWindow = new JLabel( "Smoothing window:" );
		lblSmoothingWindow.setFont( MAIN_FONT );
		lblSmoothingWindow.setBounds( 6, 30, 106, 14 );
		panelVelocityAnalysis.add( lblSmoothingWindow );

		final JLabel lblVelocityThreshold = new JLabel( "Velocity threshold:" );
		lblVelocityThreshold.setFont( MAIN_FONT );
		lblVelocityThreshold.setBounds( 6, 55, 106, 14 );
		panelVelocityAnalysis.add( lblVelocityThreshold );

		final JLabel lblMinConsecutiveFrames = new JLabel( "Min. cons. frames:" );
		lblMinConsecutiveFrames.setFont( MAIN_FONT );
		lblMinConsecutiveFrames.setBounds( 6, 80, 106, 14 );
		panelVelocityAnalysis.add( lblMinConsecutiveFrames );

		tftVelocityThreshold = new JFormattedTextField( DECIMAL_FORMAT );
		tftVelocityThreshold.setFont( MAIN_FONT );
		tftVelocityThreshold.addPropertyChangeListener( positiveChecker );
		tftVelocityThreshold.setBounds( 126, 55, 60, 16 );
		panelVelocityAnalysis.add( tftVelocityThreshold );

		tftMinConsFrames = new JFormattedTextField( INTEGER_FORMAT );
		tftMinConsFrames.setFont( MAIN_FONT );
		tftMinConsFrames.addPropertyChangeListener( positiveChecker );
		tftMinConsFrames.setBounds( 126, 80, 60, 16 );
		panelVelocityAnalysis.add( tftMinConsFrames );

		tftSmoothingWindow = new JFormattedTextField( INTEGER_FORMAT );
		tftSmoothingWindow.setFont( MAIN_FONT );
		tftSmoothingWindow.addPropertyChangeListener( positiveChecker );
		tftSmoothingWindow.setBounds( 126, 30, 60, 16 );
		panelVelocityAnalysis.add( tftSmoothingWindow );

		/*
		 * Parameters and comments.
		 */

		final JLabel lblParameterSetName = new JLabel( "Parameter set name" );
		lblParameterSetName.setFont( BIG_BOLD_FONT );
		lblParameterSetName.setBounds( 11, 11, 195, 26 );

		commentsTextArea = new JTextArea();
		commentsTextArea.setWrapStyleWord( true );
		commentsTextArea.setLineWrap( true );
		commentsTextArea.setFont( BIG_FONT );
		commentsTextArea.setOpaque( false );
		commentsTextArea.setEditable( true );
		commentsTextArea.setBounds( 11, 48, 562, 54 );

		final Object[] names = OFAppUtils.getParameterSetList();
		parameterSetsComboBox = new JComboBox( names );
		parameterSetsComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final String parametersSetName = ( String ) parameterSetsComboBox.getSelectedItem();
				parameters = new OptofluidicsParameters( Logger.IJ_LOGGER, parametersSetName );
				reload();
			}
		} );
		parameterSetsComboBox.setBounds( 216, 11, 357, 26 );

		/*
		 * Add
		 */

		mainPanel.add( btnReloadFromFile );
		mainPanel.add( btnCancel );
		mainPanel.add( btnSaveToFile );
		mainPanel.add( trackingPanel );
		mainPanel.add( detectionPanel );

		mainPanel.add( commentsTextArea );
		mainPanel.add( lblParameterSetName );
		mainPanel.add( parameterSetsComboBox );
		setFocusTraversalPolicy( new FocusTraversalOnArray( new Component[] {
				parameterSetsComboBox,
				comboBoxStillSub, ftfParticleSize, ftfQualityThreshold,
				comboBoxTracker, tftInitialRadius, tftSearchRadius, tftMaxFrameGap,
				tftFilterNSpots, tftFilterTrackDisplacement,
				tftSmoothingWindow, tftVelocityThreshold, tftMinConsFrames,
				btnReloadFromFile, btnCancel, btnSaveToFile } ) );
	}

	/*
	 * INNER CLASSES
	 */

	private  class PositiveCheckPropertyListener implements PropertyChangeListener
	{
		@Override
		public void propertyChange( final PropertyChangeEvent evt )
		{
			final Object source = evt.getSource();
			final JFormattedTextField tf = ( JFormattedTextField ) source;
			final Number valObj = ( Number ) tf.getValue();
			if ( valObj == null ) { return; }
			final double value = valObj.doubleValue();
			if ( value < 0 )
			{
				tf.setValue( new Double( -value ) );
			}
			if ( value == 0 )
			{
				tf.setValue( new Double( 5 ) ); // DEFAULT
			}
		}
	}



	@Override
	public void run( String parametersSetName )
	{
		if ( null == parametersSetName || parametersSetName.isEmpty() )
		{
			final String[] parameterSets = OFAppUtils.getParameterSetList();
			if ( parameterSets.length > 0 )
			{
				parametersSetName = parameterSets[ 0 ];
			}
			else
			{
				parametersSetName = null;
			}
		}
		parameters = new OptofluidicsParameters( Logger.IJ_LOGGER, parametersSetName );
		updateGUI();
		setVisible( true );
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OptofluidicsParametersEditorFrame().run( null );
	}
}
