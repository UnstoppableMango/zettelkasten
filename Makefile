build:
	nix build .#

test:
	go test ./...

generate:
	nix run .#generate

# The Android archive the capture app links against. -androidapi has to match
# the SDK platform the dev shell pins, and gomobile will not infer it.
bind: mobile/slip.aar
mobile/slip.aar: $(shell find internal mobile -name '*.go')
	gomobile bind -target=android -androidapi 24 -o $@ ./mobile

update:
	nix flake update

check lint:
	nix flake check

format fmt:
	nix fmt

# Refresh vendorHash in nix/package.nix after a dependency change.
gomod:
	nix-update --flake --version=skip default
