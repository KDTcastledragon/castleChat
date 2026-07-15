import './Friends.css';

import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

import { useMe } from '../../hooks/useAuthUser';
import { useDebounce } from '../../hooks/useDebounce';
import { useSearchUsers } from '../../hooks/useSearchUsers';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import {
    addAcceptedFriendToCache,
    addReceivedFriendRequestToCache,
    removeReceivedFriendRequestFromCache,
    useFriendList,
    useAddFriend,
    useReceivedFriendRequests,
    useRespondFriendRequest
} from '../../hooks/useFriend';
import { registerGlobalWsHandler } from '../../webSocket/wsClient';
import { uploadImageApi } from '../../api/chatApi';

function FriendList() {
    const nav = useNavigate();
    const queryClient = useQueryClient();

    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: friendList = [] } = useFriendList(!!me);
    const { data: receivedFriendRequests = [] } = useReceivedFriendRequests(!!me);

    const addFriendMutation = useAddFriend();
    const respondFriendRequestMutation = useRespondFriendRequest();
    const { getOrCreateDirectRoom, createGroupRoom } = useChatRoomActions();
    const groupThumbnailInputRef = useRef(null);

    const [roomName, setRoomName] = useState('');
    const [roomThumbnail, setRoomThumbnail] = useState('');
    const [roomThumbnailFileName, setRoomThumbnailFileName] = useState('');
    const [isUploadingRoomThumbnail, setIsUploadingRoomThumbnail] = useState(false);
    const [selectedFriendList, setSelectedFriendList] = useState([]);
    const [searchWord, setSearchWord] = useState('');
    const [friendContextMenu, setFriendContextMenu] = useState(null);
	const [focusedFriendPublicId, setFocusedFriendPublicId] = useState(null);
    const [friendConfirmModal, setFriendConfirmModal] = useState(null);
    const friendListContainerRef = useRef(null);

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
                    addReceivedFriendRequestToCache(queryClient, payload);
                    queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                    break;

                case 'FRIEND_REQUEST_RESPONDED':
                    addAcceptedFriendToCache(queryClient, payload, me.publicId);
                    queryClient.invalidateQueries({ queryKey: ['friends'] });
                    queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.invalidateQueries({ queryKey: ['searchUsers'] });

                    break;

                case 'RESPOND_FRIEND_OK':
                    addAcceptedFriendToCache(queryClient, payload, me.publicId);
                    removeReceivedFriendRequestFromCache(queryClient, payload);
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
            setRoomThumbnailFileName('');
            setSelectedFriendList([]);
        } catch (e) {
            alert(e.message || '단톡방 생성 실패');
        }
    }

    async function changeGroupRoomThumbnail(e) {
        const file = e.target.files?.[0];
        e.target.value = '';

        if (!file) {
            return;
        }

        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 선택할 수 있습니다.');
            return;
        }

        try {
            setIsUploadingRoomThumbnail(true);
            const uploadedImageUrl = await uploadImageApi(file, 'ROOM_THUMBNAIL');

            if (!uploadedImageUrl) {
                throw new Error('업로드된 이미지 주소가 없습니다.');
            }

            setRoomThumbnail(uploadedImageUrl);
            setRoomThumbnailFileName(file.name);
        } catch (err) {
            alert(err.message || '단톡방 썸네일 업로드 실패');
        } finally {
            setIsUploadingRoomThumbnail(false);
        }
    }

    // 친구 정보 영역 우클릭 시 msg 메뉴처럼 컨텍스트 메뉴를 띄운다.
    function openFriendContextMenu(e, friend) {
        e.preventDefault();
        e.stopPropagation();

        const containerRect = friendListContainerRef.current?.getBoundingClientRect();
        if (!containerRect) return;

        const menuWidth = 130;
        const menuHeight = 80;
        let left = e.clientX - containerRect.left;
        let top = e.clientY - containerRect.top;

        left = Math.min(Math.max(left, 8), containerRect.width - menuWidth - 8);
        top = Math.min(Math.max(top, 8), containerRect.height - menuHeight - 8);

        setFriendContextMenu({ friend, left, top });
    }

    function handleFriendMenuAction(action) {
        const targetFriend = friendContextMenu?.friend;
        setFriendContextMenu(null);

        if (!targetFriend) return;

        if (action === 'DELETE') {
            setFriendConfirmModal({ action, message: `${targetFriend.nickname}님을 친구에서 삭제하시겠습니까?` });
        }

        if (action === 'BLOCK') {
            setFriendConfirmModal({ action, message: `${targetFriend.nickname}님을 차단하시겠습니까?` });
        }
    }

    function confirmFriendMenuAction() {
        if (!friendConfirmModal) return;

        alert(friendConfirmModal.action === 'DELETE'
            ? '친구삭제는 아직 준비 중입니다.'
            : '친구차단은 아직 준비 중입니다.');
        setFriendConfirmModal(null);
    }

    useEffect(() => {
        const closeMenu = (e) => {
            if (e.type === 'keydown' && e.key !== 'Escape') return;
            if (e.type === 'mousedown' && e.target.closest('.friendContextMenu')) return;
			if (e.type === 'mousedown' && e.target.closest('.friendIdentityArea')) return;

            setFriendContextMenu(null);
			setFocusedFriendPublicId(null);
        };

        document.addEventListener('mousedown', closeMenu);
        window.addEventListener('keydown', closeMenu);

        return () => {
            document.removeEventListener('mousedown', closeMenu);
            window.removeEventListener('keydown', closeMenu);
        };
    }, []);

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
        <div className='FriendListContainer' ref={friendListContainerRef}>
            {friendConfirmModal && (
                <div className="friendConfirmOverlay" onMouseDown={() => setFriendConfirmModal(null)}>
                    <div className="friendConfirmModal" onMouseDown={(e) => e.stopPropagation()}>
                        <strong>친구 관리</strong>
                        <p>{friendConfirmModal.message}</p>
                        <div>
                            <button className="danger" onClick={confirmFriendMenuAction}>확인</button>
                            <button onClick={() => setFriendConfirmModal(null)}>취소</button>
                        </div>
                    </div>
                </div>
            )}

            {friendContextMenu && (
                <div
                    className="friendContextMenu"
                    style={{ left: friendContextMenu.left, top: friendContextMenu.top }}
                >
                    <button onClick={() => handleFriendMenuAction('DELETE')}>친구삭제</button>
                    <button className="danger" onClick={() => handleFriendMenuAction('BLOCK')}>친구차단</button>
                </div>
            )}

            <div className='friendPanel friendListPanel'>
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
                            <div
								className={`friendItem ${selected ? 'checked' : ''} ${focusedFriendPublicId === friend.publicId ? 'focused' : ''}`}
                                key={friend.publicId}
                            >
                                <input
                                    type="checkbox"
                                    checked={selected}
                                    onChange={() => toggleFriendSelect(friend)}
                                />

                                <div
                                    className="friendIdentityArea"
                                    onContextMenu={(e) => {
                                        setFocusedFriendPublicId(friend.publicId);
                                        openFriendContextMenu(e, friend);
                                    }}
                                >
                                    <img src={friend.profileImg || '/images/mococo_question.png'} alt={friend.nickname} />

                                    <div className='friendInfo'>
                                        <strong>{friend.nickname}</strong>
                                        <span>{friend.friendCode}</span>
                                    </div>
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
            </div>

            <div className='friendPanel groupCreatePanel'>
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

                <div className='groupThumbnailPicker'>
                    <img
                        src={roomThumbnail || '/images/mococo_question.png'}
                        alt='단톡방 썸네일'
                    />

                    <div className='groupThumbnailControl'>
                        <strong>{roomThumbnailFileName || '썸네일 이미지 선택'}</strong>
                        <span>{roomThumbnail ? '선택 완료' : '선택하지 않으면 기본 이미지로 생성됩니다.'}</span>

                        <button
                            type='button'
                            onClick={() => groupThumbnailInputRef.current?.click()}
                            disabled={isUploadingRoomThumbnail}
                        >
                            {isUploadingRoomThumbnail ? '업로드 중...' : '파일 선택'}
                        </button>
                    </div>

                    <input
                        ref={groupThumbnailInputRef}
                        type='file'
                        accept='image/*'
                        hidden
                        onChange={changeGroupRoomThumbnail}
                    />
                </div>

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
                    단톡방 열기
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
            </div>

            <div className='friendPanel searchPanel'>
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
                                친구 요청
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default FriendList;
