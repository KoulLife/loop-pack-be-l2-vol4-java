# Round 8 — Redis 대기열 설계 노트

## 아키텍처 개요

```
[유저] --enter--> (Redis Sorted Set: queue:waiting)
                        │
                (QueueAdmissionScheduler / 주기적으로 N명 popMin)
                        │
                        ▼
             (Redis String: queue:admission:{userId}, TTL 5분)  = 입장 토큰
                        │
   [유저] --X-Queue-Token 헤더--> POST /api/v1/orders
                        │  (QueueTokenInterceptor 검증)
                        ▼
                  주문 완료 → 토큰 삭제(소진)
```

- **대기열 진입**: `ZADD NX`(`addIfAbsent`)로 순서 보장 + userId 중복 진입 방지. score는 `INCR` 시퀀스로 부여해 동시 진입 시에도 순서가 흔들리지 않는다(밀리초 timestamp 대비 안전).
- **순번 조회**: `ZRANK`(0-based) + 1 = 순번. 전체 대기 인원 = `ZCARD`.
- **입장**: 스케줄러가 `ZPOPMIN`으로 앞에서 N명을 꺼내 입장 토큰(UUID, TTL)을 발급. 큐에서 제거되므로 이후 순번 조회는 토큰을 반환(순번 0).
- **주문 게이트**: `POST /api/v1/orders`에서만 `X-Queue-Token`을 검증. 조회(GET)는 검증 대상이 아니다. 주문이 200으로 완료되면 토큰을 삭제한다.

## 스케줄러 배치 크기 산정 근거

설정: `queue.admission.batch-size` (기본 100), `queue.admission.interval-ms` (기본 1000).

| 항목 | 값 | 출처 |
|------|-----|------|
| DB 메인 커넥션 풀 (max) | 40 | `modules/jpa/src/main/resources/jpa.yml` (`mysql-main-pool`) |
| 주문 생성 1건 평균 처리 시간(가정) | ~200ms | 상품 비관적 락(`findWithLock`) → 재고 차감 → 주문/아웃박스 저장이 단일 트랜잭션에서 커넥션 1개를 점유 |
| 다른 트래픽용 예약 커넥션 | ~10 | 유저/상품/결제 API가 같은 풀을 공유 |
| 주문 처리 가용 커넥션 | ~30 | 40 − 10 |

- 커넥션 1개의 초당 처리량 = `1 / 0.2s = 5건/s`.
- 주문 가용 커넥션 30개 → 이론상 최대 `30 × 5 = 150건/s`.
- 스케줄러 실행 주기 = **1초** → 초당 입장 인원을 이 처리량 이하로 맞춰야 병목이 생기지 않는다.
- 락 경합·GC·트래픽 스파이크 여유를 위해 상한(150)의 약 66%인 **100명/초**를 기본 배치 크기로 채택.

즉 **초당 최대 100명**만 주문 API로 흘려보내, 그 뒤에 아무리 많은 요청이 쌓여도 DB 커넥션 풀이 고갈되지 않도록 처리량을 제어한다. 운영 환경의 실측 처리 시간·풀 크기에 따라 설정만으로 조정 가능하다.

## 예상 대기 시간 계산

```
예상 대기 시간(초) = ceil(순번 / batchSize) × (interval-ms / 1000)
```

- 앞으로 필요한 스케줄 실행 횟수 `ceil(순번 / 배치 크기)`에 실행 주기를 곱한다.
- 예) 순번 350, 배치 100, 주기 1s → `ceil(350/100) × 1 = 4초`.

## 입장 토큰 TTL

- 기본 5분(`queue.token.ttl-seconds=300`). 입장 후 주문을 마치기까지 충분한 시간을 주되, 미사용 토큰이 좌석을 오래 점유하지 못하도록 제한.
- TTL 초과 시 Redis가 자동 만료 → 주문 API 진입 시 `403 FORBIDDEN`.

## 주요 Redis 키

| 키 | 타입 | 용도 |
|----|------|------|
| `queue:waiting` | Sorted Set | 대기열 (member=userId, score=시퀀스) |
| `queue:waiting:sequence` | String(INCR) | 진입 순서 시퀀스 |
| `queue:admission:{userId}` | String (TTL) | 입장 토큰 |

> 모든 Redis 접근은 순번 정확도를 위해 `masterRedisTemplate`(master 노드)로 수행한다. replica lag으로 인한 부정확한 순번을 피하기 위함.
