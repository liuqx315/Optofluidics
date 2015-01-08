package com.optofluidics.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.optofluidics.trackmate.visualization.ProfileView.ProfileViewOrientation;

public class KymographOverlay extends Roi
{
	private static final long serialVersionUID = 1L;

	private static final Color FRAME_BOX_COLOR = Color.YELLOW;

	private final Composite composite = AlphaComposite.getInstance( AlphaComposite.SRC_OVER );

	private final Model model;

	private final Map< String, Object > displaySettings;

	protected Collection< DefaultWeightedEdge > highlight = new HashSet< DefaultWeightedEdge >();

	private int frame;

	private final double dx;

	private final ProfileViewOrientation orientation;

	public KymographOverlay( final Model model, final ImagePlus imp, final Map< String, Object > displaySettings, final double dx, final ProfileViewOrientation orientation )
	{
		super( 0, 0, imp );
		this.model = model;
		this.displaySettings = displaySettings;
		this.dx = dx;
		this.orientation = orientation;
	}

	@Override
	public void drawOverlay( final Graphics g )
	{
		final int xcorner = ic.offScreenX( 0 );
		final int ycorner = ic.offScreenY( 0 );
		final double magnification = getMagnification();

		final Graphics2D g2d = ( Graphics2D ) g;
		g2d.setComposite( composite );

		/*
		 * Rectangle specifying current frame
		 */

		final int xp1;
		final int xp2;
		final int yp1;
		final int yp2;
		switch ( orientation )
		{
		case HORIZONTAL:
			yp1 = 0;
			yp2 = imp.getHeight();
			xp1 = frame;
			xp2 = frame + 1;
			break;

		default:
			xp1 = 0;
			xp2 = imp.getWidth();
			yp1 = frame;
			yp2 = frame + 1;
			break;
		}

		final int xs1 = ( int ) ( ( xp1 - xcorner ) * magnification );
		final int ys1 = ( int ) ( ( yp1 - ycorner ) * magnification );
		final int xs2 = ( int ) ( ( xp2 - xcorner ) * magnification ) - 1;
		final int ys2 = ( int ) ( ( yp2 - ycorner ) * magnification ) - 1;

		g2d.setColor( FRAME_BOX_COLOR );
		if ( ys2 <= ys1 )
		{
			g2d.drawLine( xs1, ys1, xs2, ys1 );
		}
		else
		{
			g2d.drawRect( xs1, ys1, xs2 - xs1, ys2 - ys1 );

		}


		/*
		 * Tracks
		 */

		final boolean tracksVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_TRACKS_VISIBLE );
		if ( !tracksVisible || model.getTrackModel().nTracks( true ) == 0 ) {
			return;
		}

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
		for ( final DefaultWeightedEdge edge : highlight )
		{
			source = model.getTrackModel().getEdgeSource( edge );
			target = model.getTrackModel().getEdgeTarget( edge );
			drawEdge( g2d, source, target, xcorner, ycorner, magnification );
		}

		// The rest
		final TrackColorGenerator colorGenerator = ( TrackColorGenerator ) displaySettings.get( TrackMateModelView.KEY_TRACK_COLORING );
		g2d.setStroke( new BasicStroke( 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
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
					if ( highlight.contains( edge ) )
					{
						continue;
					}

					source = model.getTrackModel().getEdgeSource( edge );
					target = model.getTrackModel().getEdgeTarget( edge );
					g2d.setColor( colorGenerator.color( edge ) );
					drawEdge( g2d, source, target, xcorner, ycorner, magnification );
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
					if ( highlight.contains( edge ) )
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
					drawEdge( g2d, source, target, xcorner, ycorner, magnification );
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
					if ( highlight.contains( edge ) )
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
					drawEdge( g2d, source, target, xcorner, ycorner, magnification, transparency );
				}
			}
			break;

		}

		}
	}

	private void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final int xcorner, final int ycorner, final double magnification, final float transparency )
	{
		// Find x & y in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double x1i = target.getFeature( Spot.POSITION_X );
		// In pixel units
		final double x0p = x0i / dx + 0.5;
		final double y0p = source.getFeature( Spot.FRAME ).intValue() + 0.5;
		final double x1p = x1i / dx + 0.5;
		final double y1p = target.getFeature( Spot.FRAME ).intValue() + 0.5;
		// Scale to image zoom
		final double x0s = ( x0p - xcorner ) * magnification;
		final double y0s = ( y0p - ycorner ) * magnification;
		final double x1s = ( x1p - xcorner ) * magnification;
		final double y1s = ( y1p - ycorner ) * magnification;
		// Round
		final int x0 = ( int ) Math.round( x0s );
		final int y0 = ( int ) Math.round( y0s );
		final int x1 = ( int ) Math.round( x1s );
		final int y1 = ( int ) Math.round( y1s );

		if ( x0 == x1 && y0 == y1 ) { return; }

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, transparency ) );
		g2d.drawLine( x0, y0, x1, y1 );
	}

	private void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final int xcorner, final int ycorner, final double magnification )
	{
		// Find x & y in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double x1i = target.getFeature( Spot.POSITION_X );
		// In pixel units
		final double x0p = x0i / dx + 0.5;
		final double y0p = source.getFeature( Spot.FRAME ).intValue() + 0.5;
		final double x1p = x1i / dx + 0.5;
		final double y1p = target.getFeature( Spot.FRAME ).intValue() + 0.5;
		// Scale to image zoom
		final double x0s = ( x0p - xcorner ) * magnification;
		final double y0s = ( y0p - ycorner ) * magnification;
		final double x1s = ( x1p - xcorner ) * magnification;
		final double y1s = ( y1p - ycorner ) * magnification;
		// Round
		final int x0 = ( int ) Math.round( x0s );
		final int y0 = ( int ) Math.round( y0s );
		final int x1 = ( int ) Math.round( x1s );
		final int y1 = ( int ) Math.round( y1s );

		if ( x0 == x1 && y0 == y1 ) { return; }

		g2d.drawLine( x0, y0, x1, y1 );

	}

	public void setFrame( final int frame )
	{
		this.frame = frame;
	}

	public void setHighlight( final Collection< DefaultWeightedEdge > edges )
	{
		this.highlight = edges;
	}

}
