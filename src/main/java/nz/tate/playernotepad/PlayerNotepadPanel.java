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
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar panel: a free-text notepad the player can edit by hand, plus a Clear button.
 * Every method here must be called on the Swing event thread.
 */
class PlayerNotepadPanel extends PluginPanel
{
	private static final int SAVE_DELAY_MS = 500;

	private final JTextArea textArea = new JTextArea();
	private final Consumer<String> saver;
	private final Timer saveTimer;

	PlayerNotepadPanel(String initialText, Consumer<String> saver, Runnable onSelectArea)
	{
		// No wrapping scroll pane: the text area brings its own, and fills the sidebar's height.
		super(false);
		this.saver = saver;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Player Notepad");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(java.awt.Color.WHITE);
		add(title, BorderLayout.NORTH);

		textArea.setText(initialText == null ? "" : initialText);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setTabSize(2);
		textArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		textArea.setForeground(java.awt.Color.WHITE);
		textArea.setCaretColor(java.awt.Color.WHITE);
		textArea.setBorder(new EmptyBorder(6, 6, 6, 6));

		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		add(scroll, BorderLayout.CENTER);

		JButton selectArea = new JButton("Select area");
		selectArea.setToolTipText("Then drag a box over the game to add every player inside it");
		selectArea.addActionListener(e -> onSelectArea.run());

		JButton clear = new JButton("Clear");
		clear.setToolTipText("Remove everything from the notepad");
		clear.addActionListener(e -> confirmAndClear());

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(selectArea);
		buttons.add(clear);
		add(buttons, BorderLayout.SOUTH);

		// Save shortly after the user stops typing, rather than on every keystroke.
		saveTimer = new Timer(SAVE_DELAY_MS, e -> save());
		saveTimer.setRepeats(false);
		textArea.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				saveTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				saveTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				saveTimer.restart();
			}
		});
	}

	void addName(String name, boolean skipDuplicates)
	{
		addNames(List.of(name), skipDuplicates);
	}

	/** Appends each name in order and returns how many were actually added. */
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
		return added;
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

	private void confirmAndClear()
	{
		if (textArea.getText().isEmpty())
		{
			return;
		}

		int answer = JOptionPane.showConfirmDialog(this,
			"Clear everything in the notepad?",
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

	private void save()
	{
		saver.accept(textArea.getText());
	}
}
