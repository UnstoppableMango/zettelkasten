// Package passthru hands invocations slip does not implement to zk.
//
// zk is invoked as a separate process, never linked. It is GPL-3.0 and this is
// not; running a program is not deriving from it.
package passthru

import (
	"fmt"
	"os/exec"
	"strings"

	"github.com/UnstoppableMango/zettelkasten/internal/cli"
)

// Binary is the command unowned invocations are handed to.
const Binary = "zk"

// builtins are the commands cobra adds for itself. They are only registered
// once a command runs, so Owns cannot discover them by asking.
var builtins = []string{"help", "completion"}

// Owns reports whether slip handles this argv itself. Everything else belongs
// to zk, which is what makes one binary cover both.
func Owns(args []string) bool {
	// Bare `slip` captures.
	if len(args) == 0 {
		return true
	}

	first := args[0]

	// A leading flag is slip's own: `slip --version`, `slip --dir x`.
	if strings.HasPrefix(first, "-") {
		return true
	}

	for _, name := range builtins {
		if first == name {
			return true
		}
	}

	return cli.Owned(first)
}

// Exec replaces this process with zk. Replacing rather than wrapping means
// signals, exit codes, and the terminal all belong to zk, with nothing of ours
// left in the pipeline to get them wrong.
func Exec(args []string) error {
	path, err := exec.LookPath(Binary)
	if err != nil {
		return fmt.Errorf(
			"%s is not a slip command, and %s was not found on PATH to pass it to: %w",
			args[0], Binary, err,
		)
	}

	return run(path, append([]string{Binary}, args...))
}
