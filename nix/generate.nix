# `make generate` writes into the working tree, so this is an app rather than a
# derivation. The apis workspace is baked in at build time, which keeps the
# proto imports resolvable with no network and no BSR.
{
  apisWorkspace,
  buf,
  coreutils,
  git,
  protoc-gen-go,
  writeShellApplication,
}:
writeShellApplication {
  name = "slip-generate";

  runtimeInputs = [
    buf
    coreutils
    git
    protoc-gen-go
  ];

  text = ''
    root="$(git rev-parse --show-toplevel)"
    ws="${apisWorkspace}"

    # buf wants a writable cache.
    HOME="$(mktemp -d)"
    export HOME

    cd "$root"
    rm -rf gen
    mkdir -p gen

    # --path resolves against the working directory, so these are the absolute
    # store paths. Only the transitive closure the zettelkasten protos actually
    # import is generated; the rest of the workspace is resolved but skipped.
    buf generate --template buf.gen.yaml "$ws" \
      --path "$ws/proto/unmango/zettelkasten" \
      --path "$ws/proto/unmango/ref" \
      --path "$ws/third_party/googleapis/google/api/field_behavior.proto" \
      --path "$ws/third_party/googleapis/google/api/field_info.proto" \
      --path "$ws/third_party/googleapis/google/api/resource.proto" \
      --path "$ws/third_party/k8s/k8s.io/apimachinery/pkg/apis/meta/v1/generated.proto" \
      --path "$ws/third_party/k8s/k8s.io/apimachinery/pkg/runtime/generated.proto" \
      --path "$ws/third_party/k8s/k8s.io/apimachinery/pkg/runtime/schema/generated.proto"

    echo "$ws" > gen/SOURCE
    echo "generated from $ws"
  '';
}
