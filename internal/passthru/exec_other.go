//go:build !unix

package passthru

import (
	"os"
	"os/exec"
)

// run forwards to a child process on platforms without exec(2). The exit code
// is propagated; signal handling is the runtime's rather than zk's.
func run(path string, argv []string) error {
	cmd := exec.Command(path, argv[1:]...)
	cmd.Stdin, cmd.Stdout, cmd.Stderr = os.Stdin, os.Stdout, os.Stderr

	return cmd.Run()
}
