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
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NotepadTextTest
{
	@Test
	public void appendsToEmptyNotepad()
	{
		assertEquals("Zezima\n", NotepadText.append("", "Zezima", true));
		assertEquals("Zezima\n", NotepadText.append(null, "Zezima", true));
	}

	@Test
	public void appendsOnNewLineWhenTextHasNoTrailingNewline()
	{
		assertEquals("my notes\nZezima\n", NotepadText.append("my notes", "Zezima", true));
	}

	@Test
	public void appendsDirectlyWhenTextEndsWithNewline()
	{
		assertEquals("A\nB\n", NotepadText.append("A\n", "B", true));
	}

	@Test
	public void skipsDuplicatesIgnoringCaseAndSeparators()
	{
		String text = "iron man_1\n";
		assertEquals(text, NotepadText.append(text, "Iron Man-1", true));
	}

	@Test
	public void addsDuplicatesWhenAllowed()
	{
		assertEquals("Zezima\nZezima\n", NotepadText.append("Zezima\n", "Zezima", false));
	}

	@Test
	public void lineWithExtraTextIsNotADuplicate()
	{
		assertFalse(NotepadText.contains("Zezima - scammer\n", "Zezima"));
		assertTrue(NotepadText.contains("x\n  Zezima  \ny", "zezima"));
	}

	@Test
	public void cleansMenuTargets()
	{
		assertEquals("Zezima", NotepadText.cleanName("<col=ffffff>Zezima<col=ff00>  (level-126)"));
		assertEquals("Iron Man", NotepadText.cleanName("<img=2>Iron Man"));
	}

	@Test
	public void ignoresBlankNames()
	{
		assertEquals("A\n", NotepadText.append("A\n", "<col=ffffff>", true));
	}
}
