# 사용자 수에 따른 규모 확장성
## 단일 서버
웹서버를 통해 사용자가`HTML`을 받아 렌더링한다.
1. 웹서버에 요청하기 위해 `도메인주소`를 `DNS`를 통해 IP 주소로 전환한다.
2. 해당 IP주소를 통해 네트워크로 요청한다.
3. CDN이 있다면, CDN을 통해서 혹은 nginx등 캐싱이 돼있다면 해당 웹서버로부터 HTML을 반환받는다.
## 데이터베이스
사용자가 늘어난다면, 인증 인가 등 다양한 사용자의 정보를 저장하고 상호작용해야한다. 따라서 데이터베이스에 각 정보를 저장해야한다.
이 때, 기존 HTML 혹은 JSON을 반환하던 웹서버와 별개로 데이터베이스를 담당하는 서버가 더 필요하다.
### RDB, 비관계형 데이터베이스
비 관계형 데이터베이스는
- 낮은 지연시간
- 비정형 데이터
- 복잡한 쿼리 없이 통으로 직렬화, 역직렬화
- 대량의 데이터 저장 (관계형은 대량 저장 시 성능저하가 발생한다.)
## 수직 확장 vs 수평 확장
`수직 확장`은 단순히 서버의 사양을 업그레이드 하는 것이며 단순하다는 장점이 있다.
`수평 확장`은 서버를 여러 대 마련해 부하를 분산하는 것이며 장애에 유연하고 수직확장에 비해 한계점이 없다.
### 수평확장
#### 로드밸런서
로드밸런서는 요청을 받는 앞단에 트래픽을 처리하는 역할을 수행한다. 사용자는 해당 로드밸런서의 도메인, IP에 요청하며 로드밸런서는 라운드로빈, 해싱(채팅 스틱키 세션 활용시)등을 통해 적절한 웹서버로 전달한다.
해당 방식의 장점은 서버가 여러대일 경우 한 대가 사용불가더라도 다른 곳으로 전달이 가능하다는 것이다.
#### 디비 다중화
관계형 데이터베이스 다중화는 읽기 요청이 대다수인 서비스들에서 거의 필수적이다. 쓰기는 master 한 곳에서만 가능하나 slave들에게서 조회함으로써 병렬로 조회 요청을 처리하는 것이 가능하다.
## 캐시
응답시간을 개선하기 위해 사용한다.
#### 만료주기 및 일관성
- 만료주기는 너무 길면 데이터의 일관성을 해친다.
- 에빅션 정책은 LRU, LFU, FIFO 중 적절한 것을 택해야한다.
#### SPOF
- 캐시에서 장애가 발생하면 캐시 스탬피드가 발생할 수 있다. 갱신 전까지 데이터베이스에 부하가 걸릴 수 있으므로, 락을 활용해 제어해야한다.


1. read look aside 전략
    - 데이터 조회 시 캐시를 먼저 조회한다.
    - 없다면 디비를 조회해 캐시를 갱신한다.
2. read through
    - 데이터 조회 시 무조건 캐시만 조회한다.
3. write back
    - 캐시에 데이터를 임시저장하고 배치작업으로 실제 작업을 수행해 데이터베이스에 반영
    - 쿠폰발급! 처럼 대규모 쓰기 작업에 사용
4. write through
    - 쓰기 요청에서 캐시와 디비 모두 갱신하는 방법
## CDN
데이터 센터에 정적 데이터를 캐싱한다.
만료시한을 잘 정해 히트율을 높이고 괴리율을 낮추자.
프론트엔드 정적 코드를 대부분 CDN으로 제공한다.
대표적으로 vercel, cloudflare
## 무상태 웹 계층
서버의 수평 확장을 고려할 때 가장 먼저 고려해야할 것이 상태관리다.
상태는 세션으로 주로 관리하나 이를 서버 인메모리에 저장하게되면
다른 서버로 요청이 갔을 때 처리가 불가능하다.

- 로드밸런서 sticky session을 활용해 처리
    - 로드밸런서는 대부분 비동기로 동작하므로 해당 방식으로 해쉬값을 얻기 위해 파싱하는 비용이 모든 요청에 영향을 준다.
    - SPOF가 될 수 있으므로 최대한 자제
