# simple-push-demo-service

블로그 글 [APNs에 던지면 끝일까 — 대용량 iOS Push 알림 시스템 설계](#)의 아키텍처를 실제 동작하는 코드로 옮기는 학습 프로젝트.

대용량 트래픽을 실제로 던지는 건 PC가 못 버티니까, **수십 건 수준의 메시지로 모든 경로(우선순위 분기 / 재시도 / DLQ / 멱등성)가 로그로 보이는** 미니 시스템을 만든다.

## 학습 목표

이 프로젝트로 확인하고 싶은 것들.

- **우선순위별 큐 격리가 실제로 동작하는가** — 마케팅 100건이 큐에 쌓여도 결제 알림 1건이 먼저 처리되는가
- **Redis 멱등 키가 중복 요청을 막는가** — 같은 키로 두 번 호출했을 때 한 번만 발송되는가
- **Outbox 없이 단순 발행해도 어디까지 안전한가** — at-least-once 보장 + 컨슈머 멱등성으로 얼마나 커버되는가
- **Spring Kafka의 `@RetryableTopic`은 어떻게 동작하는가** — 자동 생성되는 retry/dlt 토픽의 명명 규칙과 흐름
- **`@DltHandler`로 DLQ에 들어온 메시지를 어떻게 다루는가** — 운영자 알림 + 분석용 저장 패턴
- **외부 의존(APNs) 장애를 어떻게 모킹하고 격리하는가** — Mock 클라이언트로 실패율을 조절해 재시도 흐름 관찰

## 아키텍처 (목표)

```
HTTP POST /api/notifications
      │
      ▼
┌────────────────────┐
│ NotificationAPI    │  ① Redis 멱등 키 체크
└────────┬───────────┘
         │
         ▼  Kafka: notification.requested
┌────────────────────┐
│ Orchestrator       │  ② 사용자 설정 / 디바이스 토큰 / 템플릿
└────────┬───────────┘  ③ 우선순위에 맞는 토픽으로 분기
         │
   ┌─────┼─────┬─────────┐
   ▼     ▼     ▼         ▼
push.    push. push.    push.
critical high  normal   bulk
   │     │     │         │
   ▼     ▼     ▼         ▼
┌─────────┐  ┌─────────┐ ┌─────────┐
│ HIGH 풀 │  │ NORMAL  │ │ BULK 풀 │   ④ Worker가 APNs 호출
│ (3 워커)│  │ (2 워커)│ │ (1 워커)│
└────┬────┘  └────┬────┘ └────┬────┘
     ▼            ▼           ▼
            MockApnsClient
     │            │           │
     │ 실패 시: @RetryableTopic이
     │   exponential backoff + jitter로 5회 재시도
     ▼
push.high.dlt, push.normal.dlt, push.bulk.dlt   ⑤ DLQ
     │
     ▼
@DltHandler → 운영자 알림 + 분석용 기록
```

## 기술 스택

| 영역 | 선택 | 이유 |
| --- | --- | --- |
| 언어/런타임 | Java 21 | LTS, virtual thread도 실험 가능 |
| 프레임워크 | Spring Boot 3.3 | 익숙함, Spring Kafka의 `@RetryableTopic` 완성도 |
| 메시지 큐 | Apache Kafka 3.7 (KRaft 모드) | Zookeeper 없이 단일 컨테이너 |
| 멱등 키 저장소 | Redis 7 | SETNX + TTL이 정석 |
| 컨테이너 | Docker Compose | 단일 명령으로 전체 띄우기 |
| 빌드 | Maven | (취향대로 Gradle로 바꿔도 무방) |

DB는 일부러 뺀다. 사용자 설정/디바이스 토큰은 인메모리 시드로 충분 — 이 데모의 관심사는 **메시징 흐름**이지 영속성이 아님.

## 구현 로드맵

각 Phase는 *그 단계만으로도 동작하는* 단위로 잘랐다. 한 단계 끝낼 때마다 commit + 검증 시나리오 한 줄.

### Phase 0 — 환경 셋업

- [ ] `docker-compose.yml` 작성 (Kafka KRaft + Redis)
- [ ] `Dockerfile` 작성 (Maven 멀티스테이지)
- [ ] 빈 Spring Boot 앱 띄우기 → `/actuator/health` 응답 확인
- [ ] Kafka 토픽 자동 생성 설정 또는 `NewTopic` Bean

**검증**: `docker compose up` → 세 컨테이너 모두 healthy

### Phase 1 — Notification API + 멱등성

- [ ] `NotificationRequest` record (userId, type, priority, data, idempotencyKey)
- [ ] `Priority` enum + 토픽 매핑 (`CRITICAL`/`HIGH`/`NORMAL`/`LOW`)
- [ ] `IdempotencyStore` (Redis `setIfAbsent` + 24h TTL)
- [ ] `NotificationController` + `NotificationApiService` — 멱등 체크 후 `notification.requested` 토픽으로 발행

**검증**:
- 정상 요청 → 202 Accepted
- 같은 `idempotencyKey`로 재호출 → "duplicate" 응답
- Redis에 `idem:<key>` 키 존재 확인

### Phase 2 — Orchestrator

- [ ] `UserSettings` 모델 (pushEnabled, dndStart/End, language)
- [ ] `UserSettingsService` — 인메모리 시드 사용자 5명 (`1000`~`1004`)
    - `1000`: 정상
    - `1002`: `pushEnabled=false`
    - `1003`: 디바이스 토큰이 `invalid-`로 시작 (의도적 무효)
- [ ] `DeviceTokenService` — 사용자별 토큰 + `deactivate(token)`
- [ ] `TemplateRenderer` — `PAYMENT_COMPLETED`, `FRIEND_REQUEST`, `MARKETING_PROMO` 최소 3종
- [ ] `NotificationOrchestrator` — `notification.requested` 컨슘 → 검사/렌더링 → 우선순위 토픽으로 분기
- [ ] CRITICAL은 DND 무시, NORMAL/LOW는 DND 적용

**검증**:
- `userId=1002` + NORMAL → "푸시 OFF → SKIP" 로그
- `userId=1002` + CRITICAL → 정상 분기 (DND 무시)
- DND 시간대(예: 23시 테스트) + NORMAL → SKIP
- 분기된 토픽에 메시지 들어가는지 `kafka-console-consumer`로 확인

### Phase 3 — APNs Worker (우선순위별 풀)

- [ ] `MockApnsClient` — 결과 분포: SUCCESS 75% / RETRYABLE 20% / INVALID 5%
    - 토큰이 `invalid-`로 시작하면 항상 INVALID
- [ ] `ApnsWorker` — `@KafkaListener` 3개:
    - `push.critical`+`push.high` 묶어서 concurrency=3, groupId=`apns-high-pool`
    - `push.normal` concurrency=2, groupId=`apns-normal-pool`
    - `push.bulk` concurrency=1, groupId=`apns-bulk-pool`
- [ ] INVALID_TOKEN 응답 시 토큰 비활성화 + 종료 (재시도 X)
- [ ] SUCCESS 응답 시 로그만

**검증**:
- HIGH 알림 → "[HIGH 풀] APNs 호출" → "[HIGH 풀] ✓ 도달" 로그
- `userId=1003` 알림 → "[NORMAL 풀] ✗ 무효 토큰" → 비활성화
- 다음 번 `userId=1003` 알림 → "활성 디바이스 없음 → SKIP"

### Phase 4 — 재시도 + DLQ

- [ ] `RetryableApnsException` 정의
- [ ] `MockApnsClient`의 RETRYABLE_FAILURE 응답 시 worker가 예외 throw
- [ ] 각 `@KafkaListener`에 `@RetryableTopic` 부착
    - `attempts = "5"`, exponential backoff + jitter, `dltTopicSuffix = ".dlt"`
- [ ] `@DltHandler` 메서드 추가 — 에러 로그 + (선택) Slack/이메일 모킹

**검증**:
- 알림 10건 일괄 발송 → 일부는 "⟳ 재시도 가능 실패" 로그 후 backoff
- 재시도 후 성공/실패 흐름 관찰
- `kafka-topics.sh --list`로 자동 생성된 `push.high-retry-0`, `push.high.dlt` 확인
- 끝까지 실패한 메시지 → `[DLQ] 최종 실패` 로그

### Phase 5 — 벌크 발송 + 우선순위 격리 시연

- [ ] `/api/notifications/bulk?count=N&priority=LOW` 엔드포인트
- [ ] 한 번에 N건 발송하지만 일부러 1초 간격 등 분산 발행은 *생략* (작은 규모니까)

**검증 (핵심 시연)**:
- 터미널 1: `count=30&priority=LOW` 발송
- 터미널 2: 동시에 HIGH 알림 1건 발송
- 로그: BULK 풀(워커 1개)이 천천히 처리되는 동안 HIGH 풀이 즉시 처리되는 것 확인 → **우선순위 격리가 실제로 동작**한다는 증거

### Phase 6 (선택) — Outbox 패턴 + 모니터링

이 단계는 데모 본체와 분리. 시간이 남으면.

- [ ] 가상의 `PaymentService`가 `@Transactional` 안에서 `outbox_event` 테이블에 기록
- [ ] 별도 publisher가 Outbox를 폴링해 Kafka로 발행
- [ ] PostgreSQL을 docker-compose에 추가
- [ ] Micrometer + Prometheus + Grafana로 도달률/지연/DLQ 적재량 노출

## 검증 시나리오 모음 (Phase 5까지 완료 후)

각 시나리오는 curl + 로그로 확인. README의 별도 섹션 또는 `scripts/` 폴더에 정리.

| # | 시나리오 | 기대 동작 |
| --- | --- | --- |
| 1 | HIGH 결제 알림 1건 | API → Orchestrator → push.high → HIGH 풀 → ✓ 도달 |
| 2 | 같은 idempotencyKey로 재호출 | "duplicate" 응답, 큐 발행 안 됨 |
| 3 | `userId=1002`(푸시 OFF) + NORMAL | Orchestrator에서 SKIP |
| 4 | `userId=1002`(푸시 OFF) + CRITICAL | DND/설정 무시하고 발송 |
| 5 | `userId=1003`(무효 토큰) | APNs 호출 후 BadDeviceToken → 토큰 비활성화 |
| 6 | 5번 직후 같은 사용자에게 또 발송 | "활성 디바이스 없음 → SKIP" |
| 7 | 알림 10건 일괄 발송 | 일부에서 자연스럽게 재시도 발생, 일부는 DLQ |
| 8 | BULK 30건 + 동시에 HIGH 1건 | HIGH가 BULK를 건너뛰고 먼저 도달 (격리 시연) |

## 의도적으로 단순화한 부분

블로그 글에는 있지만 이 데모에서는 뺀 것들. 면접에서 *"프로덕션으로 가져가면 뭐가 더 필요한가?"* 질문 대비용 메모.

- **DB 없음** — 사용자 설정/디바이스 토큰을 인메모리로. 실제로는 PostgreSQL + Redis 캐시
- **Outbox 패턴 미포함** — 외부 비즈니스 서비스(PaymentService 등)를 흉내내지 않으므로 생략
- **In-App 다중 채널 폴백 미포함** — Push 단일 채널만 시연
- **HTTP/2 커넥션 풀 관리 미포함** — Mock 클라이언트는 단순 메서드 호출
- **모니터링 메트릭 미포함** — Micrometer + Prometheus 미연결
- **벌크 발송 시 청크 분할 + 시간 분산 발행 미포함** — 데모 규모가 작아서 단순 루프
- **JWT 인증, Rate Limit, API Gateway 미포함** — 인프라 관심사가 아님

## 실행 (구현 완료 후)

```bash
docker compose up --build
```

테스트는 README의 검증 시나리오 표를 따라 `curl`로.

## 참고

- 블로그 글: [APNs에 던지면 끝일까 — 대용량 iOS Push 알림 시스템 설계](#)
- [Spring Kafka @RetryableTopic 문서](https://docs.spring.io/spring-kafka/reference/retrytopic.html)
- [APNs Provider API](https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server)
- [bitnami/kafka Docker 이미지 (KRaft 모드)](https://hub.docker.com/r/bitnami/kafka)

## 진행 상황

- [ ] Phase 0 — 환경 셋업
- [ ] Phase 1 — Notification API + 멱등성
- [ ] Phase 2 — Orchestrator
- [ ] Phase 3 — APNs Worker
- [ ] Phase 4 — 재시도 + DLQ
- [ ] Phase 5 — 벌크 발송 + 우선순위 격리 시연
- [ ] (선택) Phase 6 — Outbox + 모니터링
