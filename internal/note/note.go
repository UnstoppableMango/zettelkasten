// Package note is the domain model for a captured zettel, along with its two
// serializations: markdown with YAML frontmatter, and the protobuf Note.
package note

import (
	"strings"
	"time"

	notev1 "github.com/UnstoppableMango/zettelkasten/gen/unmango/zettelkasten/note/v1alpha1"
)

// Note is a single zettel. Output-only proto fields are absent: they are either
// derivable (name), server-assigned (uid), or computed from the corpus by a
// later step (the link and word counts).
type Note struct {
	ZettelID    string
	Title       string
	Body        string
	Type        notev1.NoteType
	Format      notev1.ContentFormat
	Tags        []string
	SourceURI   string
	DisplayName string
	Labels      map[string]string
	Annotations map[string]string
	CreateTime  time.Time
}

// New builds a fleeting markdown note from raw captured text. The body is kept
// verbatim; the title is derived from it rather than removed.
//
// The timestamp is truncated to the second: the file is meant to be read, and
// nanoseconds on a hand-captured thought are noise.
func New(now time.Time, body string) Note {
	return Note{
		ZettelID:   ZettelID(now),
		Title:      TitleFrom(body),
		Body:       body,
		Type:       notev1.NoteType_NOTE_TYPE_FLEETING,
		Format:     notev1.ContentFormat_CONTENT_FORMAT_MARKDOWN,
		CreateTime: now.Truncate(time.Second),
	}
}

// Name is the AIP resource name.
func (n Note) Name() string {
	return "notes/" + n.ZettelID
}

// TitleFrom derives a title from the first non-empty line of body, dropping a
// leading ATX heading marker. An all-whitespace body has no title.
func TitleFrom(body string) string {
	for line := range strings.Lines(body) {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		return strings.TrimSpace(strings.TrimLeft(line, "#"))
	}

	return ""
}
