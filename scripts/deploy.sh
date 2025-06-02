#!/bin/bash
set -e

mkdir -p /home/ubuntu/logs
cd /home/ubuntu/deploy

# ✅ SSM 파라미터에서 환경변수 로드 (예: Prefix /spring/)
echo "[INFO] Fetching Spring Boot environment variables from SSM..."

aws ssm get-parameters-by-path \
  --path "/spring/prod/" \
  --with-decryption \
  --query "Parameters[*].{Name:Name,Value:Value}" \
  --output text | while read name value; do
    key=$(basename "$name" | tr '.' '_' | tr '[:lower:]' '[:upper:]')
    echo "$key=$value" >> .env
done

echo "[INFO] .env file generated:"
cat .env

# ✅ 기존 컨테이너 종료 및 정리
docker compose down || true

# ✅ 최신 이미지 풀 (필요 시)
docker pull 266735804784.dkr.ecr.ap-northeast-2.amazonaws.com/app-repo/my-spring-app:latest

# ✅ Docker Compose로 Spring + Redis 실행
docker compose up -d



