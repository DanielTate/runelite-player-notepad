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

import java.util.Locale;

/**
 * Pure text helpers for the notepad, kept free of Swing and the client so they can be tested.
 */
final class NotepadText
{
	private NotepadText()
	{
	}

	/**
	 * Returns {@code text} with {@code name} added on a new line at the end.
	 *
	 * @param skipDuplicates when true and a line already holds this name, the text is returned
	 *                       unchanged
	 */
	static String append(String text, String name, boolean skipDuplicates)
	{
		String current = text == null ? "" : text;
		String clean = cleanName(name);
		if (clean.isEmpty())
		{
			return current;
		}

		if (skipDuplicates && contains(current, clean))
		{
			return current;
		}

		if (current.isEmpty() || current.endsWith("\n"))
		{
			return current + clean + "\n";
		}
		return current + "\n" + clean + "\n";
	}

	/**
	 * True when any line of {@code text} is the same player name, ignoring case and treating
	 * spaces, underscores, hyphens and non-breaking spaces as equal, as the game does.
	 */
	static boolean contains(String text, String name)
	{
		if (text == null || text.isEmpty())
		{
			return false;
		}

		String key = normalise(name);
		for (String line : text.split("\n", -1))
		{
			if (normalise(line).equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Strips formatting tags such as {@code <col=ffffff>} and {@code <img=2>} and the combat
	 * level suffix a menu target carries, and turns non-breaking spaces into plain ones.
	 */
	static String cleanName(String name)
	{
		if (name == null)
		{
			return "";
		}

		String clean = name.replaceAll("<[^>]*>", "")
			.replace(' ', ' ')
			.replaceAll("\\s*\\(level-\\d+\\)$", "")
			.trim();
		return clean;
	}

	/** A valid RuneScape name: 1 to 12 letters, digits, spaces, hyphens or underscores. */
	private static final java.util.regex.Pattern NAME = java.util.regex.Pattern.compile("[A-Za-z0-9 _-]{1,12}");

	/**
	 * The player names in {@code text}, in order, without repeats: the start of each line, up
	 * to any note written after it (after " - ", ":" or "("), when that is a valid name.
	 */
	static java.util.List<String> names(String text)
	{
		java.util.List<String> names = new java.util.ArrayList<>();
		if (text == null)
		{
			return names;
		}
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (String line : text.split("\n"))
		{
			String name = cleanName(line).split(" - |:|\\(", 2)[0].trim();
			if (NAME.matcher(name).matches() && seen.add(normalise(name)))
			{
				names.add(name);
			}
		}
		return names;
	}

	private static String normalise(String name)
	{
		return cleanName(name)
			.replace('_', ' ')
			.replace('-', ' ')
			.replaceAll("\\s+", " ")
			.toLowerCase(Locale.ROOT);
	}
}
