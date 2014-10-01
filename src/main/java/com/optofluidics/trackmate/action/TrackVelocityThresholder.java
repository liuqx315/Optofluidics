package com.optofluidics.trackmate.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.imglib2.algorithm.Algorithm;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

public class TrackVelocityThresholder implements Algorithm
{


	private static final String BASE_ERR_MSG = "[TrackVelocityThresholder] ";

	private final Model model;

	private final double velocityThreshold;

	private final int minConsecutiveFrames;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final Comparator< ? super DefaultWeightedEdge > edgeTimeComparator;

	public TrackVelocityThresholder( final Model model, final double velocityThreshold, final int minConsecutiveFrames )
	{
		this.model = model;
		this.velocityThreshold = velocityThreshold;
		this.minConsecutiveFrames = minConsecutiveFrames;
		this.edgeTimeComparator = new EdgeTimeComparator( model.getFeatureModel() );
	}

	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	@Override
	public boolean checkInput()
	{
		if ( velocityThreshold <= 0 )
		{
			errorMessage = BASE_ERR_MSG + "Velocity threshold is negative or null.";
			return false;
		}
		if ( minConsecutiveFrames <= 0 )
		{
			errorMessage = BASE_ERR_MSG + "Min consecutive frame is negative or null.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{
		final TrackModel trackModel = model.getTrackModel();
		final FeatureModel fm = model.getFeatureModel();

		final Set< Integer > trackIDs = trackModel.unsortedTrackIDs( true );
		for ( final Integer id : trackIDs )
		{
			final Set< DefaultWeightedEdge > edges = trackModel.trackEdges( id );
			final List< DefaultWeightedEdge > ledges = new ArrayList< DefaultWeightedEdge >( edges );
			Collections.sort( ledges, edgeTimeComparator );

			boolean inGap = false;
			Collection< DefaultWeightedEdge > toRemove = null;
			int nImmobile = 0;
			int nSplits = 0;
			for ( final DefaultWeightedEdge edge : ledges )
			{
				final double v = fm.getEdgeFeature( edge, EdgeVelocityAnalyzer.VELOCITY );

				if ( v < velocityThreshold )
				{
					if ( inGap )
					{
						// Already in gap.
						nImmobile++;
					}
					else
					{
						// New gap.
						inGap = true;
						nImmobile = 1;
						toRemove = new ArrayList< DefaultWeightedEdge >();
					}
					toRemove.add( edge );
				}
				else
				{
					if ( inGap )
					{
						// Was in a gap, and leaving it.
						inGap = false;
						if ( nImmobile > minConsecutiveFrames )
						{
							// Gap is long enough; we can remove it.
							model.beginUpdate();
							try {
								for ( final DefaultWeightedEdge etr : toRemove )
								{
									model.removeEdge( etr );
								}
							}
							finally
							{
								model.endUpdate();
							}
						}
					}
					nSplits++;
				}
			}
			if ( nSplits > 0 )
			{
				logger.log( "Split track " + trackModel.name( id ) + " in " + nSplits + ( nSplits == 1 ? " segment." : " segments." ) + "\n" );
			}
			else
			{
				logger.log( "Left track " + trackModel.name( id ) + " intact.\n" );
			}
		}
		return true;
	}

	/*
	 * INNER CLASSES
	 */

	private static final class EdgeTimeComparator implements Comparator< DefaultWeightedEdge >
	{
		private final FeatureModel fm;

		public EdgeTimeComparator( final FeatureModel fm )
		{
			this.fm = fm;
		}

		@Override
		public int compare( final DefaultWeightedEdge e1, final DefaultWeightedEdge e2 )
		{
			final double t1 = fm.getEdgeFeature( e1, EdgeTimeLocationAnalyzer.TIME ).doubleValue();
			final double t2 = fm.getEdgeFeature( e2, EdgeTimeLocationAnalyzer.TIME ).doubleValue();

			if ( t1 < t2 ) { return -1; }
			if ( t1 > t2 ) { return 1; }
			return 0;
		}

	}

}
