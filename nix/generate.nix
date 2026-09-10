# `make generate` writes into the working tree, so this is an app rather than a
# derivation. The tree itself is built by ./generated.nix.
{
  coreutils,
  generated,
  git,
  writeShellApplication,
}:
writeShellApplication {
  name = "slip-generate";

  runtimeInputs = [
    coreutils
    git
  ];

  text = ''
    root="$(git rev-parse --show-toplevel)"

    cd "$root"
    rm -rf gen
    cp -R --no-preserve=mode,ownership ${generated}/gen gen

    echo "generated from $(cat gen/SOURCE)"
  '';
}
