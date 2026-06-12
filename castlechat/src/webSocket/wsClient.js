// < WEBSOCKET MODULE SCOPE >

// ====== 1. module state =========================================================================================================
let ws = null; // wsRef.current대체.  “전역 변수”처럼 보이지만, 정확히는 모듈 스코프 변수임. 이 파일 안에서만 직접 접근 가능하고, 밖에서는 함수로만 접근함.
let isConnected = false; // isWsConnectedRef 대체.
let isManualDisconnect = false;
const roomHandlers = {}; // 방별 이벤트 처리 함수 보관소야.

// ====== 2. constants =========================================================================================================
const WS_TYPES = {
    CONNECT_USER: "CONNECT_USER",
    ENTER_ROOM: "ENTER_ROOM",
    EXIT_ROOM: "EXIT_ROOM",
    SEND_MSG: "SEND_MSG",
    READ_MSG: "READ_MSG",
    TYPING_START: "TYPING_START",
    TYPING_STOP: "TYPING_STOP"
};

// 왜 appshell에서는 useRef를 쓴거야? 여기서는 단순히 let 변수로써 단순한 구조로 되는데?
// --> React 컴포넌트 안에서는 let 변수가 렌더링 때마다 '초기화'되기 때문에 useRef를 쓴다. React 모듈 바깥에서는 렌더링이 없기 때문에 let 변수가 유지된다.
// 즉, .jsx에서는 매 rendering마다 let ws = null; 이 되버린다. useRef는 rendering되어도 객체가 유지된다. & useRef값이 바뀌어도 reRendering 하지않음.

// current는 내장 method가 아님. useRef가 반환하는 객체 안에 들어있는 property의 key이다. --->>> key : current , value : null 인 것이다. 
// useRef(null)을 하면 React가 대충 이런 객체를 줌. --->>> const wsRef = { current : null };
// 그래서, const wsRef = useRef(null); --->>> wsRef.current === null 인거임.
// 왜 굳이 .current가 있냐? useRef는 렌더링 사이에서도 같은 객체를 유지함. 
// --> React는 이 wsRef 객체 자체를 계속 같은 걸로 유지해줘. 그리고 우리는 그 안의 current 값만 바꿔.

// ====== 3. func =========================================================================================================
export function emitWs(wsType, payload = {}) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        console.log('WS 연결 안 됨');
        return false;
    }

    ws.send(JSON.stringify({
        requestId: crypto.randomUUID(),
        wsType: wsType,
        payload: payload
    }));
    return true;
}

