package com.optofluidics.trackmate.action;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.optofluidics.trackmate.features.manual.TrackPausingAnalyzer;
import com.optofluidics.trackmate.features.track.TrackLinearVelocityAnalyzer;
import com.optofluidics.trackmate.features.track.TrackSpotIntensityAnalyzer;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.util.TMUtils;

public class VelocityAnalysisExporter
{

	private static final String TABLE_NAME = "Optofluidics velocity analysis";

	private final Model model;

	private final List< String > trackFeatures;

	private final SelectionModel selectionModel;

	public VelocityAnalysisExporter( final Model model, final SelectionModel selectionModel )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.trackFeatures = createFeatureList();
	}

	protected List< String > createFeatureList()
	{
		final List< String > list = new ArrayList< String >();

		list.add( TrackBranchingAnalyzer.NUMBER_SPOTS );

		list.add( TrackDurationAnalyzer.TRACK_DISPLACEMENT );
		list.add( TrackDurationAnalyzer.TRACK_DURATION );
		list.add( TrackDurationAnalyzer.TRACK_START );
		list.add( TrackDurationAnalyzer.TRACK_STOP );

		list.add( TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED );
		list.add( TrackSpeedStatisticsAnalyzer.TRACK_MEDIAN_SPEED );
		list.add( TrackSpeedStatisticsAnalyzer.TRACK_STD_SPEED );

		list.add( TrackLinearVelocityAnalyzer.TRACK_LINEAR_VELOCITY );

		list.add( TrackSpotIntensityAnalyzer.TRACK_MEAN_INTENSITY );
		list.add( TrackSpotIntensityAnalyzer.TRACK_MEDIAN_INTENSITY );
		list.add( TrackSpotIntensityAnalyzer.TRACK_STD_INTENSITY );

		list.add( TrackPausingAnalyzer.MEAN_VELOCITY_NO_PAUSES );
		list.add( TrackPausingAnalyzer.NUMBER_OF_PAUSES );
		list.add( TrackPausingAnalyzer.PAUSE_MEAN_DURATION );

		list.add( TrackIndexAnalyzer.TRACK_ID );

		return list;
	}

	public void exportToImageJTable()
	{
		final FeatureModel fm = model.getFeatureModel();
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );

		// Create table
		final ResultsTable trackTable = new ResultsTable();

		// Sort by track
		final Integer[] ids = new Integer[ trackIDs.size() ];
		int idIndex = 0;
		for ( final Integer trackID : trackIDs )
		{
			ids[ idIndex++ ] = trackID;
			trackTable.incrementCounter();
			trackTable.addLabel( model.getTrackModel().name( trackID ) );
			for ( final String feature : trackFeatures )
			{
				final Dimension dimension = fm.getTrackFeatureDimensions().get( feature );
				final String dimStr;
				if ( dimension.equals( Dimension.NONE ) )
				{
					dimStr = "";
				}
				else
				{
					dimStr = " (" + TMUtils.getUnitsFor( fm.getTrackFeatureDimensions().get( feature ), model.getSpaceUnits(), model.getTimeUnits() ) + ")";
				}
				final String featureStr = fm.getTrackFeatureNames().get( feature ) + dimStr;

				final Double val = fm.getTrackFeature( trackID, feature );
				if ( null == val )
				{
					trackTable.addValue( featureStr, "None" );
				}
				else
				{
					if ( fm.getTrackFeatureIsInt().get( feature ).booleanValue() )
					{
						trackTable.addValue( featureStr, "" + val.intValue() );
					}
					else
					{
						trackTable.addValue( featureStr, val.doubleValue() );
					}
				}
			}
		}

		// Show tables
		trackTable.show( TABLE_NAME );

		// Hack to make the results table in sync with selection model.
		if ( null != selectionModel )
				{
			final TextWindow window = ( TextWindow ) WindowManager.getWindow( TABLE_NAME );
			final TextPanel textPanel = window.getTextPanel();
			textPanel.addMouseListener( new MouseAdapter()
					{
				@Override
				public void mouseClicked( final MouseEvent e )
				{
					final int line = textPanel.getSelectionStart();
					if ( line < 0 ) { return; }
					final Integer id = ids[ line ];
					final Set< DefaultWeightedEdge > edges = model.getTrackModel().trackEdges( id );
					final Set< Spot > spots = model.getTrackModel().trackSpots( id );

					selectionModel.clearSelection();
					if ( null != edges )
					{
						selectionModel.addEdgeToSelection( edges );
					}
					if ( null != spots )
					{
						selectionModel.addSpotToSelection( spots );
					}
				};

			} );
		}
	}

}
