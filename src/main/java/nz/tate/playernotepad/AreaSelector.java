/*
 * Copyright (c) 2026, dan <dan@tate.nz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nz.tate.playernotepad;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;

/**
 * Turns a left-button drag on the game canvas into a selection rectangle.
 *
 * <p>Selection is active when it has been armed for one drag (the sidebar button) or while the
 * hold key is down. While active, the press, drag, release and click are consumed so the game
 * never sees them: you don't walk, attack or rotate anything by drawing the box. Escape cancels.
 *
 * <p>Mouse and key events arrive on the AWT thread and the overlay reads the state on the client
 * thread, so the shared fields are volatile.
 */
class AreaSelector extends MouseAdapter implements KeyListener
{
	/** Drags smaller than this in both directions are treated as a cancelled click. */
	static final int MIN_SIZE = 4;

	private final Consumer<Rectangle> onSelect;

	private volatile boolean armed;
	private volatile boolean held;
	private volatile Point start;
	private volatile Point current;
	private volatile boolean swallowClick;

	AreaSelector(Consumer<Rectangle> onSelect)
	{
		this.onSelect = onSelect;
	}

	/** Arms selection for the next drag. */
	void arm()
	{
		armed = true;
	}

	void setHeld(boolean held)
	{
		this.held = held;
	}

	void cancel()
	{
		armed = false;
		start = null;
		current = null;
	}

	/** True when waiting for a drag to begin, so the overlay can show a hint. */
	boolean isWaiting()
	{
		return (armed || held) && start == null;
	}

	/** The rectangle being dragged, or null when no drag is in progress. */
	Rectangle currentRect()
	{
		Point s = start;
		Point c = current;
		return s == null || c == null ? null : rect(s, c);
	}

	static Rectangle rect(Point a, Point b)
	{
		int x = Math.min(a.x, b.x);
		int y = Math.min(a.y, b.y);
		return new Rectangle(x, y, Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}

	static boolean bigEnough(Rectangle r)
	{
		return r.width >= MIN_SIZE || r.height >= MIN_SIZE;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (!(armed || held) || !SwingUtilities.isLeftMouseButton(e))
		{
			return e;
		}

		start = e.getPoint();
		current = start;
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		if (start == null)
		{
			return e;
		}

		current = e.getPoint();
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		Point s = start;
		if (s == null || !SwingUtilities.isLeftMouseButton(e))
		{
			return e;
		}

		Rectangle r = rect(s, e.getPoint());
		armed = false;
		start = null;
		current = null;
		swallowClick = true;
		e.consume();

		if (bigEnough(r))
		{
			onSelect.accept(r);
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (swallowClick)
		{
			swallowClick = false;
			e.consume();
		}
		return e;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (armed || start != null))
		{
			cancel();
			e.consume();
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}
}
