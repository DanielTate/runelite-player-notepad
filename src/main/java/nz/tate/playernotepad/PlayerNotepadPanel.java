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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/**
 * Sidebar panel with two views: the open notepad, and a searchable list of every notepad.
 * Pages save themselves shortly after each edit. Every method here must be called on the Swing
 * event thread.
 */
class PlayerNotepadPanel extends PluginPanel
{
	private static final int SAVE_DELAY_MS = 500;
	private static final String NOTEPAD_VIEW = "notepad";
	private static final String LIST_VIEW = "list";
	private static final Color ACCENT = ColorScheme.BRAND_ORANGE;
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color FAINT = ColorScheme.MEDIUM_GRAY_COLOR;
	private static final int GAP = 8;
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

	private final NotepadBook book;
	private final Consumer<NotepadBook.Saved> saver;
	private final LongSupplier clock;
	private final CardLayout views = new CardLayout();
	private final JPanel viewPanel = new JPanel(views);

	// The open notepad.
	private final JLabel title = new JLabel();
	private final JLabel meta = new JLabel();
	private final JTextArea textArea = new JTextArea();
	private final JButton copyNames = new JButton("Copy names");
	private final JCheckBox clanOnly = new JCheckBox("Clan members only");
	private final JCheckBox autoCopy = new JCheckBox("Auto-copy names");
	private final Timer saveTimer;
	private final Timer copiedTimer;
	/** Set while the text area is filled from a page, so that isn't taken for an edit. */
	private boolean loading;

	// The list of notepads.
	private final IconTextField search = new IconTextField();
	private final JLabel listCount = new JLabel();
	private final JPanel rows = new JPanel();

