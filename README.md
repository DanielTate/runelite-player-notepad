# Player Notepad

A RuneLite plugin by **DanielTate** that keeps a notepad of player names in the sidebar.

Right-click a player and choose **Add to notepad**, or drag a box over the game to add every
player inside it. The notepad is plain text, so you can write your own notes next to names.

## Features

- **Add to notepad** on players in the game world.
- The same option on names in chat, private messages, and the friends, ignore, clan and
  chat-channel lists.
- **Select area**: click the button in the sidebar, then drag a box over the game. Every
  other player inside it is added, left to right, and a chat message says how many. The
  players the box will pick up are outlined while you drag. The drag never reaches the
  game, so you don't walk or click anything, and Esc cancels.
- Optional **hold key**: hold it and drag to select an area without using the button.
- **Skip duplicates**: a name already on its own line isn't added again. Case, spaces,
  underscores and hyphens are treated as the same, as they are in game.
- **Clear** empties the notepad, after asking first.
- The notepad is saved with your RuneLite settings and survives restarts.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Player menu option | On | "Add to notepad" on players in the game world |
| Chat and list menu option | On | "Add to notepad" on names in chat and the social lists |
| Skip duplicates | On | Don't add a name that's already listed |
| Area select hold key | Not set | Hold and drag to select players without the sidebar button |

## Privacy

Names are only stored in your own RuneLite config. Nothing is sent anywhere, and the plugin
makes no network requests.

## Development

```
./gradlew test
../run-dev.sh player-notepad-plugin   # dev client, from ~/Sync/Runelite
```

## Version

1.0.0
