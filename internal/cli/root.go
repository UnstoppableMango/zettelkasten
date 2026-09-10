// Package cli wires the command surface.
package cli

import (
	"slices"

	"github.com/spf13/cobra"
	"github.com/spf13/pflag"
)

// Version is set at build time.
var Version = "dev"

// New builds the root command. Anything it does not own is handed to zk before
// cobra ever sees it, so the owned set is deliberately small.
func New() *cobra.Command {
	root := &cobra.Command{
		Use:   "slip",
		Short: "Capture zettelkasten notes",
		Long: "slip captures a thought into a zettel with as little ceremony as possible.\n\n" +
			"Any command slip does not implement is passed through to zk.",
		Version:       Version,
		SilenceUsage:  true,
		SilenceErrors: true,
		Args:          cobra.ArbitraryArgs,
	}

	capture := newCapture()

	// Running `slip` with no subcommand captures, so the fastest path to a note
	// is the shortest thing to type.
	root.RunE = capture.RunE
	root.Flags().AddFlagSet(capture.Flags())
	root.AddCommand(capture)
	root.AddCommand(newInit())

	return root
}

// Flags returns the options the root command accepts, including the help and
// version flags cobra would otherwise only add once it starts executing.
//
// Anything absent from this set belongs to zk, so an option slip does not
// register must reach zk rather than being rejected here.
func Flags() *pflag.FlagSet {
	root := New()
	root.InitDefaultHelpFlag()
	root.InitDefaultVersionFlag()

	return root.Flags()
}

// Owned reports whether name is a command slip implements, which is what
// decides between handling an invocation and passing it to zk.
func Owned(name string) bool {
	for _, cmd := range New().Commands() {
		if cmd.Name() == name || slices.Contains(cmd.Aliases, name) {
			return true
		}
	}

	return false
}
