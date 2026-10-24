#!/bin/bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_USER="$(stat -c %U "$REPO_DIR")"

if [[ $EUID -ne 0 ]]; then
	echo "Run as root: sudo bash $0" >&2
	exit 1
fi

echo "==> Build tools: git, Java 21 JDK (Amazon Corretto, includes javac)"
dnf install -y git java-21-amazon-corretto-devel

echo "==> Latest code"
sudo -H -u "$BUILD_USER" git -C "$REPO_DIR" pull --ff-only

echo "==> Build"
cd "$REPO_DIR"
sudo -H -u "$BUILD_USER" sh ./mvnw -B clean package

jars=(target/spring-petclinic-*.jar)
if [[ ${#jars[@]} -ne 1 || ! -f "${jars[0]}" ]]; then
	echo "Expected exactly one JAR in target/, found: ${jars[*]}" >&2
	exit 1
fi

echo "==> Install: ${jars[0]}"
bash "$REPO_DIR/deploy/setup-was.sh" "$REPO_DIR/${jars[0]}"
