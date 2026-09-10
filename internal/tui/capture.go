// Package tui is the capture screen. It owns no files: it collects text and
// reports what the user decided, which is what makes it testable without a
// terminal.
package tui

import (
	"strings"

	"github.com/charmbracelet/bubbles/help"
	"github.com/charmbracelet/bubbles/key"
	"github.com/charmbracelet/bubbles/textarea"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

// chrome is the header and footer lines plus the blank line under the header.
const chrome = 4

var headerStyle = lipgloss.NewStyle().Faint(true)

// Result is what the user decided.
type Result struct {
	Body  string
	Saved bool
}

// Model is the capture screen.
type Model struct {
	textarea textarea.Model
	help     help.Model
	header   string
	width    int
	saved    bool
}

// New builds the capture screen. The header shows where the note will land, so
// the answer to "where did that go" is on screen while typing.
func New(zettelID, noteType, path string) Model {
	ta := textarea.New()
	ta.Placeholder = "what are you thinking?"
	ta.ShowLineNumbers = false
	ta.Focus()

	// The defaults cap at 99 rows and 500 columns, which silently stops a long
	// note from growing.
	ta.CharLimit = 0

	return Model{
		textarea: ta,
		help:     help.New(),
		header:   strings.Join([]string{zettelID, noteType, path}, " · "),
	}
}

func (m Model) Init() tea.Cmd {
	return textarea.Blink
}

func (m Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.help.Width = msg.Width

		m.textarea.SetWidth(msg.Width)
		m.textarea.MaxWidth = msg.Width

		height := max(msg.Height-chrome, 1)
		m.textarea.SetHeight(height)
		m.textarea.MaxHeight = height

		return m, nil

	case tea.KeyMsg:
		switch {
		case key.Matches(msg, keys.Save):
			m.saved = true
			return m, tea.Quit

		case key.Matches(msg, keys.Discard):
			m.saved = false
			return m, tea.Quit
		}
	}

	var cmd tea.Cmd
	m.textarea, cmd = m.textarea.Update(msg)

	return m, cmd
}

func (m Model) View() string {
	return strings.Join([]string{
		headerStyle.Render(m.header),
		"",
		m.textarea.View(),
		m.help.View(keys),
	}, "\n")
}

// Result reports what the user decided.
func (m Model) Result() Result {
	return Result{Body: m.textarea.Value(), Saved: m.saved}
}
