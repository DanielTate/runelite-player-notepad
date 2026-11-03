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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(PlayerNotepadConfig.GROUP)
public interface PlayerNotepadConfig extends Config
{
	String GROUP = "playernotepad";
	String NOTES_KEY = "notes";
	String PLAYER_OPTION_KEY = "playerOption";

	@ConfigItem(
		keyName = PLAYER_OPTION_KEY,
		name = "Player menu option",
		description = "Add 'Add to notepad' to the right-click menu on players in the game world",
		position = 1
	)
	default boolean playerOption()
	{
		return true;
	}

	@ConfigItem(
		keyName = "listOption",
		name = "Chat and list menu option",
		description = "Add 'Add to notepad' when right-clicking names in chat, private messages, "
			+ "and the friends, ignore, clan and chat-channel lists",
		position = 2
	)
	default boolean listOption()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skipDuplicates",
		name = "Skip duplicates",
		description = "Don't add a name that is already on its own line in the notepad",
		position = 3
	)
	default boolean skipDuplicates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "selectKey",
		name = "Area select hold key",
		description = "Hold this key and drag on the game to add every player in the box. "
			+ "The Select area button in the sidebar works without setting one",
		position = 4
	)
	default Keybind selectKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = NOTES_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String notes()
	{
		return "";
	}
}
