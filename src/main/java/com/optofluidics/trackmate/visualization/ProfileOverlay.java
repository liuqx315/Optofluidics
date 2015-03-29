package com.optofluidics.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class ProfileOverlay implements Overlay
{

	private final Map< String, Object > displaySettings;

	private int frame;

	private Collection< DefaultWeightedEdge > edgeSelection = new HashSet< DefaultWeightedEdge >();

	private Collection< Spot > spotSelection = new HashSet< Spot >();

	private FontMetrics fm;

	private final Model model;

	private boolean displayTracks = true;

	private PerTrackFeatureColorGenerator trackFeatureColorGenerator;

	public ProfileOverlay( final Model model, final Map< String, Object > displaySettings )
	{
		this.model = model;
		this.displaySettings = displaySettings;
	}

	public void setTrackColorGenerator( final PerTrackFeatureColorGenerator colorGenerator )
	{
		this.trackFeatureColorGenerator = colorGenerator;
	}

	public void setDisplayTracks( final boolean displayTracks )
	{
		this.displayTracks = displayTracks;
	}

	@Override
	public void paintOverlay( final Graphics2D g2d, final ChartPanel chartPanel )
	{
		final JFreeChart chart = chartPanel.getChart();
		final XYPlot plot = chart.getXYPlot();
		final Rectangle2D plotArea = chartPanel.getScreenDataArea();

		/*
		 * Spots
		 */

		final boolean spotVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_SPOTS_VISIBLE );
		if ( spotVisible && model.getSpots().getNSpots( true ) > 0 )
		{
			final double radiusRatio = ( Double ) displaySettings.get( TrackMateModelView.KEY_SPOT_RADIUS_RATIO );
			@SuppressWarnings( "unchecked" )
			final FeatureColorGenerator< Spot > colorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );

			g2d.setStroke( new BasicStroke( 1.0f ) );
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			final boolean spotNameVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_DISPLAY_SPOT_NAMES );
			fm = g2d.getFontMetrics();

			for ( final Spot spot : model.getSpots().iterable( frame, true ) )
			{

				if ( spotSelection != null && spotSelection.contains( spot ) )
				{
					continue;
				}

				final Color color;
				if ( trackFeatureColorGenerator == null )
				{
					color = colorGenerator.color( spot );

				}
				else
				{
					color = trackFeatureColorGenerator.colorOf( model.getTrackModel().trackIDOf( spot ) );
				}
				g2d.setColor( color );

				drawSpot( spot, g2d, plot, plotArea, radiusRatio, spotNameVisible );
			}

			// Deal with spot selection
			if ( null != spotSelection )
			{
				g2d.setStroke( new BasicStroke( 2.0f ) );
				g2d.setColor( TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR );
				for ( final Spot spot : spotSelection )
				{
					final int sFrame = spot.getFeature( Spot.FRAME ).intValue();
					if ( sFrame != frame )
					{
						continue;
					}
					drawSpot( spot, g2d, plot, plotArea, radiusRatio, spotNameVisible );
				}
			}
		}

		/*
		 * Tracks
		 */

		final boolean tracksVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_TRACKS_VISIBLE );
		if ( displayTracks && tracksVisible && model.getTrackModel().nTracks( true ) > 0 )
		{

			final int trackDisplayMode = ( Integer ) displaySettings.get( TrackMateModelView.KEY_TRACK_DISPLAY_MODE );
			final int trackDisplayDepth = ( Integer ) displaySettings.get( TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH );

			// Determine bounds for limited view modes
			int minT = 0;
			int maxT = 0;
			switch ( trackDisplayMode )
			{
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
				minT = frame - trackDisplayDepth;
				maxT = frame + trackDisplayDepth;
				break;
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
				minT = frame;
				maxT = frame + trackDisplayDepth;
				break;
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
				minT = frame - trackDisplayDepth;
				maxT = frame;
				break;
			}

			float transparency;
			Spot source, target;
			final Set< Integer > filteredTrackKeys = model.getTrackModel().unsortedTrackIDs( true );

			// Deal with highlighted edges first: brute and thick display
			g2d.setStroke( new BasicStroke( 4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
			g2d.setColor( TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR );
			for ( final DefaultWeightedEdge edge : edgeSelection )
			{
				source = model.getTrackModel().getEdgeSource( edge );
				target = model.getTrackModel().getEdgeTarget( edge );
				drawEdge( source, target, g2d, plot, plotArea );
			}

			// The rest
			final TrackColorGenerator colorGenerator = ( TrackColorGenerator ) displaySettings.get( TrackMateModelView.KEY_TRACK_COLORING );
			g2d.setStroke( new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
			if ( trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK )
			{
				g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER ) );
			}

			switch ( trackDisplayMode )
			{

			case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE:
			{
				for ( final Integer trackID : filteredTrackKeys )
				{
					colorGenerator.setCurrentTrackID( trackID );
					Set< DefaultWeightedEdge > track;
					synchronized ( model )
					{
						track = new HashSet< DefaultWeightedEdge >( model.getTrackModel().trackEdges( trackID ) );
					}
					for ( final DefaultWeightedEdge edge : track )
					{
						if ( edgeSelection.contains( edge ) )
						{
							continue;
						}

						source = model.getTrackModel().getEdgeSource( edge );
						target = model.getTrackModel().getEdgeTarget( edge );
						g2d.setColor( colorGenerator.color( edge ) );
						drawEdge( source, target, g2d, plot, plotArea );
					}
				}
				break;
			}

			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
			{

				g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );

				for ( final Integer trackID : filteredTrackKeys )
				{
					colorGenerator.setCurrentTrackID( trackID );
					Set< DefaultWeightedEdge > track;
					synchronized ( model )
					{
						track = new HashSet< DefaultWeightedEdge >( model.getTrackModel().trackEdges( trackID ) );
					}
					for ( final DefaultWeightedEdge edge : track )
					{
						if ( edgeSelection.contains( edge ) )
						{
							continue;
						}

						source = model.getTrackModel().getEdgeSource( edge );
						final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						if ( sourceFrame < minT || sourceFrame >= maxT )
						{
							continue;
						}

						target = model.getTrackModel().getEdgeTarget( edge );
						g2d.setColor( colorGenerator.color( edge ) );
						drawEdge( source, target, g2d, plot, plotArea );
					}
				}
				break;
			}

			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
			case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
			{

				g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

				for ( final Integer trackID : filteredTrackKeys )
				{
					colorGenerator.setCurrentTrackID( trackID );
					final Set< DefaultWeightedEdge > track;
					synchronized ( model )
					{
						track = new HashSet< DefaultWeightedEdge >( model.getTrackModel().trackEdges( trackID ) );
					}
					for ( final DefaultWeightedEdge edge : track )
					{
						if ( edgeSelection.contains( edge ) )
						{
							continue;
						}

						source = model.getTrackModel().getEdgeSource( edge );
						final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						if ( sourceFrame < minT || sourceFrame >= maxT )
						{
							continue;
						}

						transparency = ( float ) ( 1 - Math.abs( ( double ) sourceFrame - frame ) / trackDisplayDepth );
						target = model.getTrackModel().getEdgeTarget( edge );
						g2d.setColor( colorGenerator.color( edge ) );
						drawEdge( source, target, g2d, plot, plotArea, transparency );
					}
				}
				break;

			}

			}

		}

	}

	protected void drawEdge( final Spot source, final Spot target, final Graphics2D g2d, final XYPlot plot, final Rectangle2D plotArea, final float transparency )
	{
		final double x1val = source.getDoublePosition( 0 );
		final double y1val = source.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY ).doubleValue();
		final double x2val = target.getDoublePosition( 0 );
		final double y2val = target.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY ).doubleValue();

		final int px1 = ( int ) plot.getDomainAxis().valueToJava2D( x1val, plotArea, plot.getDomainAxisEdge() );
		final int py1 = ( int ) plot.getRangeAxis().valueToJava2D( y1val, plotArea, plot.getRangeAxisEdge() );
		final int px2 = ( int ) plot.getDomainAxis().valueToJava2D( x2val, plotArea, plot.getDomainAxisEdge() );
		final int py2 = ( int ) plot.getRangeAxis().valueToJava2D( y2val, plotArea, plot.getRangeAxisEdge() );

		if ( px1 < plotArea.getMinX() || px1 > plotArea.getMaxX() || px2 < plotArea.getMinX() || px2 > plotArea.getMaxX() ) { return; }
		if ( px1 == px2 && py1 == py2 ) { return; }

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, transparency ) );
		g2d.drawLine( px1, py1, px2, py2 );
	}

	protected void drawEdge( final Spot source, final Spot target, final Graphics2D g2d, final XYPlot plot, final Rectangle2D plotArea )
	{
		final double x1val = source.getDoublePosition( 0 );
		final double y1val = source.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY ).doubleValue();
		final double x2val = target.getDoublePosition( 0 );
		final double y2val = target.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY ).doubleValue();

		final int px1 = ( int ) plot.getDomainAxis().valueToJava2D( x1val, plotArea, plot.getDomainAxisEdge() );
		final int py1 = ( int ) plot.getRangeAxis().valueToJava2D( y1val, plotArea, plot.getRangeAxisEdge() );
		final int px2 = ( int ) plot.getDomainAxis().valueToJava2D( x2val, plotArea, plot.getDomainAxisEdge() );
		final int py2 = ( int ) plot.getRangeAxis().valueToJava2D( y2val, plotArea, plot.getRangeAxisEdge() );

		if ( px1 < plotArea.getMinX() || px1 > plotArea.getMaxX() || px2 < plotArea.getMinX() || px2 > plotArea.getMaxX() ) { return; }
		if ( px1 == px2 && py1 == py2 ) { return; }

		g2d.drawLine( px1, py1, px2, py2 );
	}

	protected void drawSpot( final Spot spot, final Graphics2D g2, final XYPlot plot, final Rectangle2D plotArea, final double radiusRatio, final boolean spotNameVisible )
	{
		final double xval = spot.getDoublePosition( 0 );

		/*
		 * Try to guess a decent Y position. We can rely on the max intensity of
		 * the spot. But if this feature is not there yet, we have to get it
		 * somehow.
		 */
		final Double yfeat = spot.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY );
		final double yval;
		if ( yfeat != null )
		{
			yval = yfeat.doubleValue();
		}
		else
		{
			final DefaultXYDataset dataset = ( DefaultXYDataset ) plot.getDataset();
			final double prec = dataset.getXValue( 0, 1 ) - dataset.getXValue( 0, 0 );
			yval = binSearch( dataset, spot.getDoublePosition( 0 ), 0, dataset.getItemCount( 0 ), prec );
		}
		final double radiusVal = radiusRatio * spot.getFeature( Spot.RADIUS ).doubleValue();

		final int px = ( int ) plot.getDomainAxis().valueToJava2D( xval, plotArea, plot.getDomainAxisEdge() );
		int radius = ( int ) ( plot.getDomainAxis().valueToJava2D( radiusVal, plotArea, plot.getDomainAxisEdge() ) - plot.getDomainAxis().valueToJava2D( 0, plotArea, plot.getDomainAxisEdge() ) );
		if ( radius < 3 )
		{
			radius = 3;
		}
		final int py = ( int ) plot.getRangeAxis().valueToJava2D( yval, plotArea, plot.getRangeAxisEdge() );

		if ( px < plotArea.getMinX() || px > plotArea.getMaxX() ) { return; }

		g2.fillOval( px - radius, ( int ) ( py - 2.5 * radius ), 2 * radius, 2 * radius );

		if ( spotNameVisible )
		{
			final String str = spot.toString();

			final int xindent = fm.stringWidth( str );
			int xtext = px + radius + 5;
			if ( xtext + xindent > plotArea.getMaxX() )
			{
				xtext = px - radius - 5 - xindent;
			}

			final int yindent = fm.getAscent() / 2;
			final int ytext = py + yindent;;

			g2.drawString( spot.toString(), xtext, ytext );
		}
	}

	private static double binSearch( final DefaultXYDataset dataset, final double key, final int low, final int high, final double prec )
	{
		if ( low > high ) { return Double.NaN; }

		final int middle = ( high + low ) / 2;
		final double diff = key - dataset.getXValue( 0, middle );
		if ( Math.abs( diff ) <= prec )
		{
			return dataset.getYValue( 0, middle );
		}
		else if ( diff > 0 )
		{
			return binSearch( dataset, key, middle + 1, high, prec );
		}
		else
		{
			return binSearch( dataset, key, low, middle - 1, prec );
		}
	}

	@Override
	public void addChangeListener( final OverlayChangeListener listener )
	{}

	@Override
	public void removeChangeListener( final OverlayChangeListener listener )
	{}

	public void setFrame( final int frame )
	{
		this.frame = frame;
	}

	public void setHighlight( final Collection< DefaultWeightedEdge > edges )
	{
		this.edgeSelection = edges;
	}

	public void setSpotSelection( final Collection< Spot > spots )
	{
		this.spotSelection = spots;
	}

}