- 외부 저장소에 세션을 저장한다 -> 대부분 채택, 매 요청마다 외부요청을 해야한다는 것이 단점
- 토큰으로 처리한다. -> 대부분 채택, 외부요청을 하지않아도 되나 클라이언트에 저장되기 때문에 실시간 반영이 어렵다는 단점이 있다.
## 데이터 센터
수평적확장을 했을 때, 어느 지역에 제공할 것인지도 중요하다.
지리적위치를 이용해 GeoDNS로 가까운 데이터센터로 유도되도록
로드밸런서를 설정해 사용할 수 있다.
## 메세지 큐
여기서 설명하는 메세지큐는 레디스 펍섭이나 래빗엠큐를 말하지 않는다.
메세지 브로커인 카프카, SQS 만을 말하는 것이므로 착각하면 안된다.
메세지큐는 CPU Intensive한 작업을 분리해 빠른 응답을 내릴 수 있도록 해준다.
멀티 스레딩으로 해도 되긴 하지만, 차이점은 각 요청을 쌓아두고 여유가 생겼을 때 가져올 수 있기 때문에
요청 대기로 인한 손실 및 부하에서 자유롭다. 또한 구독자 프로세스를 늘려 처리량을 조절하는 것도 가능하다.
rate limit을 현재 여유 자원으로 가능한 만큼 동적으로 관리할 수 있는 것.

## 로그, 메트릭 그리고 자동화
로그는 예외나 오류가 발생했을 때 원인 추적을 빠르게 할 수 있도록 돕는다.
메트릭은 다양한 지표를 통해 어떤 점을 개선해야하는지 사업 운영에 필요한 정보를 얻을 수도 있고
현재 시스템 상태를 파악하고 피해를 사전에 인지하고 예방할 수도 있다.

## 데이터베이스 확장
서버와 동일하다. 차이점은 데이터베이스는 상태를 관리하는 주체이므로, 수평적 확장 시 샤딩을 통해 스틱키 세션처럼 해싱하는 과정이 필요하다. 따라서 링크드해시테이블을 설계할 때처럼 균일하게 배정될 수 있도록 해시를 지정하는 것이 중요하다.
데이터가 너무 많이 쌓여 샤드 소진이 발생해 재 샤딩을 할 때 안정해시 기법을 사용해 재배치 해야한다.
유명인사 문제: 핫스팟 키 문제라고해서 특정 데이터 조회, 쓰기가 너무 많이 발생하여 특정 샤드에 부하가 걸리는 상황이다. 서적에서는 유명인사의 경우 수동으로 재배치하여 해결하도록 소개하지만 해당 문제는 데이터 중심 애플리케이션 설계에서 소개된 Pan-out 기법으로 캐시를 통해 해결할 수 있다.
조인, 비정규화: 샤드를 쪼갰을 때 조인하기 어려워지는 상황이 발생한다. 이는 조인이 필요한 케이스의 경우 Pan out 기법으로 반정규화테이블을 별도로 관리하는 방식으로 해소할 수 있다.

## 대규모 트래픽
위와 같은 방식으로 대규모 트래픽을 받기위해 무한 반복하면 된다.
# 개략적인 규모 추정

## 가용성에 관계된 수치들
가정
추정에 사용할 조건
추정
트래픽: QPS, 최대 QPS
공간복잡도: 저장소, 캐시, 서버 수

근사치로 구한다.
단위를 붙여라.
# 시스템 설계 면접 공략법
긍정적
- 협업에 적합한지
- 압박이 심해도 괜찮은지
- 모호한 문제를 재정의할 수 있는지
- 좋은 질문으로 문제해결에 필요한 요소를 찾을 수 있는지
  부정적