export function connectWs() {
    // 이미 연결되어 있거나 연결 중이면 새 WebSocket을 또 만들지 말라는 방어 코드
    // ws 객체가 이미 있고 && 그 ws가 OPEN 또는 CONNECTING 상태라면 ---> connectWs 함수를 그냥 끝내라. return.
    // React 렌더링이나 개발모드 StrictMode 때문에 connectWs()가 여러 번 호출될 수 있음.
    // connectWs() -> new WebSocket 생성 -> connectWs() -> new WebSocket 생성 -> connectWs() -> new WebSocket 생성 ==> 여러개의 연결이 생성됨.
    // 그러면 서버에서는, 같은 유저가 WS 2개, 3개 연결됨. 메시지 중복 수신. roomSessions,onclose 꼬임
    // WebSocket.CONNECTING/OPEN/CLOSING/CLOSED  --->>  0:연결 시도 중,  1:연결 완료,  2:닫히는 중,  3:닫힘
    // connectWs()의 목적은 “무조건 새로 연결”이 아니라 --> 연결이 없으면 연결한다. 만약 이미 있다면? 아무것도 안 한다.
    // 이런 함수로 만드는 게 좋아. 이런 함수를 보통 idempotent하다고 해. 여러 번 호출해도 결과가 이상하게 중복되지 않는다는 뜻이야.
    // idempotence :동일한 요청을 서버에 한 번 보내는 것과 실수로 여러 번 연속으로 보내는 것이 동일한 결과를 냅니다. 네트워크 오류 등으로 요청이 중복 처리되어도 데이터가 중복 생성되지 않아야 할 때 필수적입니다.

    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        return;
    }

    isManualDisconnect = false; // 수동 종료 직후 재연결 같은 흐름에서 플래그가 애매하게 남는 걸 막기 위해서야.

    // const ws = new WebSocket(~) : AppShell에서와 다르게 const가 안 되는 이유? 
    // 핵심은 스코프 차이야. AppShell에서는 useEffect 한 번 실행 안에서만 쓰니까 const로 충분.
    // 그런데 .js 모듈에서는 여러 함수가 같은 WS 객체를 공유해야 함. 이 함수들이 모두 같은 ws를 봐야 해. 그래서 바깥에 이게 있어야 함.
    // 즉 const ws = ...가 아니라 바깥의 let ws에 재할당해야 함.
    // 이건 “위에서 아래로 한 번 실행되니까 안다”가 아니야. 
    // 함수가 만들어질 때 자기 바깥 스코프를 기억한다는 JS의 성질 때문이야. 이걸 closure라고 해. connectWs와 sendChatWs는 서로 독립적인 함수 맞음.
    ws = new WebSocket('ws://localhost:8080/ws/chat'); // --> 이때!! 같은 모듈 변수 ws가 바뀜. 그러면 sendChatWs가 기억하고 있던 같은 ws를 봄.
    // 즉, 이미 WebSocket 객체가 들어 있음. 함수끼리 서로 아는 게 아니다. 함수들이 같은 모듈 스코프 변수를 닫아두고 참조한다. 이게 closure다.

    ws.onopen = () => {
        isConnected = true;

        console.log('CONNECT_WEBSOCKET_OK');

        // sendWs({
        //     requestId: crypto.randomUUID(),
        //     wsType: 'CONNECT_USER',
        //     payload: {}
        // });
        // 이렇게 보내면 gg..오류나버린다. 아래와 같은.
        //com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
        //  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 62] (through reference chain: com.chat.castledragon.domain.WebSocketDTO["wsType"])

        // sendWs('CONNECT_USER', {}); // 이게 맞다.
        emitWsConnectUser(); // 리팩토링 버전.
    };

    ws.onmessage = (evt) => {
        const wsEvt = JSON.parse(evt.data); // evt.data는 서버가 보낸 JSON 문자열이야.
        console.log('ws 수신', wsEvt);

        switch (wsEvt.wsType) {
            case 'CONNECT_USER_OK':
                console.log('CONNECT_USER_OK');
                break;

            case 'ENTER_ROOM_OK':
                console.log('ENTER_ROOM_OK');
                break;

            case 'MSG_CREATED':
            case 'MSG_READ':
            case 'TYPING_START':
            case 'TYPING_STOP': {
                const roomId = wsEvt.payload?.roomId;

                if (roomId == null) { // roomId가 없으면 roomHandlers[undefined]를 보게 됨. 큰 문제는 아니지만, 명시적으로 막으면 더 좋음.
                    return;
                }

                roomHandlers[roomId]?.(wsEvt);
                break;
            }

            default:
                break;
        }
    };

    ws.onclose = (evt) => {
        if (isManualDisconnect) {

            console.log('WebSocket 수동 종료', {
                code: evt.code,
                reason: evt.reason,
                wasClean: evt.wasClean
            });

            isManualDisconnect = false;

        } else {
            console.log('WebSocket 비정상/외부 종료', {
                code: evt.code,
                reason: evt.reason,
                wasClean: evt.wasClean
            });
        }

        isConnected = false;

        // 현재 살아있는 최신 ws를 실수로 지우지 않기 위해서, 아래의 조건을 둔거다. 닫힌 소켓이 현재 모듈이 들고 있는 ws와 같은 소켓일 때만 null 처리.
        // ws가 evt.target이 아니면? === 닫힌 건 예전 소켓이고, 현재 ws는 이미 다른 새 소켓이다. 그래서! 비우면 안 됨.
        // ws !== evt.target이라면? === 현재 ws는 다른 최신 연결일 가능성이 있다. --> 그래서, 유지해야만 한다.
        if (ws === evt.target) {
            ws = null;
        }

    };

    ws.onerror = (evt) => {
        console.error('WebSocket 오류', evt);
    };
}

