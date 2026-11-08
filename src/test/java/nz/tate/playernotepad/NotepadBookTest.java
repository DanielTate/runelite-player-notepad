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

import com.google.gson.Gson;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NotepadBookTest
{
	@Test
	public void theOldNotepadBecomesTheFirstPage()
	{
		NotepadBook book = NotepadBook.load(null, "Zezima\n", 100);
		assertEquals(1, book.size());
		assertEquals("Notepad", book.open().title);
		assertEquals("Zezima\n", book.open().text);
	}

	@Test
	public void aSaveRoundTripsThroughGson()
	{
		Gson gson = new Gson();
		NotepadBook book = NotepadBook.load(null, "", 1);
		book.create("Nex mass", 2);
		book.setText("Woox\n", 3);
		String json = gson.toJson(book.save());

		NotepadBook again = NotepadBook.load(gson.fromJson(json, NotepadBook.Saved.class), "ignored", 4);
		assertEquals(2, again.size());
		assertEquals("Nex mass", again.open().title);
		assertEquals("Woox\n", again.open().text);
	}

	@Test
	public void newPagesOpenAndDuplicatesCopyTheText()
	{
		NotepadBook book = NotepadBook.load(null, "Zezima\n", 1);
		Notepad blank = book.create("Team", 2);
		assertEquals(blank, book.open());
		assertEquals("", blank.text);
		book.setText("Woox\n", 3);
		Notepad copy = book.duplicate("Team copy", 4);
		assertEquals("Woox\n", copy.text);
		assertNotEquals(blank.id, copy.id);
		assertEquals(3, book.size());
	}

	@Test
	public void pagesListNewestFirstAndSearchTitlesAndNames()
	{
		NotepadBook book = NotepadBook.load(null, "Zezima\n", 1);
		book.create("Nex mass", 5);
		book.setText("Lynx_Titan\n", 6);
		book.create("Raids", 3);

		List<Notepad> all = book.pages();
		assertEquals("Nex mass", all.get(0).title);
		assertEquals(1, book.search("nex").size());
		assertEquals("Nex mass", book.search("lynx titan").get(0).title);
		assertEquals(3, book.search("  ").size());
		assertTrue(book.search("nobody").isEmpty());
	}

	@Test
	public void deletingOpensTheNewestPageAndNeverLeavesNone()
	{
		NotepadBook book = NotepadBook.load(null, "", 1);
		book.create("Second", 2);
		book.deleteOpen(3);
		assertEquals("Notepad", book.open().title);
		book.deleteOpen(4);
		assertEquals(1, book.size());
		assertEquals("", book.open().text);
	}

	@Test
	public void titlesAreTidiedAndUnusedOnesCountUp()
	{
		NotepadBook book = NotepadBook.load(null, "", 1);
		assertEquals("Notepad 2", book.unusedTitle("Notepad"));
		book.create("  Nex \n  mass ", 2);
		assertEquals("Nex mass", book.open().title);
		book.rename("   ", 3);
		assertEquals("Nex mass", book.open().title);
		assertEquals(NotepadBook.MAX_TITLE, NotepadBook.cleanTitle("x".repeat(60)).length());
	}

	@Test
	public void aSaveWithAMissingOpenPageOpensTheNewest()
	{
		NotepadBook.Saved saved = new NotepadBook.Saved();
		saved.pages.add(new Notepad("a", "Old", "", 1));
		saved.pages.add(new Notepad("b", "New", "", 9));
		saved.open = "gone";
		assertEquals("New", NotepadBook.load(saved, "", 10).open().title);
	}
}
