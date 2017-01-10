package edu.mit.simile.longwell.renderer;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.servlet.http.HttpServletRequest;

import edu.mit.simile.longwell.RenderingException;

/**
 * Interface for an object which can render a design into a 2D image,
 * at a requested size, using the parameters from an HTTP request.
 */
public interface Renderer {

    public void render(
		Graphics2D 			g2d,
		Rectangle2D			crop,
		HttpServletRequest	request
	) throws RenderingException;
    
}
