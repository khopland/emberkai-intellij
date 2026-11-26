// Check Go colors in GoLand, or IntelliJ IDEA Ultimate with the Go plugin: packages,
// types, method receivers, built-ins, constants, and generics.
package main

import (
	"fmt"
	"strings"
)

const defaultTone = "warm"

// Swatch is a palette color.
type Swatch struct {
	Name string
	Hex  uint32
}

type Describer interface {
	Describe(upper bool) string
}

func (s Swatch) Describe(upper bool) string {
	name := s.Name
	if upper {
		name = strings.ToUpper(name)
	}
	return fmt.Sprintf("%-8s #%06x %s", name, s.Hex, defaultTone)
}

func First[T any](items []T) (T, bool) {
	var zero T
	if len(items) == 0 {
		return zero, false
	}
	return items[0], true
}

func main() {
	swatches := []Swatch{{"ember", 0xfd9353}, {"teal", 0x5ad4e6}}
	if first, ok := First(swatches); ok {
		var d Describer = first
		fmt.Println(d.Describe(true), len(swatches), cap(swatches))
	}
}
