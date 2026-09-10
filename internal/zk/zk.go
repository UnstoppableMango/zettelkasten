// Package zk reads a notebook through the zk CLI.
//
// zk already parses markdown properly: hashtag, colon and frontmatter tags,
// wiki and markdown links with their offsets and rels, title resolution. Asking
// it for JSON is cheaper and more faithful than reimplementing any of that, and
// it keeps the boundary at a subprocess rather than a linked GPL library.
//
// The output format is stable in practice but carries no version and no
// compatibility promise, so decoding here is deliberately lenient and pinned by
// a fixture rather than trusted.
package zk

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os/exec"
	"time"
)

// Binary is the command this package shells out to.
const Binary = "zk"

// Note is one entry of `zk list --format jsonl`. Fields zk emits but slip has
// no use for yet are omitted; unknown keys are ignored rather than rejected.
type Note struct {
	Filename     string         `json:"filename"`
	FilenameStem string         `json:"filenameStem"`
	Path         string         `json:"path"`
	AbsPath      string         `json:"absPath"`
	Title        string         `json:"title"`
	Link         string         `json:"link"`
	Lead         string         `json:"lead"`
	Body         string         `json:"body"`
	RawContent   string         `json:"rawContent"`
	WordCount    int            `json:"wordCount"`
	Tags         []string       `json:"tags"`
	Metadata     map[string]any `json:"metadata"`
	Created      time.Time      `json:"created"`
	Modified     time.Time      `json:"modified"`
	Checksum     string         `json:"checksum"`
}

// ZettelID returns the slip identity zk carried through in its metadata map,
// which is how a slip note is recognised among notes from anywhere else.
func (n Note) ZettelID() string {
	id, _ := n.Metadata["zettel_id"].(string)
	return id
}

// ParseList decodes the JSON-lines stream `zk list --format jsonl` produces.
func ParseList(r io.Reader) ([]Note, error) {
	var notes []Note

	scanner := bufio.NewScanner(r)

	// Note bodies are arbitrarily long, and the default 64KiB token limit would
	// truncate a single long note into a parse error.
	scanner.Buffer(make([]byte, 0, 64*1024), 8*1024*1024)

	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue
		}

		var n Note
		if err := json.Unmarshal(line, &n); err != nil {
			return nil, fmt.Errorf("parsing zk output: %w", err)
		}

		notes = append(notes, n)
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("reading zk output: %w", err)
	}

	return notes, nil
}

// List runs zk against dir and returns everything it knows. zk reindexes on
// every invocation, so a note written a moment ago is already included.
func List(ctx context.Context, dir string) ([]Note, error) {
	cmd := exec.CommandContext(ctx, Binary,
		"list", "--quiet", "--no-pager", "--format", "jsonl",
	)
	cmd.Dir = dir

	out, err := cmd.Output()
	if err != nil {
		// Output captures stderr into ExitError.Stderr, but its message drops
		// it, which loses the only useful part of a config parse failure.
		var exit *exec.ExitError
		if errors.As(err, &exit) && len(exit.Stderr) > 0 {
			return nil, fmt.Errorf("running %s list in %s: %w: %s",
				Binary, dir, err, bytes.TrimSpace(exit.Stderr),
			)
		}

		return nil, fmt.Errorf("running %s list in %s: %w", Binary, dir, err)
	}

	return ParseList(bytes.NewReader(out))
}
