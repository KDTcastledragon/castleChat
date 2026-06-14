import './ChatList.css';
// import { useState } from 'react';
import { useMe } from '../../hooks/useAuthUser';
import { useChatRoomActions, useGetMyAllRooms } from '../../hooks/useChatRoom';

function ChatList() {
    // const { data: myAllRooms = [], refetch: refetchMyAllRooms } = useGetMyAllRooms(!!me);
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: myAllRooms = [], isLoading, isError } = useGetMyAllRooms(!!me);

    const { enterExistingRoom } = useChatRoomActions();

    if (isCheckingLogin || isLoading) {
        return <div className='ChatListContainer'>채팅방 목록 불러오는 중...</div>;
    }

    if (!me) {
        return <div className='ChatListContainer'>로그인이 필요합니다.</div>;
    }

    if (isError) {
        return <div className='ChatListContainer'>채팅방 목록 불러오기 실패</div>;
    }


    // ======< return >=======================================================================================================
    return (
        <div className='ChatListContainer'>
            <div className='chatList'>
                <div className='chatListTitle'><span>채팅방 목록</span></div>
                {myAllRooms.map((r) => (
                    <div className='chatListBox' key={r.roomId}>
                        <span>{r.roomId}-</span>
                        <span>{r.roomType}</span>
                        <span>/</span>&nbsp;
                        <span>{r.customRoomName}</span>&nbsp;&nbsp;&nbsp;
                        <span>{r.roomType === 'DIRECT' ? null : '/'}</span>&nbsp;&nbsp;
                        <span>{r.roomType === 'DIRECT' ? null : `${r.activeMemberCount}명`}</span>
                        &nbsp;&nbsp;&nbsp;&nbsp;
                        <button onClick={() => enterExistingRoom(r.roomId)}>채팅</button>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default ChatList;