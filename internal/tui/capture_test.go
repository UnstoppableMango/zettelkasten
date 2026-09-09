package tui_test

import (
	"strings"
	"testing"

	"github.com/UnstoppableMango/zettelkasten/internal/tui"
	tea "github.com/charmbracelet/bubbletea"
)

// typed drives a model the way the runtime would: size it, type into it, then
// press one key. It returns the final model and the command that key produced.
func typed(t *testing.T, body string, final tea.KeyMsg) (tui.Model, tea.Cmd) {
	t.Helper()

	var m tea.Model = tui.New("202609081412", "fleeting", "/notes/202609081412.md")

	m, _ = m.Update(tea.WindowSizeMsg{Width: 80, Height: 24})

	for _, r := range body {
		msg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{r}}
		if r == '\n' {
			msg = tea.KeyMsg{Type: tea.KeyEnter}
		}

		m, _ = m.Update(msg)
	}

	m, cmd := m.Update(final)

	return m.(tui.Model), cmd
}

func TestSaveKeys(t *testing.T) {
	for _, key := range []tea.KeyMsg{
		{Type: tea.KeyCtrlS},
		{Type: tea.KeyCtrlD},
	} {
		t.Run(key.String(), func(t *testing.T) {
			m, cmd := typed(t, "a thought", key)

			got := m.Result()
			if !got.Saved {
				t.Error("Saved = false, want true")
			}

			if want := "a thought"; got.Body != want {
				t.Errorf("Body = %q, want %q", got.Body, want)
			}

			assertQuits(t, cmd)
		})
	}
}

func TestDiscardKeys(t *testing.T) {
	for _, key := range []tea.KeyMsg{
		{Type: tea.KeyCtrlC},
		{Type: tea.KeyEsc},
	} {
		t.Run(key.String(), func(t *testing.T) {
			m, cmd := typed(t, "a thought", key)

			if m.Result().Saved {
				t.Error("Saved = true, want false")
			}

			assertQuits(t, cmd)
		})
	}
}

// Enter must insert a newline rather than saving: this is a multi-line editor.
func TestEnterIsANewline(t *testing.T) {
	m, _ := typed(t, "first\nsecond", tea.KeyMsg{Type: tea.KeyCtrlS})

	got := m.Result()
	if want := "first\nsecond"; got.Body != want {
		t.Errorf("Body = %q, want %q", got.Body, want)
	}

	if !got.Saved {
		t.Error("Saved = false, want true")
	}
}

// Nothing typed is not an error here; the caller decides what an empty capture
// means.
func TestEmptyCapture(t *testing.T) {
	m, _ := typed(t, "", tea.KeyMsg{Type: tea.KeyCtrlS})

	if got := m.Result(); got.Body != "" {
		t.Errorf("Body = %q, want empty", got.Body)
	}
}

func TestViewShowsHeader(t *testing.T) {
	var m tea.Model = tui.New("202609081412", "fleeting", "/notes/202609081412.md")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 80, Height: 24})

	view := m.(tui.Model).View()
	for _, want := range []string{"202609081412", "fleeting", "/notes/202609081412.md"} {
		if !strings.Contains(view, want) {
			t.Errorf("View() is missing %q:\n%s", want, view)
		}
	}
}

func assertQuits(t *testing.T, cmd tea.Cmd) {
	t.Helper()

	if cmd == nil {
		t.Fatal("got a nil command, want tea.Quit")
	}

	if _, ok := cmd().(tea.QuitMsg); !ok {
		t.Errorf("command produced %T, want tea.QuitMsg", cmd())
	}
}
