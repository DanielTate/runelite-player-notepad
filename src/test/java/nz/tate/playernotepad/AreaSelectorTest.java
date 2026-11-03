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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.Test;

public class AreaSelectorTest
{
	private final JPanel source = new JPanel();
	private final List<Rectangle> selected = new ArrayList<>();
	private final AreaSelector selector = new AreaSelector(selected::add);

	private MouseEvent mouse(int id, int x, int y)
	{
		return new MouseEvent(source, id, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

	@Test
	public void rectNormalisesAnyDragDirection()
	{
		assertEquals(new Rectangle(10, 20, 30, 40), AreaSelector.rect(new Point(40, 60), new Point(10, 20)));
	}

	@Test
	public void doesNothingUnlessArmed()
	{
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 10, 10);
		selector.mousePressed(press);
		assertFalse(press.isConsumed());
		assertNull(selector.currentRect());
	}

	@Test
	public void armedDragSelectsOnceAndConsumesEvents()
	{
		selector.arm();
		assertTrue(selector.isWaiting());

		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 100, 100);
		selector.mousePressed(press);
		MouseEvent drag = mouse(MouseEvent.MOUSE_DRAGGED, 150, 130);
		selector.mouseDragged(drag);
		assertEquals(new Rectangle(100, 100, 50, 30), selector.currentRect());

		MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, 160, 140);
		selector.mouseReleased(release);
		MouseEvent click = mouse(MouseEvent.MOUSE_CLICKED, 160, 140);
		selector.mouseClicked(click);

		assertTrue(press.isConsumed() && drag.isConsumed() && release.isConsumed() && click.isConsumed());
		assertEquals(List.of(new Rectangle(100, 100, 60, 40)), selected);

		// Disarmed after one use.
		MouseEvent next = mouse(MouseEvent.MOUSE_PRESSED, 5, 5);
		selector.mousePressed(next);
		assertFalse(next.isConsumed());
	}

	@Test
	public void tinyDragIsIgnored()
	{
		selector.arm();
		selector.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100));
		selector.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 102, 101));
		assertTrue(selected.isEmpty());
	}

	@Test
	public void holdKeyEnablesSelection()
	{
		selector.setHeld(true);
		selector.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 0, 0));
		selector.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 50, 50));
		assertEquals(1, selected.size());
	}

	@Test
	public void escapeCancels()
	{
		selector.arm();
		selector.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 0, 0));
		KeyEvent esc = new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
		selector.keyPressed(esc);
		assertTrue(esc.isConsumed());
		assertNull(selector.currentRect());

		selector.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 50, 50));
		assertTrue(selected.isEmpty());
	}
}
