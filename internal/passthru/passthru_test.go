package passthru_test

import (
	"strings"
	"testing"

	"github.com/UnstoppableMango/zettelkasten/internal/passthru"
)

func TestOwns(t *testing.T) {
	tests := map[string]struct {
		args []string
		want bool
	}{
		"no args is capture":     {nil, true},
		"empty slice is capture": {[]string{}, true},
		"capture":                {[]string{"capture"}, true},
		"capture with text":      {[]string{"capture", "a", "thought"}, true},
		"init":                   {[]string{"init"}, true},
		"help":                   {[]string{"help"}, true},
		"completion":             {[]string{"completion", "zsh"}, true},
		"long flag":              {[]string{"--version"}, true},
		"short flag":             {[]string{"-h"}, true},
		"flag with value":        {[]string{"--dir", "/notes"}, true},

		// zk's surface, which slip deliberately does not shadow.
		"zk list":  {[]string{"list"}, false},
		"zk edit":  {[]string{"edit"}, false},
		"zk new":   {[]string{"new", "--title", "x"}, false},
		"zk index": {[]string{"index"}, false},
		"zk graph": {[]string{"graph"}, false},
		"zk lsp":   {[]string{"lsp"}, false},
		"unknown":  {[]string{"nonsense"}, false},
	}

	for name, tt := range tests {
		t.Run(name, func(t *testing.T) {
			if got := passthru.Owns(tt.args); got != tt.want {
				t.Errorf("Owns(%q) = %v, want %v", tt.args, got, tt.want)
			}
		})
	}
}

// Without zk on PATH the failure has to name what was missing and what was
// being attempted, rather than reading as an unknown command.
func TestExecReportsMissingBinary(t *testing.T) {
	t.Setenv("PATH", t.TempDir())

	err := passthru.Exec([]string{"list"})
	if err == nil {
		t.Fatal("Exec() = nil error, want an error")
	}

	for _, want := range []string{"list", passthru.Binary, "PATH"} {
		if !strings.Contains(err.Error(), want) {
			t.Errorf("error %q does not mention %q", err, want)
		}
	}
}
