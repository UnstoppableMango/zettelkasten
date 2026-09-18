//go:build tools

package mobile

// gomobile resolves the bind runtime through the module graph, so this module
// has to require golang.org/x/mobile even though nothing here imports it.
// Without this file `go mod tidy` drops the requirement and the next
// `make bind` fails with "no required module provides package".
//
// The build tag means nothing compiles it. Build constraints do not hide an
// import from `go mod tidy`, which is the whole point.
import _ "golang.org/x/mobile/bind"
