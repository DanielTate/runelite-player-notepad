# Player Notepad

A RuneLite plugin by **DanielTate** that keeps a notepad of player names in the sidebar.

Right-click a player and choose **Add to notepad**, or drag a box over the game to add every
player inside it. Each notepad is plain text, so you can write your own notes next to names,
and you can keep as many notepads as you like.

## Features

- **Add to notepad** on players in the game world.
- The same option on names in chat, private messages, and the friends, ignore, clan and
  chat-channel lists.
- **Select area**: click the button in the sidebar, then drag a box over the game. Every
  other player inside it is added, left to right, and a chat message says how many. The
  players the box will pick up are outlined while you drag. The drag never reaches the
  game, so you don't walk or click anything, and Esc cancels.
- Optional **hold key**: hold it and drag to select an area without using the button. Ctrl or an
  unused F key works best, as a letter key can no longer be typed in chat.
- Optional **press key**: press it once, then drag, the same as clicking the button.
- **Skip duplicates**: a name already on its own line isn't added again. Case, spaces,
  underscores and hyphens are treated as the same, as they are in game.
- **Clan members only**: tick it under the notepad and Select area only adds players in your
  clan. Right-clicking a player always adds them.
- **Saved notepads**: give each notepad a title. **All notepads** lists them, newest first, with
  a search box that finds titles and names. Open, rename, duplicate or delete them any time.
- **Copy names** copies just the names, one per line, without your notes.
- **Auto-copy names**: tick it under the notepad and the names are copied the same way whenever
  players are added by Select area or the right-click menu.
- **Clear** empties the open notepad, after asking first.
- Notepads save as you type, with your RuneLite settings, and survive restarts.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Player menu option | On | "Add to notepad" on players in the game world |
| Chat and list menu option | On | "Add to notepad" on names in chat and the social lists |
| Skip duplicates | On | Don't add a name that's already listed |
| Area select hold key | Not set | Hold and drag to select players without the sidebar button |
| Area select press key | Not set | Press once, then drag, the same as clicking the sidebar button |
| Clan members only | Off | Select area only adds players in your clan (also a checkbox in the sidebar) |
| Auto-copy names | Off | Copy the names to the clipboard whenever players are added (also a checkbox in the sidebar) |

## Privacy

Names are only stored in your own RuneLite config. Nothing is sent anywhere, and the plugin
makes no network requests.

## Development

```
./gradlew test
../run-dev.sh player-notepad-plugin   # dev client, from ~/Sync/Runelite
```

## Version

1.2
