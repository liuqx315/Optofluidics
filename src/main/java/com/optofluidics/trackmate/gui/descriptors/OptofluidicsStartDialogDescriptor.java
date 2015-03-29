package com.optofluidics.trackmate.gui.descriptors;

import ij.ImagePlus;

import java.util.List;
import java.util.Map;

import com.optofluidics.trackmate.visualization.ProfileView;
import com.optofluidics.trackmate.visualization.ProfileViewHorizontalFactory;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class OptofluidicsStartDialogDescriptor extends StartDialogDescriptor
{

	private ProfileView mainView;

	private final TrackMateGUIController controller;

	public OptofluidicsStartDialogDescriptor( final TrackMateGUIController controller )
	{
		super( controller );
		this.controller = controller;
	}

	@Override
	public boolean isImpValid()
	{
		if ( !getComponent().isImpValid() ) { return false; }

		final TrackMate trackmate = controller.getPlugin();
		if ( null == trackmate.getSettings().imp )
		{
			return false;
		}
		else
		{
			final ImagePlus imp = trackmate.getSettings().imp;
			if ( imp.getHeight() != 1 ) { return false; }
		}
		return true;
	}

	@Override
	public void displayingPanel()
	{
		System.out.println( "Coucou!" );// DEBUG
		super.displayingPanel();
	}

	@Override
	public void aboutToHidePanel()
	{
		final TrackMate trackmate = controller.getPlugin();
		final Settings settings = trackmate.getSettings();
		final Model model = trackmate.getModel();

		/*
		 * Get settings and pass them to the trackmate managed by the wizard
		 */

		getComponent().updateTo( model, settings );
		trackmate.getModel().getLogger().log( settings.toStringImageInfo() );

		/*
		 * Configure settings object with spot, edge and track analyzers as
		 * specified in the providers.
		 */

		settings.clearSpotAnalyzerFactories();
		final SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
		{
			final SpotAnalyzerFactory< ? > spotFeatureAnalyzer = spotAnalyzerProvider.getFactory( key );
			settings.addSpotAnalyzerFactory( spotFeatureAnalyzer );
		}

		settings.clearEdgeAnalyzers();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
		{
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
			settings.addEdgeAnalyzer( edgeAnalyzer );
		}

		settings.clearTrackAnalyzers();
		final TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
		{
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
			settings.addTrackAnalyzer( trackAnalyzer );
		}

		trackmate.getModel().getLogger().log( settings.toStringFeatureAnalyzersInfo() );

		/*
		 * Launch the ImagePlus view now.
		 */

		// De-register old one, if any.
		if ( mainView != null )
		{
			mainView.clear();
			model.removeModelChangeListener( mainView );
		}

		final SelectionModel selectionModel = controller.getSelectionModel();
		mainView = new ProfileViewHorizontalFactory().create( model, settings, selectionModel );
		controller.getGuimodel().addView( mainView );
		final Map< String, Object > displaySettings = controller.getGuimodel().getDisplaySettings();
		for ( final String key : displaySettings.keySet() )
		{
			mainView.setDisplaySettings( key, displaySettings.get( key ) );
		}
		mainView.render();
	}

}
