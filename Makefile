build:
	nix build .#

test:
	go test ./...

generate:
	nix run .#generate

update:
	nix flake update

check lint:
	nix flake check

format fmt:
	nix fmt

# Refresh vendorHash in nix/package.nix after a dependency change.
gomod:
	nix-update --flake --version=skip default
