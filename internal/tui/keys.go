package tui

import "charm.land/bubbles/v2/key"

// keyMap is the capture screen's bindings. Enter is deliberately absent: this
// is a multi-line editor, and saving on enter would fight the thing it is for.
type keyMap struct {
	Save    key.Binding
	Discard key.Binding
}

var keys = keyMap{
	Save: key.NewBinding(
		// ctrl+d as well, for the EOF reflex.
		key.WithKeys("ctrl+s", "ctrl+d"),
		key.WithHelp("ctrl+s", "save"),
	),
	Discard: key.NewBinding(
		key.WithKeys("ctrl+c", "esc"),
		key.WithHelp("ctrl+c", "discard"),
	),
}

func (k keyMap) ShortHelp() []key.Binding {
	return []key.Binding{k.Save, k.Discard}
}

func (k keyMap) FullHelp() [][]key.Binding {
	return [][]key.Binding{k.ShortHelp()}
}
