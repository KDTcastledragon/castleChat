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
            {myAllRooms.map((r) => (
                <div key={r.roomId}>
                    <span>{r.customRoomName}</span>
                    <span>{r.roomType}</span>
                    <span>{r.roomMemberCount}</span>
                    <button onClick={() => enterExistingRoom(r)}>채팅</button>
                </div>
            ))}
        </div>
    );
}

export default ChatList;