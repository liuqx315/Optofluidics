package com.optofluidics.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Map;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class ProfileOverlay implements Overlay
{

	private final Map< String, Object > displaySettings;

	private int frame;

	private Collection< DefaultWeightedEdge > edgeSelection;

	private Collection< Spot > spotSelection;

	private final SpotCollection spots;

	private FontMetrics fm;

	public ProfileOverlay( final Model model, final Map< String, Object > displaySettings )
	{
		this.spots = model.getSpots();
		this.displaySettings = displaySettings;
	}

	@Override
	public void paintOverlay( final Graphics2D g2, final ChartPanel chartPanel )
	{
		final JFreeChart chart = chartPanel.getChart();
		final XYPlot plot = chart.getXYPlot();
		final Rectangle2D plotArea = chartPanel.getScreenDataArea();

		/*
		 * Spots
		 */

		final boolean spotVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_SPOTS_VISIBLE );
		if ( spotVisible && spots.getNSpots( true ) != 0 )
		{
			final double radiusRatio = ( Double ) displaySettings.get( TrackMateModelView.KEY_SPOT_RADIUS_RATIO );
			@SuppressWarnings( "unchecked" )
			final FeatureColorGenerator< Spot > colorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );

			g2.setStroke( new BasicStroke( 1.0f ) );
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			final boolean spotNameVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_DISPLAY_SPOT_NAMES );
			fm = g2.getFontMetrics();

			for ( final Spot spot : spots.iterable( frame, true ) )
			{

				if ( spotSelection != null && spotSelection.contains( spot ) )
				{
					continue;
				}

				final Color color = colorGenerator.color( spot );
				g2.setColor( color );

				drawSpot( spot, g2, plot, plotArea, radiusRatio, spotNameVisible );
			}

			// Deal with spot selection
			if ( null != spotSelection )
			{
				g2.setStroke( new BasicStroke( 2.0f ) );
				g2.setColor( TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR );
				for ( final Spot spot : spotSelection )
				{
					final int sFrame = spot.getFeature( Spot.FRAME ).intValue();
					if ( sFrame != frame )
					{
						continue;
					}
					drawSpot( spot, g2, plot, plotArea, radiusRatio, spotNameVisible );
				}
			}
		}

	}

	protected void drawSpot( final Spot spot, final Graphics2D g2, final XYPlot plot, final Rectangle2D plotArea, final double radiusRatio, final boolean spotNameVisible )
	{
		final double xval = spot.getDoublePosition( 0 );
		final double yval = spot.getFeature( SpotIntensityAnalyzerFactory.MAX_INTENSITY ).doubleValue();
		final double radiusVal = radiusRatio * spot.getFeature( Spot.RADIUS ).doubleValue();

		final int px = ( int ) plot.getDomainAxis().valueToJava2D( xval, plotArea, plot.getDomainAxisEdge() );
		final int radius = ( int ) ( plot.getDomainAxis().valueToJava2D( radiusVal, plotArea, plot.getDomainAxisEdge() ) - plot.getDomainAxis().valueToJava2D( 0, plotArea, plot.getDomainAxisEdge() ) );
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
