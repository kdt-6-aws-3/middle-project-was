#!/bin/bash
# DB 접속 정보를 Parameter Store(/petclinic/db, SecureString)에서 읽어 환경 변수로 넘기고 앱을 실행한다.
#
# 이 방식을 고른 이유
# - env 파일 방식: 비밀번호가 디스크와 AMI에 남고, 바꾸려면 AMI를 다시 만들어야 한다.
# - 사용자 데이터 방식: 첫 부팅에만 실행되고, 결국 디스크에 env 파일을 다시 쓴다.
# - Spring Cloud AWS: Boot 4.1 공식 지원이 없고, AWS SDK로 JAR +17MB, 메모리 +20~40MB (1GB 인스턴스에 부담).
# - 이 스크립트: 서버에 이미 있는 AWS CLI를 쓰므로 앱 코드·용량·메모리 변화가 없고,
#   값은 이 프로세스 환경 변수에만 있어 파일로 남지 않으며, 재시작만 하면 새 값을 읽는다.
#   값을 명령줄 인자(-D)로 넘기지 않는 이유: ps로 다른 사용자에게 보인다.
# 이름을 지정해 조회(get-parameters)하는 이유: SSM 접속용으로 이미 붙어 있는
# AmazonSSMManagedInstanceCore에 GetParameters 권한이 있어 IAM 정책을 추가할 필요가 없다.
set -euo pipefail

PARAM_PATH=/petclinic/db

TOKEN=$(curl -sf -X PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 60") \
	|| { echo "Cannot reach instance metadata (IMDSv2)" >&2; exit 1; }
REGION=$(curl -sf -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)

params=$(aws ssm get-parameters --region "$REGION" --with-decryption \
	--names "$PARAM_PATH/url" "$PARAM_PATH/username" "$PARAM_PATH/password" "$PARAM_PATH/type" \
	--query 'Parameters[].[Name,Value]' --output text) \
	|| { echo "Cannot read $PARAM_PATH from Parameter Store (check the instance IAM role)" >&2; exit 1; }

while IFS=$'\t' read -r name value; do
	case "${name##*/}" in
		url) export SPRING_DATASOURCE_URL="$value" ;;
		username) export SPRING_DATASOURCE_USERNAME="$value" ;;
		password) export SPRING_DATASOURCE_PASSWORD="$value" ;;
		type) export SPRING_PROFILES_ACTIVE="$value" ;;
	esac
done <<< "$params"

for var in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD SPRING_PROFILES_ACTIVE; do
	if [[ -z "${!var:-}" ]]; then
		echo "Missing parameter for $var under $PARAM_PATH" >&2
		exit 1
	fi
done

# inetaddr.ttl=5: DB 페일오버 후 바뀐 RDS IP를 5초 안에 다시 조회한다 (기본 30초).
exec /usr/bin/java -Dsun.net.inetaddr.ttl=5 -Xms256m -Xmx512m -jar /opt/petclinic/petclinic.jar
