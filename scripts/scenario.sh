#!/usr/bin/env bash
# README의 검증 시나리오 표를 curl로 실행한다.
# 사용법: ./scripts/scenario.sh <번호>
# 번호: 1 ~ 8 (없으면 1)

set -euo pipefail

BASE=${BASE:-http://localhost:8080}
N=${1:-1}

post() {
  curl -sS -X POST "$BASE/api/notifications" \
    -H 'Content-Type: application/json' \
    -d "$1"
  echo
}

case "$N" in
  1)
    echo "[1] HIGH 결제 알림 1건"
    post '{"userId":1000,"type":"PAYMENT_COMPLETED","priority":"HIGH","data":{"orderId":"A-1"},"idempotencyKey":"scn1-'"$(uuidgen)"'"}'
    ;;
  2)
    KEY="scn2-fixed-key"
    echo "[2-a] 첫 호출"
    post '{"userId":1000,"type":"PAYMENT_COMPLETED","priority":"HIGH","data":{"orderId":"A-2"},"idempotencyKey":"'"$KEY"'"}'
    echo "[2-b] 같은 idempotencyKey 재호출"
    post '{"userId":1000,"type":"PAYMENT_COMPLETED","priority":"HIGH","data":{"orderId":"A-2"},"idempotencyKey":"'"$KEY"'"}'
    ;;
  3)
    echo "[3] userId=1002 (푸시 OFF) + NORMAL"
    post '{"userId":1002,"type":"FRIEND_REQUEST","priority":"NORMAL","data":{"fromName":"홍길동"},"idempotencyKey":"scn3-'"$(uuidgen)"'"}'
    ;;
  4)
    echo "[4] userId=1002 (푸시 OFF) + CRITICAL → 무시하고 발송"
    post '{"userId":1002,"type":"PAYMENT_COMPLETED","priority":"CRITICAL","data":{"orderId":"CR-1"},"idempotencyKey":"scn4-'"$(uuidgen)"'"}'
    ;;
  5)
    echo "[5] userId=1003 (invalid token) + NORMAL → BadDeviceToken → 비활성화"
    post '{"userId":1003,"type":"FRIEND_REQUEST","priority":"NORMAL","data":{"fromName":"테스터"},"idempotencyKey":"scn5-'"$(uuidgen)"'"}'
    ;;
  6)
    echo "[6] 5번 직후 userId=1003 재발송 → 활성 디바이스 없음 → SKIP"
    sleep 2
    post '{"userId":1003,"type":"FRIEND_REQUEST","priority":"NORMAL","data":{"fromName":"테스터2"},"idempotencyKey":"scn6-'"$(uuidgen)"'"}'
    ;;
  7)
    echo "[7] 10건 일괄 발송 → 일부는 재시도/DLQ 자연 발생"
    for i in $(seq 1 10); do
      post '{"userId":1000,"type":"PAYMENT_COMPLETED","priority":"HIGH","data":{"orderId":"B-'"$i"'"},"idempotencyKey":"scn7-'"$(uuidgen)"'"}'
    done
    ;;
  8)
    echo "[8] BULK 30건 + 동시에 HIGH 1건 (우선순위 격리 시연)"
    curl -sS -X POST "$BASE/api/notifications/bulk?count=30&priority=LOW&userId=1000" >/dev/null &
    BULK_PID=$!
    sleep 0.2
    post '{"userId":1000,"type":"PAYMENT_COMPLETED","priority":"HIGH","data":{"orderId":"PRIORITY-1"},"idempotencyKey":"scn8-'"$(uuidgen)"'"}'
    wait $BULK_PID || true
    echo "  → 로그에서 [HIGH 풀] ✓ 도달이 [BULK 풀] 처리 중간에 먼저 나타나는지 확인"
    ;;
  *)
    echo "사용법: $0 <1-8>"
    exit 1
    ;;
esac