	PlayerNotepadPanel(NotepadBook book, Consumer<NotepadBook.Saved> saver, LongSupplier clock, Runnable onSelectArea,
		boolean clanOnlyOn, Consumer<Boolean> onClanOnly, boolean autoCopyOn, Consumer<Boolean> onAutoCopy)
	{
		// No wrapping scroll pane: the text area and list bring their own, and fill the sidebar's height.
		super(false);
		this.book = book;
		this.saver = saver;
		this.clock = clock;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		viewPanel.setOpaque(false);
		viewPanel.add(notepadView(onSelectArea, clanOnlyOn, onClanOnly, autoCopyOn, onAutoCopy), NOTEPAD_VIEW);
		viewPanel.add(listView(), LIST_VIEW);
		add(viewPanel, BorderLayout.CENTER);

		// Save shortly after the user stops typing, rather than on every keystroke.
		saveTimer = new Timer(SAVE_DELAY_MS, e -> save());
		saveTimer.setRepeats(false);
		copiedTimer = new Timer(1500, e -> copyNames.setText("Copy names"));
		copiedTimer.setRepeats(false);
		textArea.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				edited();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				edited();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				edited();
			}
		});

		showPage();
	}

	private JComponent notepadView(Runnable onSelectArea, boolean clanOnlyOn, Consumer<Boolean> onClanOnly,
		boolean autoCopyOn, Consumer<Boolean> onAutoCopy)
	{
		JLabel back = link("‹ All notepads", "See, search and open your other notepads");
		back.addMouseListener(click(this::showList));

		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setMinimumSize(new Dimension(0, 0));
		meta.setFont(FontManager.getRunescapeSmallFont());
		meta.setForeground(MUTED);

		JLabel rename = link("Rename", "Give this notepad a new title");
		rename.addMouseListener(click(this::rename));
		JLabel duplicate = link("Duplicate", "Start a new notepad with a copy of this one");
		duplicate.addMouseListener(click(this::duplicate));
		JLabel delete = link("Delete", "Delete this notepad");
		delete.addMouseListener(click(this::delete));
		JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		actions.setOpaque(false);
		actions.add(rename);
		actions.add(dot());
		actions.add(duplicate);
		actions.add(dot());
		actions.add(delete);

		JPanel header = column(4);
		header.add(back);
		header.add(spacer(2));
		header.add(title);
		header.add(meta);
		header.add(spacer(2));
		header.add(actions);

		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setTabSize(2);
		textArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		textArea.setForeground(Color.WHITE);
		textArea.setCaretColor(Color.WHITE);
		textArea.setBorder(new EmptyBorder(6, 8, 6, 8));
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setBorder(BorderFactory.createLineBorder(FAINT));
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JButton selectArea = button("Select area", "Then drag a box over the game to add every player inside it. "
			+ "Set a key in the plugin's settings to skip this button");
		selectArea.addActionListener(e -> onSelectArea.run());
		copyNames.setToolTipText("Copy just the names, one per line, without your notes");
		copyNames.setFocusPainted(false);
		copyNames.addActionListener(e -> copyNames());
		JButton clear = button("Clear", "Remove everything from this notepad");
		clear.addActionListener(e -> confirmAndClear());
		JPanel pair = new JPanel(new GridLayout(1, 2, 4, 0));
		pair.setOpaque(false);
		pair.add(copyNames);
		pair.add(clear);
		JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 4));
		buttons.setOpaque(false);
		buttons.add(selectArea);
		buttons.add(pair);

		clanOnly.setSelected(clanOnlyOn);
		clanOnly.setOpaque(false);
		clanOnly.setFocusPainted(false);
		clanOnly.setToolTipText("Select area only adds players in your clan");
		clanOnly.addActionListener(e -> onClanOnly.accept(clanOnly.isSelected()));

		autoCopy.setSelected(autoCopyOn);
		autoCopy.setOpaque(false);
		autoCopy.setFocusPainted(false);
		autoCopy.setToolTipText("Copy the names to the clipboard whenever players are added");
		autoCopy.addActionListener(e -> onAutoCopy.accept(autoCopy.isSelected()));

		JPanel footer = column(4);
		footer.add(buttons);
		footer.add(clanOnly);
		footer.add(autoCopy);

		JPanel view = new JPanel(new BorderLayout(0, GAP));
		view.setOpaque(false);
		view.add(header, BorderLayout.NORTH);
		view.add(scroll, BorderLayout.CENTER);
		view.add(footer, BorderLayout.SOUTH);
		return view;
	}

	private JComponent listView()
	{
		JLabel heading = new JLabel("Notepads");
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(Color.WHITE);
		listCount.setFont(FontManager.getRunescapeSmallFont());
		listCount.setForeground(MUTED);
		JPanel headingRow = new JPanel(new BorderLayout());
		headingRow.setOpaque(false);
		headingRow.add(heading, BorderLayout.WEST);
		headingRow.add(listCount, BorderLayout.EAST);

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(0, 30));
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		search.setToolTipText("Search titles and names");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				fillList();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				fillList();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				fillList();
			}
		});
		search.addClearListener(this::fillList);

		JButton create = button("New notepad", "Start a new, empty notepad");
		create.addActionListener(e -> create());
		create.setMaximumSize(new Dimension(Integer.MAX_VALUE, create.getPreferredSize().height));
		search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		JPanel header = column(GAP);
		header.add(headingRow);
		header.add(search);
		header.add(create);

		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		rows.setOpaque(false);
		JPanel rowsTop = new JPanel(new BorderLayout());
		rowsTop.setOpaque(false);
		rowsTop.add(rows, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(rowsTop);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		JPanel view = new JPanel(new BorderLayout(0, GAP));
		view.setOpaque(false);
		view.add(header, BorderLayout.NORTH);
		view.add(scroll, BorderLayout.CENTER);
		return view;
	}

	/** Fills the list with the notepads matching the search, the open one marked. */
	private void fillList()
	{
		List<Notepad> pages = book.search(search.getText());
		rows.removeAll();
		listCount.setText(book.size() == 1 ? "1 notepad" : book.size() + " notepads");
		if (pages.isEmpty())
		{
			JLabel none = new JLabel("No notepads match.");
			none.setForeground(MUTED);
			none.setBorder(new EmptyBorder(6, 2, 0, 0));
			rows.add(none);
		}
		long now = clock.getAsLong();
		for (Notepad page : pages)
		{
			rows.add(row(page, page == book.open(), now));
			rows.add(spacer(4));
		}
		rows.revalidate();
		rows.repaint();
	}

	/** One notepad in the list: its title, then how many names and when it was last edited. */
	private JComponent row(Notepad page, boolean open, long now)
	{
		JPanel row = new JPanel(new BorderLayout(0, 2))
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (open)
				{
					g.setColor(ACCENT);
					g.fillRect(0, 0, 3, getHeight());
				}
			}
		};
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(7, 10, 7, 8));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(open ? "Open now" : "Open this notepad");

		JLabel name = new JLabel(page.title);
		name.setForeground(Color.WHITE);
		name.setMinimumSize(new Dimension(0, 0));
		JLabel detail = new JLabel(names(NotepadText.names(page.text).size()) + " · " + ago(page.updated, now));
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(MUTED);
		row.add(name, BorderLayout.NORTH);
		row.add(detail, BorderLayout.SOUTH);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				openPage(page.id);
			}
		});
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	void addName(String name, boolean skipDuplicates)
	{
		addNames(List.of(name), skipDuplicates);
	}

	/** Appends each name to the open notepad in order and returns how many were actually added. */
	int addNames(List<String> names, boolean skipDuplicates)
	{
		String original = textArea.getText();
		String updated = original;
		int added = 0;
		for (String name : names)
		{
			String next = NotepadText.append(updated, name, skipDuplicates);
			if (!next.equals(updated))
			{
				added++;
				updated = next;
			}
		}

		if (added > 0)
		{
			textArea.setText(updated);
			textArea.setCaretPosition(updated.length());
		}

		// Even when every name was already listed, so the clipboard matches the notepad you just used.
		if (autoCopy.isSelected())
		{
			copyNames();
		}
		return added;
	}

	/** The open notepad's title, for chat messages. */
	String openTitle()
	{
		return book.open().title;
	}

	/** Keeps the checkbox in step when the setting changes from the config panel. */
	void setClanOnly(boolean on)
	{
		clanOnly.setSelected(on);
	}

	/** Keeps the checkbox in step when the setting changes from the config panel. */
	void setAutoCopy(boolean on)
	{
		autoCopy.setSelected(on);
	}

	/** Writes any pending edit straight away. Called when the plugin shuts down. */
	void flush()
	{
		if (saveTimer.isRunning())
		{
			saveTimer.stop();
			save();
		}
	}

	private void edited()
	{
		if (!loading)
		{
			updateMeta();
			saveTimer.restart();
		}
	}

	private void save()
	{
		book.setText(textArea.getText(), clock.getAsLong());
		saver.accept(book.save());
		updateMeta();
	}

	private void persist()
	{
		saver.accept(book.save());
	}

	/** Fills the notepad view from the open page and shows it. */
	private void showPage()
	{
		Notepad page = book.open();
		loading = true;
		textArea.setText(page.text);
		textArea.setCaretPosition(page.text.length());
		loading = false;
		title.setText(page.title);
		title.setToolTipText(page.title);
		updateMeta();
		views.show(viewPanel, NOTEPAD_VIEW);
	}

	private void updateMeta()
	{
		Notepad page = book.open();
		int count = NotepadText.names(textArea.getText()).size();
		String edited = saveTimer != null && saveTimer.isRunning() ? "editing" : "edited " + ago(page.updated, clock.getAsLong());
		meta.setText(names(count) + " · " + edited);
	}

	private void showList()
	{
		flush();
		search.setText("");
		fillList();
		views.show(viewPanel, LIST_VIEW);
		search.requestFocusInWindow();
	}

	private void openPage(String id)
	{
		flush();
		book.open(id);
		persist();
		showPage();
	}

	private void create()
	{
		String name = askTitle("New notepad", book.unusedTitle(NotepadBook.FIRST_TITLE));
		if (name != null)
		{
			flush();
			book.create(name, clock.getAsLong());
			persist();
			showPage();
			textArea.requestFocusInWindow();
		}
	}

	private void duplicate()
	{
		String name = askTitle("Duplicate notepad", book.unusedTitle(book.open().title + " copy"));
		if (name != null)
		{
			flush();
			book.duplicate(name, clock.getAsLong());
			persist();
			showPage();
		}
	}

	private void rename()
	{
		String name = askTitle("Rename notepad", book.open().title);
		if (name != null)
		{
			book.rename(name, clock.getAsLong());
			persist();
			showPage();
		}
	}

	private void delete()
	{
		int answer = JOptionPane.showConfirmDialog(this,
			"Delete \"" + book.open().title + "\"? This can't be undone.",
			"Player Notepad",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (answer == JOptionPane.YES_OPTION)
		{
			saveTimer.stop();
			book.deleteOpen(clock.getAsLong());
			persist();
			showPage();
		}
	}

	/** Asks for a title, offering {@code suggested}; null if cancelled or left empty. */
	private String askTitle(String heading, String suggested)
	{
		Object answer = JOptionPane.showInputDialog(this, "Title:", heading, JOptionPane.PLAIN_MESSAGE,
			null, null, suggested);
		String clean = NotepadBook.cleanTitle(answer == null ? null : answer.toString());
		return clean.isEmpty() ? null : clean;
	}

	private void copyNames()
	{
		List<String> names = NotepadText.names(textArea.getText());
		if (names.isEmpty())
		{
			copyNames.setText("No names");
		}
		else
		{
			try
			{
				Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(String.join("\n", names)), null);
				copyNames.setText("Copied " + names.size());
			}
			catch (IllegalStateException e)
			{
				// Another program is holding the clipboard.
				copyNames.setText("Copy failed");
			}
		}
		copiedTimer.restart();
	}

	private void confirmAndClear()
	{
		if (textArea.getText().isEmpty())
		{
			return;
		}

		int answer = JOptionPane.showConfirmDialog(this,
			"Clear everything in \"" + book.open().title + "\"?",
			"Player Notepad",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (answer == JOptionPane.YES_OPTION)
		{
			textArea.setText("");
			saveTimer.stop();
			save();
		}
	}

	static String names(int count)
	{
		return count == 1 ? "1 name" : count + " names";
	}

	/** "just now", "5m ago", "3h ago", "2d ago", then the date. */
	static String ago(long then, long now)
	{
		long minutes = Math.max(0, now - then) / 60_000;
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + "m ago";
		}
		if (minutes < 24 * 60)
		{
			return minutes / 60 + "h ago";
		}
		if (minutes < 7 * 24 * 60)
		{
			return minutes / (24 * 60) + "d ago";
		}
		return Instant.ofEpochMilli(then).atZone(ZoneId.systemDefault()).format(DAY);
	}

	private static JLabel link(String text, String tooltip)
	{
		JLabel link = new JLabel(text);
		link.setFont(FontManager.getRunescapeSmallFont());
		link.setForeground(ACCENT);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.setToolTipText(tooltip);
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				link.setForeground(ACCENT.brighter());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				link.setForeground(ACCENT);
			}
		});
		return link;
	}

	/** A faint dot between the page actions. */
	private static JLabel dot()
	{
		JLabel dot = new JLabel(" \u00B7 ");
		dot.setFont(FontManager.getRunescapeSmallFont());
		dot.setForeground(FAINT);
		return dot;
	}

	private static MouseAdapter click(Runnable action)
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}
		};
	}

	private static JButton button(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.setToolTipText(tooltip);
		return button;
	}

	/** A top-to-bottom stack, each child as wide as the stack. */
	private static JPanel column(int gap)
	{
		JPanel column = new JPanel()
		{
			@Override
			public Component add(Component c)
			{
				if (gap > 0 && getComponentCount() > 0 && !(c instanceof Spacer))
				{
					super.add(spacer(gap));
				}
				((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
				return super.add(c);
			}
		};
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		return column;
	}

	private static Spacer spacer(int size)
	{
		return new Spacer(size);
	}

	private static final class Spacer extends JComponent
	{
		Spacer(int size)
		{
			setPreferredSize(new Dimension(0, size));
			setMinimumSize(new Dimension(0, size));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, size));
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}
	}
}