- 설계 순수성 집착 -> 오버엔지니어링
- 시스템 유지 비용 상승
- 완고함, 편협함으로 협업에 부정적 인상
## 4단계 접근법
1. 문제 이해 및 설계 범위 확정 - 3분에서 10분
    1. 무조건 빠르게 대답하지 않기.
    2. 올바른 가정을 하기위해 좋은 질문을 생각할 것
        1. 구체적인 기능적 요구사항
        2. 비기능적 요구사항 (트래픽)
        3. 회사 성장 속도 -> 3, 6, 12개월 뒤
        4. 기술 스택, 단순화하기위해 사용하고 있는 인프라
    3. 예시
        1. 지원 플랫폼 (클라이언트 종류 파악)
        2. 요구사항 질문
        3. 요구사항에 대한 정책 질문
        4. 사용자별로 예상되는 트래픽을 추정하기 위해 행위와 연관된 대상의 양 질문
        5. 구체적인 현재 트래픽 질문
        6. 저장소 요구를 파악하기 위한 요구사항 질문
2. 개략적 설계안 제시 및 동의 구하기 - 10분에서 15분
    1. 과정
        1. 청사진을 그리고 피드백 구하기.
        2. 클라이언트 및 인프라 요소 그리기
        3. 각 인프라 요소들이 1번에서 추정한 요구를 만족하는지 근사치 구하며 설명하기.
        4. 단일 기능에 대한 설계가 아니면 API 엔드포인트와 스키마도 작성할지 피드백 받기
        5. 구체적 사용사례 질문을 통해 에지케이스 발견하기
    2. 뉴스피드 시스템 설계 예시
        1. 피드 발행
            1. 포스팅을 캐시 및 데이터베이스에 추가한다.
            2. 친구들의 뉴스피드 캐시에 PAN OUT으로 반영한다.
            3. 알림 서비스로 알림을 발행한다.
        2. 피드 생성 (조회 오역인듯?)
            1. 뉴스피드 캐시에서 조회 후 정렬 및 조회해서 반환한다.
3. 상세 설계 - 10분에서 25분
    1. 과정
        1. 컴포넌트 우선순위 정하기.
        2. 병목구간, 자원요구량을 고려하며 컴포넌트 세부사항 설명
            1. 단축 URL 기능이라면 해시함수의 설계
            2. 채팅이라면 지연시간 감소 및 온라인 상태표시 설계
        3. 너무 자세하게는 설명하지 않는다. 네임드 항목에 대해선 이름만 언급.
    2. 예제
        1. 피드 발행
            1. 뉴스피드 캐시 등록 시 그래프DB로 관계도를 통해 뉴스피드를 추가할 친구목록을 조회한다.
            2. 사용자 정보 캐시에서 친구 정보가 유효한지 검증한다.
            3. 메세지 큐를 통해 캐시 등록은 비동기로 처리한다. (대상 유저가 많기 때문)
        2. 뉴스 피드 조회 (오역 맞았었네)
            1. 뉴스 피드 서비스(퍼사드)에서 사용자, 포스팅, 뉴스피드 캐시를 조회해 앱 조인 후 반환한다.

4. 마무리 - 3분에서 5분
    1. 과정
        1. 개선점을 말해보라고 할 것이다. 잘 고민해서 트레이드 오프를 설명하기.
        2. 요약해주기
        3. 장애요인 분석 및 대응 설명
        4. 운영을 위한 로그, 메트릭 수집방법, 각 컴포넌트 CI/CD 전략
        5. 규모확장시 방안
        6. 아까 네임드 컴포넌트 설명하기
5. 해야할 것
    1. 가정을 끊임없이 질문하기
    2. 요구사항 이해하기
    3. 트래픽마다 좋은 설계는 다를 수 있으므로 주의
    4. 적극적인 소통
    5. 여러 해법 제시하기
    6. 다 넘기고 시간이 남으면 컴포넌트의 세부사항 설명하기
    7. 면접관의 아이디어 이끌어내기
6. 하지말 것
    1. 기출점검하기
    2. 요구사항 파악과 가정을 완료하기 전에 설계하지 말기
    3. 처음에 세부사항 설명하지 않기
    4. 힌트를 청하기
    5. 피드백을 항상. 구하기
7. 시간분배
    1. 문제 이해 및 설계 범위 확정: 3분에서 10분
    2. 개략적 설계안 제시 및 동의 구하기: 10분에서 15분
    3. 상세 설계: 10분에서 25분
    4. 마무리: 3분에서 5분



