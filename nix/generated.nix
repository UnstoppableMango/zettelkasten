# The generated protobuf tree, built from the apis workspace baked in at build
# time, which keeps the proto imports resolvable with no network and no BSR.
# `make generate` copies this into `gen/`; `checks.generate` diffs it against
# what is checked in.
{
  apisWorkspace,
  bufLib,
  protoc-gen-go,
}:
bufLib.generate {
  name = "slip-gen";

  # The workspace root, so the k8s.io and google/api imports resolve as sibling
  # modules rather than BSR dependencies.
  src = apisWorkspace;

  # The in-tree template, so `buf generate` in the dev shell and this derivation
  # read the same configuration.
  template = ../buf.gen.yaml;

  # `local: protoc-gen-go` resolves off PATH.
  env.nativeBuildInputs = [ protoc-gen-go ];

  # Only the transitive closure the zettelkasten protos actually import is
  # generated; the rest of the workspace is resolved but skipped. Paths resolve
  # against the working directory, so they are the absolute store paths.
  paths = [
    "${apisWorkspace}/proto/unmango/zettelkasten"
    "${apisWorkspace}/proto/unmango/ref"
    "${apisWorkspace}/third_party/googleapis/google/api/field_behavior.proto"
    "${apisWorkspace}/third_party/googleapis/google/api/field_info.proto"
    "${apisWorkspace}/third_party/googleapis/google/api/resource.proto"
    "${apisWorkspace}/third_party/k8s/k8s.io/apimachinery/pkg/apis/meta/v1/generated.proto"
    "${apisWorkspace}/third_party/k8s/k8s.io/apimachinery/pkg/runtime/generated.proto"
    "${apisWorkspace}/third_party/k8s/k8s.io/apimachinery/pkg/runtime/schema/generated.proto"
  ];

  # Records the workspace the tree came from.
  env.postRun = ''
    echo "${apisWorkspace}" > "$out/gen/SOURCE"
  '';
}
