{
  description = "A Nix flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    systems.url = "github:nix-systems/triplet";

    flake-parts = {
      url = "github:hercules-ci/flake-parts";
      inputs.nixpkgs-lib.follows = "nixpkgs";
    };

    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    apis = {
      url = "github:unmango/apis";
      inputs.nixpkgs.follows = "nixpkgs";
      inputs.systems.follows = "systems";
      inputs.flake-parts.follows = "flake-parts";
      inputs.treefmt-nix.follows = "treefmt-nix";
    };
  };

  outputs =
    inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      imports = [ inputs.treefmt-nix.flakeModule ];

      perSystem =
        {
          self',
          inputs',
          pkgs,
          ...
        }:
        {
          packages.default = self'.packages.slip;
          packages.slip = pkgs.callPackage ./nix/package.nix { };

          # An attribute path cannot apply `.override`, so the zk-free build
          # needs an output of its own to be reachable from `nix build`.
          packages.slip-standalone = self'.packages.slip.override { withZk = false; };

          packages.generate = pkgs.callPackage ./nix/generate.nix {
            apisWorkspace = inputs'.apis.legacyPackages.unmangoApis.workspace;
          };
          apps.generate.program = self'.packages.generate;

          # buildGoModule already runs `go test ./...`; this adds vet and the
          # race detector on top of it.
          checks.vet = self'.packages.slip.overrideAttrs {
            checkPhase = ''
              runHook preCheck
              go vet ./...
              go test -race ./...
              runHook postCheck
            '';
          };

          devShells.default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              buf
              gnumake
              go_1_27
              gofumpt
              gopls
              gotools
              nix-update
              nixfmt
            ];
          };

          treefmt.programs = {
            actionlint.enable = true;
            gofumpt.enable = true;
            mdformat.enable = true;
            nixfmt.enable = true;

            yamllint = {
              enable = true;
              settings.document-start = "disable";
            };
          };

          # Generated protobuf code is checked in verbatim. Formatting it would
          # put every regeneration permanently at odds with the generator.
          # Golden files are compared byte for byte, and a note is markdown whose
          # frontmatter fences mdformat reads as headings. Formatting either
          # tree rewrites the thing under test.
          treefmt.settings.global.excludes = [
            "gen/**"
            "**/testdata/**"
            "flake.lock"
            "LICENSE"
          ];
        };
    };
}
