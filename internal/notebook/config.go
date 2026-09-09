package notebook

import (
	"errors"
	"fmt"
	"io/fs"
	"path/filepath"
	"strings"

	"github.com/spf13/afero"
)

// ConfigPath is the config file zk reads, relative to a notebook root.
var ConfigPath = filepath.Join(marker, "config.toml")

// dateKey is the frontmatter key slip writes the creation timestamp under. zk
// defaults to "date"; pointing it here is the one line that makes zk read a
// slip note's creation time rather than falling back to the filesystem.
const dateKey = "create_time"

// Stanza is the configuration that teaches zk about slip's frontmatter.
const Stanza = `[format.markdown.frontmatter]
creation-date-key = "` + dateKey + `"
`

// table is the TOML table Stanza defines.
const table = "[format.markdown.frontmatter]"

// ConfigState describes what, if anything, needs doing to a notebook's config.
type ConfigState int

const (
	// ConfigMissing means the stanza is absent and can be appended safely.
	ConfigMissing ConfigState = iota

	// ConfigPresent means zk is already pointed at slip's date key.
	ConfigPresent

	// ConfigConflict means the table exists with a different or absent key.
	// Appending would define the table twice, which is invalid TOML, so this
	// case is reported rather than repaired.
	ConfigConflict
)

// InspectConfig reports what state root's config file is in.
func InspectConfig(fsys afero.Fs, root string) (ConfigState, error) {
	path := filepath.Join(root, ConfigPath)

	ok, err := afero.Exists(fsys, path)
	if err != nil {
		return 0, fmt.Errorf("checking %s: %w", path, err)
	}

	if !ok {
		return ConfigMissing, nil
	}

	b, err := afero.ReadFile(fsys, path)
	if err != nil {
		return 0, fmt.Errorf("reading %s: %w", path, err)
	}

	// zk's own starter config documents this table in comments, so matching the
	// raw text would report a conflict on every freshly created notebook.
	var hasTable, hasKey bool

	for line := range strings.Lines(string(b)) {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		if strings.HasPrefix(line, table) {
			hasTable = true
		}

		if key, value, ok := strings.Cut(line, "="); ok &&
			strings.TrimSpace(key) == "creation-date-key" &&
			strings.Contains(value, dateKey) {
			hasKey = true
		}
	}

	switch {
	case hasKey:
		return ConfigPresent, nil
	case hasTable:
		return ConfigConflict, nil
	default:
		return ConfigMissing, nil
	}
}

// WriteConfig appends the stanza to root's config file, creating it if needed.
func WriteConfig(fsys afero.Fs, root string) error {
	path := filepath.Join(root, ConfigPath)

	if err := fsys.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return fmt.Errorf("creating %s: %w", filepath.Dir(path), err)
	}

	existing, err := afero.ReadFile(fsys, path)
	if err != nil && !errors.Is(err, fs.ErrNotExist) {
		return fmt.Errorf("reading %s: %w", path, err)
	}

	var buf strings.Builder
	if len(existing) > 0 {
		buf.Write(existing)

		if !strings.HasSuffix(string(existing), "\n") {
			buf.WriteString("\n")
		}

		buf.WriteString("\n")
	}

	buf.WriteString(Stanza)

	if err := afero.WriteFile(fsys, path, []byte(buf.String()), 0o644); err != nil {
		return fmt.Errorf("writing %s: %w", path, err)
	}

	return nil
}
