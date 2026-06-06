import './Home.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';
import { useNavigate, Navigate } from 'react-router-dom'; //  Navigate : “화면 렌더링 중 조건에 따라 다른 주소로 보내는 컴포넌트”야.

import { useMe } from '../../hooks/useMe';
import { useLogout } from '../../hooks/useLogout';


function Home() {
    const navigator = useNavigate();

    const { data: me, isLoading: isCheckingLogin } = useMe();

    const logoutMutation = useLogout();
    const isWsConnectedRef = useRef(false);
    const wsRef = useRef(null);

    const roomHandlersRef = useRef({});

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        // if (!me) return;

        // 만약 new Ws를 바깥으로 뺀다면? --> React 생명주기랑 충돌해서 터짐. 컴포넌트 랜더링 될때마다 연결함.
        const webSocket = new WebSocket(`ws://localhost:8080/ws/chat`); // roomId=${roomId}&userId=${userID} 삭제. query string --> ENTER 이벤트송신으로 변경.
        wsRef.current = webSocket;
        // onopen = FUNCTION_NAME 식으로 function저장을 해도 되지만,,,? 어차피 onopen때 딱 한!번! 쓰고 말것이기 때문에 굳이 바깥으로 function으로 빼지 않는다.

        webSocket.onopen = async () => { // async라서 useEffect안쪽에 callback함수 못 넣는다. useEffect는 cleaup function을 return해야 할수도있다. 바깥으로 빼면, parameter전달필요 , stale closure 위험, 의존성 증가 등이 생김.
            // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen에 저장된 함수가 "자동으로 실행된다" (o). 현재는 익명함수
            // wsRef.current = webSocket; // 연결후에 집어넣을 경우, onopen전에 sendMsg할수도있어서 위험함. 그래서 new Ws하자마자 위에서 바로 ㄱㄱ.

            isWsConnectedRef.current = true; // 연결 상태 false --> true

            console.log(`webSocket연결 완료.`);

            // wsRef.current.send(JSON.stringify({
            webSocket.send(JSON.stringify({
                requestId: crypto.randomUUID(),
                wsType: "CONNECT_USER",
                payload: {
                    // session정보를 신뢰하고, f-e에서는 useMe로 로그인정보 관리하므로, 굳이 보낼 필요 없다. 
                }
            }))

        }

        // ====== ws수신시 처리 ==============================================================================================
        webSocket.onmessage = (evt) => {
            const wsEvt = JSON.parse(evt.data);
            console.log(`ws 수신`, wsEvt);

            switch (wsEvt.wsType) {
                case "CONNECT_USER_OK":
                    console.log(`CONNECT_USER_OK`);
                    break;


                case "ENTER_ROOM_OK":
                    console.log(`ENTER_ROOM_OK`);
                    break;

                case "MSG_CREATED": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    console.log(`MSG_CREATED`);
                    break;
                }

                case "MSG_READ": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    console.log(`MSG_READ`);
                    break;
                }

                case "TYPING_START":
                case "TYPING_STOP": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    break;
                }

                default:
                    // alert(`알수없는 타입`);
                    break;
            }

        }

        webSocket.onclose = (evt) => {
            console.log('WebSocket 종료', {
                code: evt.code,
                reason: evt.reason,
                wasClean: evt.wasClean
            });

            isWsConnectedRef.current = false;

            if (wsRef.current === webSocket) {
                wsRef.current = null;
            }
        };

        webSocket.onerror = (evt) => {
            console.error('WebSocket 오류', evt);
        };

        return () => {
            webSocket.close();
        }
    }, [me])

    // ===== 로그아웃 ===========================================================================================
    function logout() {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
            console.log(`로그아웃 및 ws 연결종료`);
        }

        isWsConnectedRef.current = false;

        logoutMutation.mutate(null, {
            onSuccess: () => {
                navigator('/login');
            }
        });
    }

    // ======< return >=======================================================================================================
    return (
        <div className='HomeContainer'>

            {/**============== 로그인 구역==================== */}
            <div className='loginSection'>
                {me ?
                    <>
                        <div>{me.profileImg ? me.profileImg : '프사 없음'}</div>
                        <div className='loginForm'> {me.nickname} 님 안녕하세요.</div>
                        <div>{me.publicId}</div>
                        <div>{me.friendCode}</div>
                        <button onClick={() => logout()}>로그아웃</button>
                    </>
                    :
                    <>
                        <div className='loginForm'>
                            <div>오류</div>
                        </div>
                    </>
                }

            </div>

        </div >
    );
}

export default Home;