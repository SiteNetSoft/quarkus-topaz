#!/usr/bin/env bash
# Regenerates the committed gRPC stubs from the Topaz protos on the Buf Schema Registry.
# Requires: podman (rootless), mvn, JAVA_HOME pointing at a JDK 21+.
set -euo pipefail

BASE_DIR="$(git rev-parse --show-toplevel)"
PROTO_DIR="${BASE_DIR}/grpc/generator/src/main/proto"
PROTO_DIR_IN_CONTAINER="grpc/generator/src/main/proto"
GENERATED_DIR="${BASE_DIR}/grpc/generator/target/generated-sources/grpc"
TARGET_DIR="${BASE_DIR}/grpc/stubs/src/main/java"

cd "${BASE_DIR}"

eval_prop() {
  mvn -q -N help:evaluate -Dexpression="$1" -DforceStdout
}
PB_AUTHORIZER_TAG="${1:-$(eval_prop pb-authorizer.tag)}"
PB_DIRECTORY_TAG="${2:-$(eval_prop pb-directory.tag)}"

buf_export() {
  podman run --rm --userns=keep-id --user "$(id -u):$(id -g)" -e HOME=/tmp -e XDG_CACHE_HOME=/tmp/.cache \
    -v "${PWD}:/workspace:Z" -w /workspace docker.io/bufbuild/buf export "$1" -o "${PROTO_DIR_IN_CONTAINER}"
}

echo "Updating proto files (authorizer ${PB_AUTHORIZER_TAG}, directory ${PB_DIRECTORY_TAG})"
rm -rf "${PROTO_DIR}"
mkdir -p "${PROTO_DIR}"

buf_export buf.build/grpc-ecosystem/grpc-gateway
# keep only the openapiv2 options protos from grpc-gateway
find "${PROTO_DIR}" -mindepth 1 -maxdepth 1 ! -name protoc-gen-openapiv2 -exec rm -rf {} +
find "${PROTO_DIR}/protoc-gen-openapiv2" -mindepth 1 -maxdepth 1 ! -name options -exec rm -rf {} +

buf_export "buf.build/aserto-dev/authorizer:${PB_AUTHORIZER_TAG}"
buf_export "buf.build/aserto-dev/directory:${PB_DIRECTORY_TAG}"

# google/* is resolved from proto-google-common-protos; v2 directory protos are not wanted
rm -rf "${PROTO_DIR}/google"
rm -rf "${PROTO_DIR}"/aserto/directory/*/v2

# Topaz protos ship without java_multiple_files; we want top-level Java classes (idempotent).
find "${PROTO_DIR}" -name '*.proto' -print0 | while IFS= read -r -d '' f; do
  grep -q '^option java_multiple_files' "$f" || sed -i '0,/^package .*;/s//&\noption java_multiple_files = true;/' "$f"
done

if [ -z "$(find "${PROTO_DIR}/aserto" -type f -name '*.proto' 2>/dev/null | head -n 1)" ]; then
  echo "ERROR: ${PROTO_DIR}/aserto is empty after the proto export" >&2
  exit 1
fi

echo "Generating gRPC stubs"
mvn -Pgenerate -pl grpc/generator clean package

if [ -z "$(find "${GENERATED_DIR}" -type f 2>/dev/null | head -n 1)" ]; then
  echo "ERROR: ${GENERATED_DIR} is empty after generation" >&2
  exit 1
fi

echo "Replacing committed stubs"
rm -rf "${TARGET_DIR}/aserto" "${TARGET_DIR}/grpc"
mkdir -p "${TARGET_DIR}"
cp -r "${GENERATED_DIR}"/* "${TARGET_DIR}"

echo "Done: stubs regenerated from authorizer ${PB_AUTHORIZER_TAG} and directory ${PB_DIRECTORY_TAG}"
