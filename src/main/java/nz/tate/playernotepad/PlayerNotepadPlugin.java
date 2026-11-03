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

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Player Notepad",
	description = "Keep a sidebar notepad of player names, added from the right-click menu or by dragging a box over players",
	tags = {"notes", "notepad", "players", "names", "list"}
)
public class PlayerNotepadPlugin extends Plugin
{
	static final String ADD_OPTION = "Add to notepad";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PlayerNotepadConfig config;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AreaSelectOverlay overlay;

	private PlayerNotepadPanel panel;
	private NavigationButton navButton;

	@lombok.Getter(lombok.AccessLevel.PACKAGE)
	private AreaSelector selector;

	private final HotkeyListener holdKeyListener = new HotkeyListener(() -> config.selectKey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selector != null)
			{
				selector.setHeld(true);
			}
		}

		@Override
		public void hotkeyReleased()
		{
			if (selector != null)
			{
				selector.setHeld(false);
			}
		}
	};

	/**
	 * Player names by menu identifier, captured when the menu opens, so the click still works
	 * if the player walks out of view between opening the menu and clicking.
	 */
	private final Map<Integer, String> playerIndexName = new HashMap<>();

	@Provides
	PlayerNotepadConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerNotepadConfig.class);
	}

	@Override
	protected void startUp()
	{
		selector = new AreaSelector(this::onAreaSelected);
		panel = new PlayerNotepadPanel(config.notes(), this::saveNotes, selector::arm);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Player Notepad")
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		mouseManager.registerMouseListener(selector);
		keyManager.registerKeyListener(selector);
		keyManager.registerKeyListener(holdKeyListener);
		overlayManager.add(overlay);

		if (config.playerOption())
		{
			clientThread.invoke(() -> menuManager.get().addPlayerMenuItem(ADD_OPTION));
		}
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() -> menuManager.get().removePlayerMenuItem(ADD_OPTION));
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(holdKeyListener);
		keyManager.unregisterKeyListener(selector);
		mouseManager.unregisterMouseListener(selector);
		selector = null;
		clientToolbar.removeNavigation(navButton);
		panel.flush();
		panel = null;
		navButton = null;
		playerIndexName.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PlayerNotepadConfig.GROUP.equals(event.getGroup())
			|| !PlayerNotepadConfig.PLAYER_OPTION_KEY.equals(event.getKey()))
		{
			return;
		}

		clientThread.invoke(() ->
		{
			menuManager.get().removePlayerMenuItem(ADD_OPTION);
			if (config.playerOption())
			{
				menuManager.get().addPlayerMenuItem(ADD_OPTION);
			}
		});
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		playerIndexName.clear();
		for (MenuEntry entry : event.getMenuEntries())
		{
			if (entry.getType() != MenuAction.RUNELITE_PLAYER || !ADD_OPTION.equals(entry.getOption()))
			{
				continue;
			}

			Player player = entry.getPlayer();
			if (player != null)
			{
				playerIndexName.put(entry.getIdentifier(), player.getName());
			}
		}
	}

	/** Adds the option to names in chat and the social lists, as the Hiscore plugin does. */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.listOption()
			|| (event.getType() != MenuAction.CC_OP.getId() && event.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId()))
		{
			return;
		}

		final String option = event.getOption();
		final int componentId = event.getActionParam1();
		final int groupId = WidgetUtil.componentToInterface(componentId);

		if (groupId == InterfaceID.FRIENDS && option.equals("Delete")
			|| groupId == InterfaceID.CHATCHANNEL_CURRENT && (option.equals("Add ignore") || option.equals("Remove friend"))
			|| groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message"))
			|| groupId == InterfaceID.IGNORE && option.equals("Delete")
			|| (componentId == InterfaceID.ClansSidepanel.PLAYERLIST || componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST)
				&& (option.equals("Add ignore") || option.equals("Remove friend"))
			|| groupId == InterfaceID.PM_CHAT && (option.equals("Add ignore") || option.equals("Message"))
			|| groupId == InterfaceID.GIM_SIDEPANEL && (option.equals("Add friend") || option.equals("Remove friend") || option.equals("Remove ignore")))
		{
			client.createMenuEntry(-2)
				.setOption(ADD_OPTION)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.setIdentifier(event.getIdentifier())
				.onClick(e -> addName(e.getTarget()));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE_PLAYER || !ADD_OPTION.equals(event.getMenuOption()))
		{
			return;
		}

		Player player = event.getMenuEntry().getPlayer();
		String name = player != null ? player.getName() : playerIndexName.get(event.getId());
		playerIndexName.clear();
		addName(name);
	}

	/**
	 * Other players whose on-screen model touches {@code area}, left to right. Must be called on
	 * the client thread.
	 */
	List<Player> playersInside(Rectangle area)
	{
		Player local = client.getLocalPlayer();
		Map<Player, Double> found = new HashMap<>();
		for (Player p : client.getTopLevelWorldView().players())
		{
			if (p == null || p == local || p.getName() == null)
			{
				continue;
			}

			Shape hull = p.getConvexHull();
			if (hull != null && hull.intersects(area))
			{
				found.put(p, hull.getBounds2D().getCenterX());
			}
		}

		List<Player> ordered = new ArrayList<>(found.keySet());
		ordered.sort(Comparator.comparingDouble(found::get));
		return ordered;
	}

	/** Called on the AWT thread when a drag finishes. */
	private void onAreaSelected(Rectangle area)
	{
		clientThread.invoke(() ->
		{
			List<String> names = playersInside(area).stream()
				.map(Player::getName)
				.collect(Collectors.toList());
			if (names.isEmpty())
			{
				chat("Player Notepad: no players in that area.");
				return;
			}

			final boolean skipDuplicates = config.skipDuplicates();
			SwingUtilities.invokeLater(() ->
			{
				if (panel == null)
				{
					return;
				}

				int added = panel.addNames(names, skipDuplicates);
				int skipped = names.size() - added;
				String msg = "Player Notepad: added " + added + (added == 1 ? " player" : " players")
					+ (skipped > 0 ? " (" + skipped + " already listed)." : ".");
				clientThread.invoke(() -> chat(msg));
			});
		});
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private void addName(String name)
	{
		if (name == null)
		{
			return;
		}

		final boolean skipDuplicates = config.skipDuplicates();
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.addName(name, skipDuplicates);
			}
		});
	}

	private void saveNotes(String text)
	{
		configManager.setConfiguration(PlayerNotepadConfig.GROUP, PlayerNotepadConfig.NOTES_KEY, text);
	}
}
