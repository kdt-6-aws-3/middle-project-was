#!/bin/bash
set -euo pipefail

JAR_SRC="${1:?Usage: sudo $0 <path to spring-petclinic-*.jar>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR=/opt/petclinic
CONF_DIR=/etc/petclinic
ENV_FILE="$CONF_DIR/petclinic.env"
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

echo "==> Application JAR -> $APP_DIR/petclinic.jar"
install -d -m 755 "$APP_DIR"
install -m 644 "$JAR_SRC" "$APP_DIR/petclinic.jar"

echo "==> Environment file -> $ENV_FILE"
install -d -m 755 "$CONF_DIR"
if [[ ! -f "$ENV_FILE" ]]; then
	install -m 600 "$SCRIPT_DIR/petclinic.env.example" "$ENV_FILE"
fi

echo "==> systemd service"
install -m 644 "$SCRIPT_DIR/petclinic.service" /etc/systemd/system/petclinic.service
systemctl daemon-reload
systemctl enable petclinic

if grep -v '^#' "$ENV_FILE" | grep -q '<'; then
	echo
	echo "Fill in the DB connection info, then run this script again:"
	echo "  sudo vi $ENV_FILE"
	exit 0
fi

echo "==> Starting"
systemctl restart petclinic
for _ in $(seq 1 60); do
	if curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
		echo "PetClinic WAS is UP: $HEALTH_URL"
		exit 0
	fi
	sleep 2
done
echo "Health check did not report UP within 120s. Logs: journalctl -u petclinic -n 100" >&2
exit 1
