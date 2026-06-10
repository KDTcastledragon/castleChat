import './FriendList.css';
import axios from 'axios';
import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';

import { useMe } from '../../hooks/useAuthUser';
import { useFriendList, useAddFriend, useReceivedFriendRequests, useRespondFriendRequest } from '../../hooks/useFriend';
import { useDebounce } from '../../hooks/useDebounce';
import { useSearchUsers } from '../../hooks/useSearchUsers';
import { useGetMyAllRooms } from '../../hooks/useGetMyAllRooms';

import { openChatWindow } from '../../store/chatWindowsSlice';
import { sendWs } from '../../webSocket/wsClient';


function FriendList() {
    const nav = useNavigate();
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: friendList = [] } = useFriendList(!!me);
    const [roomName, setRoomName] = useState('');
    const [selectedFriendList, setSelectedFriendList] = useState([]);
    const addFriendMutation = useAddFriend();
    const respondFriendRequestMutation = useRespondFriendRequest();
    const { data: receivedFriendRequests = [] } = useReceivedFriendRequests(!!me);
    const { data: myAllRooms = [], refetch: refetchMyAllRooms } = useGetMyAllRooms(!!me);

    const dispatch = useDispatch();

    const [searchWord, setSearchWord] = useState('');
    const debouncedSearchWord = useDebounce(searchWord, 500);
    const {
        data: searchUsersResults = [],
        isLoading: isSearching
    } = useSearchUsers(debouncedSearchWord);

    // ==== 친구 추가 ==================================================
    function addFriend(targetPublicId) {
        addFriendMutation.mutate(targetPublicId, {
            onSuccess: () => {
                alert('친구 요청을 보냈습니다!');
            },
            onError: (e) => {
                if (e.response?.status === 409) {
                    alert('이미 친구이거나 친구 요청 중입니다.');
                    return;
                }

                if (e.response?.status === 401) {
                    alert('로그인이 필요합니다.');
                    nav('/login');
                    return;
                }

                alert('친구 요청 실패');
                console.log(e);
            }
        });
    }//addFriend

    // ====== 친구 수락/거절 =======================================================================================================
    function respondFriendRequest(publicId, action) {
        respondFriendRequestMutation.mutate(
            { publicId, action },
            {
                onSuccess: () => {
                    if (action === 'ACCEPT') {
                        alert('친구 요청을 수락 했습니다. accept');
                    }

                    if (action === 'REJECT') {
                        alert('친구 요청을 거절 했습니다. reject');
                    }
                },
                onError: (e) => {
                    alert('친구 요청 처리 실패');
                    console.log(e);
                }
            }
        );
    }//respondFriendRequest

    const enterRoom = async (room, enterType) => {

        const res = null;

        if (enterType === "DIRECT") {
            res = await axios.post(`/chat/getOrCreateDirectRoom`, { friendPublicId: friInfo.publicId })
        } else if (enterType === "GROUP") {
            res = await axios.post(`/chat/enterGroupRoom`, { roomId: openedRoomId })
        }

        sendWs("ENTER_ROOM", { roomId: room.roomId })

        dispatch(openChatWindow({
            roomId: room.roomId,
            roomType: room.roomType,
            roomName: room.roomName
        }));

    };

    // ====== 1:1 채팅 =======================================================================================================
    const getOrCreateDirectRoom = async (friInfo) => {
        try {
            const res = await axios.post(`/chat/getOrCreateDirectRoom`, {
                friendPublicId: friInfo.publicId
            });

            const createdDirectRoom = res.data;

            const openedRoomId = createdDirectRoom.roomId;

            sendWs("ENTER_ROOM", { roomId: openedRoomId });

            dispatch(openChatWindow({
                roomId: openedRoomId,
                roomType: createdDirectRoom.roomType,
                roomName: createdDirectRoom.roomName,
                friend: friInfo
            }));

            nav('/ChatList');

        } catch (e) {
            console.log(`채팅방 열기 실패!`);
            console.log(e);
        }
    };//getOrCreateDirectRoom

    // ====== 단톡방 ============================================================================
    const createGroupRoom = async (roomName, selectedFriends) => {
        if (selectedFriends.length === 0) {
            alert(`초대할 친구를 선택해주세요.`);
            return;
        }

        try {
            const selectedFriendPublicIdList = selectedFriends.map(f => f.publicId); // axios 즉시요청해서 객체 전부 보내지말고, 필터링 한번빼라.

            // const res = await axios.post(`/chat/createGroupRoom`, {
            await axios.post(`/chat/createGroupRoom`, {
                roomName: roomName,
                selectedFriendPublicIdList: selectedFriendPublicIdList
            });

            await refetchMyAllRooms();

            setRoomName('');
            setSelectedFriendList([]);

            console.log(`단톡방 만들기 성공`);

        } catch (e) {
            console.log(`단톡실패`);
        }
    };// createGroupRoom



    // ===== 친구 목록 체크 박스 설정=================================================================
    const isFriendSelected = (publicId) => {
        return selectedFriendList.some(friend => friend.publicId === publicId);
    };

    const toggleFriendSelect = (friend) => {
        setSelectedFriendList(prev => {
            const alreadySelected = prev.some(
                selectedFriend => selectedFriend.publicId === friend.publicId
            );

            if (alreadySelected) {
                return prev.filter(
                    selectedFriend => selectedFriend.publicId !== friend.publicId
                );
            }

            return [...prev, friend];
        });
    };

    const isAllSelected = friendList.length > 0 && friendList.every(friend => selectedFriendList.some(
        selectedFriend => selectedFriend.publicId === friend.publicId));

    const toggleSelectAllFriends = () => {
        setSelectedFriendList(prev => {
            const isAllSelectedNow =
                friendList.length > 0 &&
                prev.length === friendList.length;

            if (isAllSelectedNow) {
                return [];
            }

            return friendList;
        });
    };

    // ======< return >=======================================================================================================
    return (
        <div className='FriendListContainer'>

            {/**========= 친구 목록=================== */}
            <div className='friendsListSection'>
                <div>친구목록</div>
                {friendList.length > 0 ? friendList.map((friend) => (
                    <div key={friend.publicId}>
                        <input
                            type="checkbox"
                            checked={selectedFriendList.some(
                                selectedFriend => selectedFriend.publicId === friend.publicId
                            )}
                            onChange={() => toggleFriendSelect(friend)}
                        />
                        <span>{friend.nickname}</span>
                        <span>{friend.friendCode}</span>
                        &nbsp;&nbsp;
                        <button onClick={() => getOrCreateDirectRoom(friend)}>
                            채팅
                        </button>
                    </div>
                )) : (
                    <div>친구 없음</div>
                )}
            </div>

            {/* <div>{selectedFriendList}</div> */} {/** Object그 자체는 React가 rendering 할 수 없다. */}
            <div>단톡 초대 체크된 친구들</div>
            <br />
            <input
                type="checkbox"
                checked={isAllSelected}
                onChange={toggleSelectAllFriends}
            />
            <span>모두선택</span>
            &nbsp;&nbsp;&nbsp;
            <input type="text" value={roomName} onChange={(e) => setRoomName(e.target.value)} placeholder='단톡방 이름 입력...' />
            <button
                onClick={() => createGroupRoom(roomName, selectedFriendList)}
            >
                단톡초대하기
            </button>
            <div></div>
            <br />
            <div>
                {/* <pre>{JSON.stringify(selectedFriendList, null, 2)}</pre> */} {/**객체 정보 다 보기 */}
                {selectedFriendList.map(friend => friend.nickname).join(', ')} {/**닉만 보기. */}
            </div>

            {/**========= 친구 요청 목록=================== */}
            <div className='friendsAlertList'>
                <div>친구 요청 목록</div>

                {receivedFriendRequests.length > 0 ? receivedFriendRequests.map((requestUser) => (
                    <div key={requestUser.publicId}>
                        <span>{requestUser.nickname}</span>
                        <span>{requestUser.friendCode}</span>
                        &nbsp;&nbsp;
                        <button
                            onClick={() => respondFriendRequest(requestUser.publicId, 'ACCEPT')}
                            disabled={respondFriendRequestMutation.isPending}
                        >
                            수락
                        </button>

                        <button
                            onClick={() => respondFriendRequest(requestUser.publicId, 'REJECT')}
                            disabled={respondFriendRequestMutation.isPending}
                        >
                            거절
                        </button>
                    </div>
                )) : (
                    <div>받은 친구 요청 없음</div>
                )}

            </div>

            {/**========= 유저 검색 및 친구추가 =================== */}
            <div className='searchOthersSection'>
                <div className='searchFriend'>
                    <span>검색 : </span>
                    <input
                        type="text"
                        value={searchWord}
                        onChange={(e) => setSearchWord(e.target.value)}
                        placeholder='닉네임/친구코드 검색'
                    />
                </div>
                <div>
                    {isSearching && <div>검색 중...</div>}

                    {!isSearching && debouncedSearchWord.trim().length > 0 && searchUsersResults.length === 0 && (
                        <div>검색 결과 없음</div>
                    )}

                    {searchUsersResults.map((user) => (
                        <div key={user.publicId}>
                            <span>{user.nickname}</span>
                            <span>{user.friendCode}</span> {/** <-- 임시로 개발중에만 띄움. 추후 삭제 예정. */}
                            &nbsp;&nbsp;
                            <button
                                onClick={() => addFriend(user.publicId)}
                                disabled={addFriendMutation.isPending}>
                                추가
                            </button>

                        </div>
                    ))}
                </div>
            </div>

            {/**FriendList끝. */}
        </div>
    );
}

export default FriendList;