// ws가 있으면 close 요청. 모듈 변수 ws를 null로 변경. isConnected false 처리
export function disconnectWs(action) {
    const targetWs = ws;

    isManualDisconnect = true;
    isConnected = false;
    ws = null;

    Object.keys(roomHandlers).forEach((roomId) => {
        delete roomHandlers[roomId];
    });

    if (targetWs && (targetWs.readyState === WebSocket.OPEN || targetWs.readyState === WebSocket.CONNECTING)) {
        targetWs.close(1000, action);
    }

}//disconnectWs

// export function sendWs_legacy(data) { //현재의 emitWs legacy버전임.
//     if (!ws || ws.readyState !== WebSocket.OPEN) {
//         console.log('WS 연결 안 됨');
//         return false;
//     }

//     ws.send(JSON.stringify(data));
//     return true;
// }

export function isWsConnected() {
    return isConnected && ws?.readyState === WebSocket.OPEN;
}

export function registerRoomHandler(roomId, handler) {
    roomHandlers[roomId] = handler;
}

export function unregisterRoomHandler(roomId) {
    delete roomHandlers[roomId];
}

export function emitWsConnectUser() {
    return emitWs(WS_TYPES.CONNECT_USER);
}

export function emitWsEnterRoom(roomId) {
    return emitWs(WS_TYPES.ENTER_ROOM, { roomId: roomId }); // WS_TYPES.ENTER_ROOM은 그냥 객체 안에 저장해둔 문자열을 꺼내 쓰는 것이야. 
    // Back-end에서 ENTER_ROOM wsType 처리를 ENTER_CHAT_ROOM_NOW로 변경한다고 했을때, 
    // 위의 WS_TYPES안의 ENTER_ROOM:"ENTER_ROOM"에서 "ENTER_ROOM" --> "ENTER_CHAT_ROOM_NOW"로 바꿔주기만 하면됨.
    // Java에서 상수 쓰는 거랑 비슷해. public static final String ENTER_ROOM = "ENTER_ROOM"; JS에서는 객체로 상수 모음집을 만든 거라고 보면 돼.
}

export function emitWsExitRoom(roomId) {
    return emitWs(WS_TYPES.EXIT_ROOM, { roomId: roomId });
}

export function emitWsSendMessage(roomId, messageText) {
    return emitWs(WS_TYPES.SEND_MSG, {
        roomId: roomId,
        messageText: messageText
    });
}

export function emitWsReadMessage(roomId, lastReadMessageId) {
    return emitWs(WS_TYPES.READ_MSG, {
        roomId: roomId,
        lastReadMessageId: lastReadMessageId // lastOtherMsgInRoom전체를 보내면, 너무 커지고 책임도 이상해짐. payload가 두꺼워져.
    });
}

export function emitWsTypingStart(roomId) {
    return emitWs(WS_TYPES.TYPING_START, { roomId: roomId });
}

export function emitWsTypingStop(roomId) {
    return emitWs(WS_TYPES.TYPING_STOP, { roomId: roomId });
}

// ws 객체 대략 구조
// WebSocket {
//     binaryType: "blob",
//     bufferedAmount: 0,
//     extensions: "",
//     onclose: ƒ,
//     onerror: ƒ,
//     onmessage: ƒ,
//     onopen: ƒ,
//     protocol: "",
//     readyState: 1,
//     url: "ws://localhost:8080/ws/chat"
// }