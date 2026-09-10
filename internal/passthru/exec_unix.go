//go:build unix

package passthru

import (
	"os"
	"syscall"
)

// run replaces the current process, so nothing of slip survives to interfere.
func run(path string, argv []string) error {
	return syscall.Exec(path, argv, os.Environ())
}
