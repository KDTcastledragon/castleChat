import './Friends.css';

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

import { useMe } from '../../hooks/useAuthUser';
import { useDebounce } from '../../hooks/useDebounce';
import { useSearchUsers } from '../../hooks/useSearchUsers';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import { useFriendList, useAddFriend, useReceivedFriendRequests, useRespondFriendRequest } from '../../hooks/useFriend';
import { registerGlobalWsHandler } from '../../webSocket/wsClient';

function FriendList() {
    const nav = useNavigate();
    const queryClient = useQueryClient();

    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: friendList = [] } = useFriendList(!!me);
    const { data: receivedFriendRequests = [] } = useReceivedFriendRequests(!!me);

    const addFriendMutation = useAddFriend();
    const respondFriendRequestMutation = useRespondFriendRequest();
    const { getOrCreateDirectRoom, createGroupRoom } = useChatRoomActions();

    const [roomName, setRoomName] = useState('');
    const [roomThumbnail, setRoomThumbnail] = useState('');
    const [selectedFriendList, setSelectedFriendList] = useState([]);
    const [searchWord, setSearchWord] = useState('');

    const debouncedSearchWord = useDebounce(searchWord, 500);
    const {
        data: searchUsersResults = [],
        isLoading: isSearching
    } = useSearchUsers(debouncedSearchWord);

    useEffect(() => {
        if (!me) return;

        return registerGlobalWsHandler((wsEvt) => {
            const payload = wsEvt.payload ?? {};

            switch (wsEvt.wsType) {
                case 'FRIEND_REQUEST_RECEIVED':
                    queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                    break;

                case 'FRIEND_REQUEST_RESPONDED':
                    queryClient.invalidateQueries({ queryKey: ['friends'] });
                    queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });

                    break;

                case 'RESPOND_FRIEND_OK':
                    queryClient.invalidateQueries({ queryKey: ['friends'] });
                    queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                    break;

                case 'ADD_FRIEND_OK':
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                    break;

                default:
                    break;
            }
        });
    }, [me, queryClient]);

    function addFriend(targetPublicId) {
        addFriendMutation.mutate(targetPublicId, {
            onSuccess: () => {
                alert('친구 요청을 보냈습니다.');
            },
            onError: (e) => {
                if (e.response?.status === 401) {
                    alert('로그인이 필요합니다.');
                    nav('/login');
                    return;
                }

                alert(e.message || '친구 요청 실패');
            }
        });
    }

    function respondFriendRequest(publicId, action) {
        respondFriendRequestMutation.mutate(
            { publicId, action },
            {
                onSuccess: () => {
                    alert(action === 'ACCEPT' ? '친구 요청을 수락했습니다.' : '친구 요청을 거절했습니다.');
                },
                onError: (e) => {
                    alert(e.message || '친구 요청 처리 실패');
                }
            }
        );
    }

    async function createGroupRoomBySelectedFriends() {
        try {
            await createGroupRoom(roomName, roomThumbnail, selectedFriendList, true);
            setRoomName('');
            setRoomThumbnail('');
            setSelectedFriendList([]);
        } catch (e) {
            alert(e.message || '단톡방 생성 실패');
        }
    }

    function toggleFriendSelect(friend) {
        setSelectedFriendList(prev => {
            const alreadySelected = prev.some(selectedFriend => selectedFriend.publicId === friend.publicId);

            if (alreadySelected) {
                return prev.filter(selectedFriend => selectedFriend.publicId !== friend.publicId);
            }

            return [...prev, friend];
        });
    }

    const isAllSelected = friendList.length > 0 && friendList.every(friend =>
        selectedFriendList.some(selectedFriend => selectedFriend.publicId === friend.publicId)
    );

    function toggleSelectAllFriends() {
        setSelectedFriendList(isAllSelected ? [] : friendList);
    }

    if (isCheckingLogin) {
        return <div className='FriendListContainer'>로그인 확인 중...</div>;
    }

    if (!me) {
        return <div className='FriendListContainer'>로그인이 필요합니다.</div>;
    }

    return (
        <div className='FriendListContainer'>
            <section className='friendPanel friendListPanel'>
                <div className='friendPanelHeader'>
                    <div>
                        <h2>친구목록</h2>
                        <p>{friendList.length}명</p>
                    </div>
                    <button onClick={toggleSelectAllFriends}>
                        {isAllSelected ? '전체해제' : '전체선택'}
                    </button>
                </div>

                <div className='friendScrollBox'>
                    {friendList.length > 0 ? friendList.map((friend) => {
                        const selected = selectedFriendList.some(selectedFriend => selectedFriend.publicId === friend.publicId);

                        return (
                            <div className={`friendItem ${selected ? 'selected' : ''}`} key={friend.publicId}>
                                <input
                                    type="checkbox"
                                    checked={selected}
                                    onChange={() => toggleFriendSelect(friend)}
                                />

                                <img src={friend.profileImg || '/images/mococo_question.png'} alt={friend.nickname} />

                                <div className='friendInfo'>
                                    <strong>{friend.nickname}</strong>
                                    <span>{friend.friendCode}</span>
                                </div>

                                <button onClick={() => getOrCreateDirectRoom(friend)}>
                                    채팅
                                </button>
                            </div>
                        );
                    }) : (
                        <div className='friendEmpty'>친구 없음</div>
                    )}
                </div>
            </section>

            <section className='friendPanel groupCreatePanel'>
                <div className='friendPanelHeader'>
                    <div>
                        <h2>단톡 만들기</h2>
                        <p>{selectedFriendList.length}명 선택됨</p>
                    </div>
                </div>

                <input
                    className='groupInput'
                    type="text"
                    value={roomName}
                    onChange={(e) => setRoomName(e.target.value)}
                    placeholder='단톡방 이름'
                />

                <input
                    className='groupInput'
                    type="text"
                    value={roomThumbnail}
                    onChange={(e) => setRoomThumbnail(e.target.value)}
                    placeholder='썸네일 URL'
                />

                <div className='selectedFriendChips'>
                    {selectedFriendList.length > 0
                        ? selectedFriendList.map(friend => <span key={friend.publicId}>{friend.nickname}</span>)
                        : <em>친구를 선택하면 여기에 표시됩니다.</em>}
                </div>

                <button
                    className='createGroupButton'
                    onClick={createGroupRoomBySelectedFriends}
                    disabled={selectedFriendList.length === 0}
                >
                    단톡초대하기
                </button>

                <div className='requestBox'>
                    <h3>받은 친구 요청</h3>

                    {receivedFriendRequests.length > 0 ? receivedFriendRequests.map((requestUser) => (
                        <div className='requestItem' key={requestUser.publicId}>
                            <img src={requestUser.profileImg || '/images/mococo_question.png'} alt={requestUser.nickname} />
                            <div>
                                <strong>{requestUser.nickname}</strong>
                                <span>{requestUser.friendCode}</span>
                            </div>
                            <button
                                onClick={() => respondFriendRequest(requestUser.publicId, 'ACCEPT')}
                                disabled={respondFriendRequestMutation.isPending}
                            >
                                수락
                            </button>
                            <button
                                className='rejectButton'
                                onClick={() => respondFriendRequest(requestUser.publicId, 'REJECT')}
                                disabled={respondFriendRequestMutation.isPending}
                            >
                                거절
                            </button>
                        </div>
                    )) : (
                        <div className='friendEmpty'>받은 친구 요청 없음</div>
                    )}
                </div>
            </section>

            <section className='friendPanel searchPanel'>
                <div className='friendPanelHeader'>
                    <div>
                        <h2>친구추가</h2>
                        <p>닉네임 / 친구코드 검색</p>
                    </div>
                </div>

                <input
                    className='searchFriendInput'
                    type="text"
                    value={searchWord}
                    onChange={(e) => setSearchWord(e.target.value)}
                    placeholder='검색어 입력'
                />

                <div className='friendScrollBox searchResultBox'>
                    {isSearching && <div className='friendEmpty'>검색 중...</div>}

                    {!isSearching && debouncedSearchWord.trim().length > 0 && searchUsersResults.length === 0 && (
                        <div className='friendEmpty'>검색 결과 없음</div>
                    )}

                    {searchUsersResults.map((user) => (
                        <div className='friendItem' key={user.publicId}>
                            <img src={user.profileImg || '/images/mococo_question.png'} alt={user.nickname} />

                            <div className='friendInfo'>
                                <strong>{user.nickname}</strong>
                                <span>{user.friendCode}</span>
                            </div>

                            <button
                                onClick={() => addFriend(user.publicId)}
                                disabled={addFriendMutation.isPending}
                            >
                                추가
                            </button>
                        </div>
                    ))}
                </div>
            </section>
        </div>
    );
}

export default FriendList;
