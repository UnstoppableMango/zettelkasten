{
  lib,
  buildGoModule,
  go_1_27,
  makeWrapper,
  zk,
  # slip hands any command it does not implement to zk. Wrapping PATH makes that
  # work out of the box; disable it for a binary with no zk in its closure.
  #
  # zk is GPL-3.0 and slip is MIT. Putting a program on PATH is invoking it, not
  # linking against it, so this stays an arms-length boundary.
  withZk ? true,
}:
(buildGoModule.override { go = go_1_27; }) (finalAttrs: {
  pname = "slip";
  version = "0.1.0";

  src = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../go.mod
      ../go.sum
      ../cmd
      ../gen
      ../internal
    ];
  };

  vendorHash = "sha256-oGVLx71JuDmLf2a6bhZZ6PTRq9L6ZAcG7z/BKU18nbc=";

  subPackages = [ "cmd/slip" ];

  nativeBuildInputs = lib.optional withZk makeWrapper;

  postInstall = lib.optionalString withZk ''
    wrapProgram $out/bin/slip --suffix PATH : ${lib.makeBinPath [ zk ]}
  '';

  ldflags = [
    "-s"
    "-w"
    "-X main.version=${finalAttrs.version}"
  ];

  meta = {
    description = "Capture zettelkasten notes";
    homepage = "https://github.com/UnstoppableMango/zettelkasten";
    license = lib.licenses.mit;
    mainProgram = "slip";
  };
})
