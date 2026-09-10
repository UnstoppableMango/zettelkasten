package note

import "time"

// idLayout is the timestamp scheme the proto documents for zettel_id, in local
// time so the digits match the wall clock the note was captured against.
const idLayout = "200601021504"

// ZettelID formats t as a permanent address.
func ZettelID(t time.Time) string {
	return t.Format(idLayout)
}

// NextID returns id, or id with the shortest alphabetic suffix that taken
// rejects. Minute-precision IDs collide whenever two thoughts land in the same
// minute, which is routine when pasting.
func NextID(id string, taken func(string) bool) string {
	if !taken(id) {
		return id
	}

	for i := 0; ; i++ {
		if candidate := id + suffix(i); !taken(candidate) {
			return candidate
		}
	}
}

// suffix counts in lowercase letters: a, b, ... z, aa, ab, ...
func suffix(i int) string {
	out := []byte{}
	for {
		out = append([]byte{byte('a' + i%26)}, out...)
		if i /= 26; i == 0 {
			return string(out)
		}
		i--
	}
}
