# infra/nginx

`devbridge.conf`는 운영 EC2 nginx 사이트 설정의 원본이다. 운영 서버에서 직접 고치지 않고 이 파일을 고친 뒤 반영한다.

## 반영 절차 (수동)

자동 배포(`deploy.yml`)와 연결돼 있지 않다. 잘못된 설정이 reload되면 서비스 전체가 멈추므로 사람이 확인하며 반영한다.

1. 파일 전송: `scp infra/nginx/devbridge.conf <EC2>:/tmp/devbridge.conf`
2. 운영 경로로 복사: `sudo cp /tmp/devbridge.conf /etc/nginx/sites-available/devbridge` (실제 경로는 아래 확인 항목 참고)
3. 문법 검사: `sudo nginx -t` — 실패하면 reload하지 않는다.
4. 반영: `sudo systemctl reload nginx`

## 서버 확인 필요 항목 (첫 반영 전 1회)

이 파일은 2026-06 배포 기록을 바탕으로 만든 초안이며 운영 설정과 아직 대조하지 않았다.
운영 EC2에서 `sudo nginx -T`로 활성 설정을 확인하고 아래 항목을 맞춘 뒤 반영한다.

- [ ] 사이트 설정 파일의 실제 경로 (`ls /etc/nginx/sites-enabled/ /etc/nginx/conf.d/`)
- [ ] `server_name` (현재 `devbridge-ax.cloud`)
- [ ] TLS 종료 위치: 앞단(Cloudflare 등)이면 현행 `listen 80` 유지, nginx에서 종료하면 `listen 443 ssl`과 인증서 경로 추가
- [ ] 앞단에서 TLS를 종료하면 `X-Forwarded-Proto`를 `$scheme` 대신 `$http_x_forwarded_proto`로 변경
- [ ] 운영의 `/ws/` `proxy_read_timeout` 값 (없으면 이 파일의 3600s 사용)
- [ ] 운영에만 있는 location(프론트 정적 서빙 `location /` 등)과 `client_max_body_size`를 이 파일로 옮김
- [ ] 반영 후 access log에 `token=***`로 기록되는지 확인 (`sudo tail /var/log/nginx/devbridge.access.log`)

## 로그 마스킹

- access log: `map`과 `log_format masked`로 `?token=` 값을 `***`로 바꿔 기록한다.
- error log: 형식을 바꿀 수 없고, 업스트림 오류 시 요청 줄을 토큰째 남긴다. 그래서 `/ws/`에서만 `error_log ... crit`으로 올려 기록하지 않는다. WebSocket 연결 오류 원인은 백엔드 로그(`docker compose -f docker-compose.prod.yml logs app`)에서 확인한다.
