< castleChat 채팅웹 프로그램 기획 문서 >

A. priority principle (===prpr) [이 prpr은 프로젝트의 모든 곳에서 철저히 검증하고 지켜야한다.]

1. 항상 대용량 트래픽에 충분히 대응 가능하면서 실무적 관점에 맞는 설계를 해야한다. 니가 반드시 지켜야할 원칙이자 이 프로젝트의 최우선 목표다.
2. Client의 UX를 "우선"으로 한다. 이를 위해서, Redis와 kafka 비동기 처리를 적극 활용한다.   
3. sendMessage는 channel-engine process의 @Service에서, DB insert후 response하지 않고, kafka에서 durable save후에 response한다. DB insert는 kafka에서 "비동기"로 묶어서 처리한다.
4. readMessage는 절대로 DB에 대한 CRUD행위를 "동기"로 처리하지 않는다.(client의 read 요청:db select/update 행위 = 1:n(n>=1자연수) 구조가 되면 절대로 안된다.) Redis를 적극적으로 활용하며, updateLastReadMessageId(===lrm) 은 반드시 batch로 "비동기" 처리한다.(현재 dirty flush worker 사용중.)
5. variable , constant , function , method 등의 naming은 "무조건" 나의 스타일에 맞춰서 한다. 니 마음대로 naming 하지 말 것.

B. 구현해야 할 기능
1. sendMessage : websocket(===ws) 기반 구현. db insert 는 kafka로 비동기처리. channel-engine(===cheg)이 response하는 시점은, db insert후가 아닌, kafka에서 durable save 후다.

2. readMessage : ws기반 구현. read처리도 비동기로 한다. cheg.service->reids-> cheg.worker의 dirty flush worker가 비동기로 처리함.
lrm기반으로 Front-end(===fe)에서 unreadCount(===urc. 해당 Message를 읽지 않은 사람수)를 계산하며, urc는 db에 저장하지 않는다. 실제 fe의 urc계산은 oldLrm과 lrm(최신Lrm의미)으로 계산함.

3. deleteMessage : 