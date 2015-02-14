package com.optofluidics.util;

import java.awt.Color;
import java.util.Arrays;

import fiji.plugin.trackmate.Logger;

public class IndentLogger extends Logger
{

	private final Logger logger;

	private final String indent;

	public IndentLogger( final Logger logger, final int indent )
	{
		this.logger = logger;
		final char[] ca = new char[ indent ];
		Arrays.fill( ca, ' ' );
		this.indent = new String( ca );
	}

	@Override
	public void log( final String message, final Color color )
	{
		logger.log( indent + message, color );
	}

	@Override
	public void error( final String message )
	{
		logger.error( indent + message );
	}

	@Override
	public void setProgress( final double val )
	{
		logger.setProgress( val );
	}

	@Override
	public void setStatus( final String status )
	{
		logger.setStatus( status );
	}

}
