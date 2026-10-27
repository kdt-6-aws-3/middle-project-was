#!/bin/bash
set -euo pipefail

JAR_SRC="${1:?Usage: sudo $0 <path to spring-petclinic-*.jar>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR=/opt/petclinic
HEALTH_URL=http://localhost:8080/actuator/health

if [[ $EUID -ne 0 ]]; then
	echo "Run as root: sudo $0 $JAR_SRC" >&2
	exit 1
fi
if [[ ! -f "$JAR_SRC" ]]; then
	echo "JAR not found: $JAR_SRC" >&2
	exit 1
fi

echo "==> Java 21 (Amazon Corretto)"
dnf install -y java-21-amazon-corretto-headless
java -version

echo "==> Service user"
id petclinic &>/dev/null || useradd --system --no-create-home --shell /sbin/nologin petclinic

echo "==> Application JAR and start script -> $APP_DIR"
install -d -m 755 "$APP_DIR"
install -m 644 "$JAR_SRC" "$APP_DIR/petclinic.jar"
install -m 755 "$SCRIPT_DIR/start.sh" "$APP_DIR/start.sh"

echo "==> systemd service"
install -m 644 "$SCRIPT_DIR/petclinic.service" /etc/systemd/system/petclinic.service
systemctl daemon-reload
systemctl enable petclinic

echo "==> Starting"
systemctl restart petclinic
for _ in $(seq 1 150); do
	if curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
		echo "PetClinic WAS is UP: $HEALTH_URL"
		exit 0
	fi
	sleep 2
done
echo "Health check did not report UP within 300s. Logs: journalctl -u petclinic -n 100" >&2
exit 1
