{
  description = "A Nix flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    systems.url = "github:UnstoppableMango/nix-systems";

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

    # apis already depends on a2b for the same buf library. Following its node
    # keeps one a2b, and its pulumi closure, in the lock rather than two.
    a2b.follows = "apis/a2b";
  };

  outputs =
    inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      imports = [
        inputs.systems.flakeModule
        inputs.treefmt-nix.flakeModule
      ];

      perSystem =
        {
          self',
          inputs',
          pkgs,
          system,
          ...
        }:
        let
          # The Android SDK and NDK are unfree, and the SDK carries a licence
          # that has to be accepted before it will evaluate at all. Confining
          # both to a nixpkgs of their own keeps every other output, and
          # anything a consumer builds from this flake, on the default config.
          androidNixpkgs = import inputs.nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };

          # gomobile compiles against the SDK platform matching its -androidapi
          # and refuses to run when that platform is absent, so the oldest
          # Android the app supports is pinned here rather than left to
          # whichever platform the SDK happens to ship. The NDK accepts 21
          # through 35; 24 is the floor that still reaches ordinary phones.
          androidApi = "24";

          androidBuild = androidNixpkgs.androidenv.composeAndroidPackages {
            includeNDK = true;
            platformVersions = [ androidApi ];
          };

          gomobile = androidNixpkgs.gomobile.override { androidPkgs = androidBuild; };
        in
        {
          packages.default = self'.packages.slip;
          packages.slip = pkgs.callPackage ./nix/package.nix { };

          # An attribute path cannot apply `.override`, so the zk-free build
          # needs an output of its own to be reachable from `nix build`.
          packages.slip-standalone = self'.packages.slip.override { withZk = false; };

          packages.generated = pkgs.callPackage ./nix/generated.nix {
            apisWorkspace = inputs'.apis.legacyPackages.unmangoApis.workspace;
            bufLib = inputs'.a2b.legacyPackages.lib.buf;
          };

          packages.generate = pkgs.callPackage ./nix/generate.nix {
            inherit (self'.packages) generated;
          };
          apps.generate.program = self'.packages.generate;

          # gen/ is checked in, so it can fall behind the pinned apis input.
          # This is the only thing that notices.
          checks.generate = pkgs.runCommand "check-generate" { } ''
            diff -ruN ${self'.packages.generated}/gen ${./gen}
            touch "$out"
          '';

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

          # Separate, because the Android SDK and NDK are several gigabytes of
          # unfree closure and none of it is needed to change a line of Go.
          devShells.android = pkgs.mkShellNoCC {
            inputsFrom = [ self'.devShells.default ];

            packages = [
              # gomobile builds mobile/ into an .aar. Its wrapper puts the SDK
              # on PATH and sets ANDROID_HOME; the JDK is what assembles the
              # archive once the NDK has compiled the Go side.
              gomobile
              pkgs.jdk
            ];

            # gomobile's wrapper appends its own store path to GOPATH, so an
            # unset GOPATH leaves the read-only store as the only entry and the
            # module cache has nowhere to go. Setting Go's own default here
            # puts a writable directory in front of it.
            shellHook = ''
              export GOPATH="''${GOPATH:-$HOME/go}"
            '';
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
