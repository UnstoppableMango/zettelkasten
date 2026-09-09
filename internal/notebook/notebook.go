// Package notebook locates a zk notebook. Mirroring zk's own discovery is what
// lets slip drop a note where zk will index it without being told.
package notebook

import (
	"os"
	"path/filepath"

	"github.com/spf13/afero"
)

// marker is the directory zk puts at a notebook root.
const marker = ".zk"

// EnvDir is the override zk itself honours.
const EnvDir = "ZK_NOTEBOOK_DIR"

// Find walks up from dir looking for a notebook root, git-style, and reports
// whether one was found.
func Find(fsys afero.Fs, dir string) (string, bool) {
	for {
		if ok, err := afero.DirExists(fsys, filepath.Join(dir, marker)); err == nil && ok {
			return dir, true
		}

		parent := filepath.Dir(dir)
		if parent == dir {
			return "", false
		}

		dir = parent
	}
}

// Dir resolves the notebook root the way zk does: the environment override
// first, then a walk up from cwd.
func Dir(fsys afero.Fs, cwd string) (string, bool) {
	if dir := os.Getenv(EnvDir); dir != "" {
		return dir, true
	}

	return Find(fsys, cwd)
}
