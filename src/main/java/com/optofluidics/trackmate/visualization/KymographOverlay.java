package com.optofluidics.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

public class KymographOverlay extends Roi
{
	private static final Color FRAME_BOX_COLOR = Color.YELLOW;

	private final Composite composite = AlphaComposite.getInstance( AlphaComposite.SRC_OVER );

	private final Model model;

	private final ImagePlus imp;

	private final Map< String, Object > displaySettings;

	private int frame;

	public KymographOverlay( final Model model, final ImagePlus imp, final Map< String, Object > displaySettings )
	{
		super( 0, 0, imp );
		this.model = model;
		this.imp = imp;
		this.displaySettings = displaySettings;
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

		final int xp1 = 0;
		final int xp2 = imp.getWidth();
		final int yp1 = frame;
		final int yp2 = frame + 1;

		final int xs1 = ( int ) ( ( xp1 - xcorner ) * magnification );
		final int ys1 = ( int ) ( ( yp1 - ycorner ) * magnification );
		final int xs2 = ( int ) ( ( xp2 - xcorner ) * magnification ) - 1;
		final int ys2 = ( int ) ( ( yp2 - ycorner ) * magnification ) - 1;

		g2d.setColor( FRAME_BOX_COLOR );
		g2d.drawRect( xs1, ys1, xs2 - xs1, ys2 - ys1 );
	}

	public void setFrame( final int frame )
	{
		this.frame = frame;
	}

}
