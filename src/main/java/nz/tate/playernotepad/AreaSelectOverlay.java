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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the selection box while dragging, outlines the players it will pick up, and shows a
 * short hint next to the cursor while waiting for the drag to start.
 */
class AreaSelectOverlay extends Overlay
{
	private static final Color BOX_FILL = new Color(0, 200, 255, 40);
	private static final Color BOX_LINE = new Color(0, 200, 255, 220);
	private static final Color PLAYER_LINE = new Color(255, 255, 0, 220);
	private static final String HINT_HELD = "Drag to select players";
	private static final String HINT_ARMED = HINT_HELD + " (Esc to cancel)";

	private final Client client;
	private final PlayerNotepadPlugin plugin;

	@Inject
	AreaSelectOverlay(Client client, PlayerNotepadPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		AreaSelector selector = plugin.getSelector();
		if (selector == null)
		{
			return null;
		}

		Rectangle r = selector.currentRect();
		if (r != null)
		{
			g.setStroke(new BasicStroke(1.5f));
			for (Player p : plugin.playersInside(r))
			{
				Shape hull = p.getConvexHull();
				if (hull != null)
				{
					g.setColor(PLAYER_LINE);
					g.draw(hull);
				}
			}

			g.setColor(BOX_FILL);
			g.fill(r);
			g.setColor(BOX_LINE);
			g.draw(r);
		}
		else if (selector.isWaiting())
		{
			Point mouse = client.getMouseCanvasPosition();
			if (mouse != null && mouse.getX() >= 0)
			{
				g.setFont(FontManager.getRunescapeSmallFont());
				OverlayUtil.renderTextLocation(g, new Point(mouse.getX() + 14, mouse.getY() + 24),
					selector.isArmed() ? HINT_ARMED : HINT_HELD, BOX_LINE);
			}
		}
		return null;
	}
}
