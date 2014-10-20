package com.optofluidics.trackmate.action;

import ij.measure.ResultsTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.optofluidics.trackmate.features.track.TrackLinearVelocityAnalyzer;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.util.TMUtils;

public class VelocityAnalysisExporter
{

	private final Model model;

	private final List< String > trackFeatures;

	public VelocityAnalysisExporter( final Model model )
	{
		this.model = model;
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
		// Add track mean intensity analyzer TODO
		// Add track median intensity analyzer TODO
		// Add track std intensity analyzer TODO
		list.add( TrackVelocityThresholder.MEAN_VELOCITY_NO_PAUSES );
		list.add( TrackVelocityThresholder.NUMBER_OF_PAUSES );
		list.add( TrackVelocityThresholder.PAUSE_MEAN_DURATION );

		return list;
	}

	public void exportToImageJTable()
	{
		System.out.println( model );// DEBUG

		final FeatureModel fm = model.getFeatureModel();

		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );

		// Create table
		final ResultsTable trackTable = new ResultsTable();

		// Sort by track
		int trackNumber = 1;
		for ( final Integer trackID : trackIDs )
		{
			trackTable.incrementCounter();
			trackTable.addLabel( "" + trackNumber++ );
			for ( final String feature : trackFeatures )
			{
				System.out.println( feature );// DEBUG
				System.out.println( fm.getTrackFeatureDimensions().get( feature ) );// DEBUG

				final String dimStr = TMUtils.getUnitsFor( fm.getTrackFeatureDimensions().get( feature ), model.getSpaceUnits(), model.getTimeUnits() );
				final String featureStr = fm.getTrackFeatureNames().get( feature ) + " (" + dimStr + ")";

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
		trackTable.show( "Optofluidics velocity analysis" );
	}

}
