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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Every saved notepad page and which one is open. Pure logic, free of Swing and the client, so
 * it can be tested. There is always at least one page, and always one open.
 */
final class NotepadBook
{
	static final String FIRST_TITLE = "Notepad";

	/** The save format: the pages, and the id of the open one. */
	static final class Saved
	{
		List<Notepad> pages = new ArrayList<>();
		String open;
	}

	private final List<Notepad> pages;
	private String openId;

	private NotepadBook(List<Notepad> pages, String openId)
	{
		this.pages = pages;
		this.openId = openId;
	}

	/**
	 * Restores a book from its save, or starts one from the single notepad of version 1.0
	 * ({@code legacyText}) when there's no save yet.
	 */
	static NotepadBook load(Saved saved, String legacyText, long now)
	{
		List<Notepad> pages = new ArrayList<>();
		if (saved != null && saved.pages != null)
		{
			for (Notepad page : saved.pages)
			{
				if (page != null && page.id != null)
				{
					page.title = page.title == null || page.title.isBlank() ? FIRST_TITLE : page.title;
					page.text = page.text == null ? "" : page.text;
					pages.add(page);
				}
			}
		}
		if (pages.isEmpty())
		{
			pages.add(new Notepad(newId(), FIRST_TITLE, legacyText == null ? "" : legacyText, now));
		}
		NotepadBook book = new NotepadBook(pages, saved == null ? null : saved.open);
		if (book.find(book.openId) == null)
		{
			book.openId = book.newest().id;
		}
		return book;
	}

	Saved save()
	{
		Saved saved = new Saved();
		saved.pages = new ArrayList<>(pages);
		saved.open = openId;
		return saved;
	}

	Notepad open()
	{
		return find(openId);
	}

	void open(String id)
	{
		if (find(id) != null)
		{
			openId = id;
		}
	}

	/** Every page, most recently edited first. */
	List<Notepad> pages()
	{
		List<Notepad> sorted = new ArrayList<>(pages);
		sorted.sort(Comparator.comparingLong((Notepad p) -> p.updated).reversed());
		return sorted;
	}

	/**
	 * Pages whose title or text holds {@code query}, most recently edited first. Case doesn't
	 * matter, and spaces, underscores and hyphens count as the same, as they do in names.
	 */
	List<Notepad> search(String query)
	{
		String key = squash(query);
		List<Notepad> found = new ArrayList<>();
		for (Notepad page : pages())
		{
			if (key.isEmpty() || squash(page.title).contains(key) || squash(page.text).contains(key))
			{
				found.add(page);
			}
		}
		return found;
	}

	/** Adds a blank page, opens it, and returns it. */
	Notepad create(String title, long now)
	{
		return add(title, "", now);
	}

	/** Adds a copy of the open page's text under a new title, opens it, and returns it. */
	Notepad duplicate(String title, long now)
	{
		return add(title, open().text, now);
	}

	void rename(String title, long now)
	{
		String clean = cleanTitle(title);
		if (!clean.isEmpty())
		{
			Notepad page = open();
			page.title = clean;
			page.updated = now;
		}
	}

	/** Sets the open page's text, returning true if it changed. */
	boolean setText(String text, long now)
	{
		Notepad page = open();
		String next = text == null ? "" : text;
		if (next.equals(page.text))
		{
			return false;
		}
		page.text = next;
		page.updated = now;
		return true;
	}

	/** Deletes the open page and opens the most recent one left, starting a blank page if none are. */
	void deleteOpen(long now)
	{
		pages.remove(open());
		if (pages.isEmpty())
		{
			pages.add(new Notepad(newId(), FIRST_TITLE, "", now));
		}
		openId = newest().id;
	}

	int size()
	{
		return pages.size();
	}

	/** A title nobody has used yet: "Notepad 2", "Notepad 3" and so on. */
	String unusedTitle(String base)
	{
		if (!hasTitle(base))
		{
			return base;
		}
		for (int n = 2; ; n++)
		{
			String title = base + " " + n;
			if (!hasTitle(title))
			{
				return title;
			}
		}
	}

	/** Titles are one line, trimmed, and at most {@link #MAX_TITLE} characters. */
	static String cleanTitle(String title)
	{
		if (title == null)
		{
			return "";
		}
		String clean = title.replaceAll("\\s+", " ").trim();
		return clean.length() > MAX_TITLE ? clean.substring(0, MAX_TITLE).trim() : clean;
	}

	static final int MAX_TITLE = 40;

	private Notepad add(String title, String text, long now)
	{
		String clean = cleanTitle(title);
		Notepad page = new Notepad(newId(), clean.isEmpty() ? unusedTitle(FIRST_TITLE) : clean, text, now);
		pages.add(page);
		openId = page.id;
		return page;
	}

	private boolean hasTitle(String title)
	{
		for (Notepad page : pages)
		{
			if (page.title.equalsIgnoreCase(title))
			{
				return true;
			}
		}
		return false;
	}

	private Notepad find(String id)
	{
		for (Notepad page : pages)
		{
			if (page.id.equals(id))
			{
				return page;
			}
		}
		return null;
	}

	private Notepad newest()
	{
		return pages().get(0);
	}

	private static String newId()
	{
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private static String squash(String text)
	{
		return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ')
			.replace(' ', ' ').replaceAll("\\s+", " ").trim();
	}
}
