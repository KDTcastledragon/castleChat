import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef, useMemo } from 'react';
import {
    emitWsExitRoom,
    emitWsLeftRoom,
    emitWsReadMessage,
    emitWsSendMessage,
    emitWsTypingStart,
    emitWsTypingStop,
    registerRoomHandler,
    unregisterRoomHandler
} from '../../webSocket/wsClient';
import { useMe } from '../../hooks/useAuthUser';
import { useFriendList } from '../../hooks/useFriend';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import { useQueryClient } from '@tanstack/react-query';
import { leftRoomApi, loadMessagesInRoomApi } from '../../api/chatApi';

function ChatBox({ roomId, roomType, roomName, memberList, x, y, zIndex, exitChatRoom, onMove, onFocus }) {
    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const [typingUsers, setTypingUsers] = useState([]);

    const [isRoomMenuOpen, setIsRoomMenuOpen] = useState(false);
    const [isInviteFriendPanelOpen, setIsInviteFriendPanelOpen] = useState(false);
    const [selectedInviteFriends, setSelectedInviteFriends] = useState([]);
    const [isInvitingMembers, setIsInvitingMembers] = useState(false);

    const [profileTargetMember, setProfileTargetMember] = useState(null);
    const [messageContextMenu, setMessageContextMenu] = useState(null);

    const [locallyRemovedMemberPublicIds, setLocallyRemovedMemberPublicIds] = useState(() => new Set());
    const [locallyAddedRoomMembers, setLocallyAddedRoomMembers] = useState([]);

    const queryClient = useQueryClient();
    const { getOrCreateDirectRoom } = useChatRoomActions();

    const { data: me } = useMe();
    const { publicId: myPublicId } = me || {};

    const { data: friendList = [], isLoading: isFriendListLoading } = useFriendList(!!me && isInviteFriendPanelOpen);

    const visibleRoomMembers = useMemo(() => {
        const map = new Map();

        (memberList ?? []).forEach(member => {
            map.set(member.publicId, member);
        });

        locallyAddedRoomMembers.forEach(member => {
            map.set(member.publicId, member);
        });

        locallyRemovedMemberPublicIds.forEach(publicId => {
            map.delete(publicId);
        });

        return Array.from(map.values());
    }, [memberList, locallyAddedRoomMembers, locallyRemovedMemberPublicIds]);

    const memberMap = useMemo(() => {
        const map = {};

        visibleRoomMembers.forEach(member => {
            map[member.publicId] = member;
        });

        return map;
    }, [visibleRoomMembers]);

    const myRoomMemberInfo = useMemo(() => {
        return visibleRoomMembers.find(member => member.publicId === myPublicId);
    }, [visibleRoomMembers, myPublicId]);

    const myRoomRole = myRoomMemberInfo?.role;

    const inviteCandidateFriends = useMemo(() => {
        const currentRoomMemberPublicIds = new Set(
            visibleRoomMembers.map(member => member.publicId)
        );

        return friendList.filter(friend => !currentRoomMemberPublicIds.has(friend.publicId));
    }, [friendList, visibleRoomMembers]);

    function canKickMember(member) {
        if (!member) return false;

        const isMe = member.publicId === myPublicId;

        if (isMe) return false;
        if (roomType !== 'GROUP') return false;

        if (myRoomRole === 'HOST') {
            return member.role !== 'HOST';
        }

        if (myRoomRole === 'MANAGER') {
            return member.role === 'MEMBER';
        }

        return false;
    }

    const chatRoomSectionRef = useRef(null);
    const isChatBoxFocusedRef = useRef(false);

    const chatEndRef = useRef(null);

    const isTypingRef = useRef(false);
    const typingTimerRef = useRef(null);

    const readerReadPositionsRef = useRef({});
    const pendingReadMessageIdRef = useRef(null);
    const readDebounceTimerRef = useRef(null);

    const MESSAGE_PAGE_SIZE = 50;
    const LOAD_PREV_THRESHOLD_PX = 80;

    const chatListRef = useRef(null);
    const isLoadingPrevRef = useRef(false);
    const hasMorePrevRef = useRef(true);
    const oldestMessageIdRef = useRef(null);
    const isPrependingPrevRef = useRef(false);

    const dragRef = useRef({
        isDragging: false,
        startMouseX: 0,
        startMouseY: 0,
        startX: 0,
        startY: 0
    });

    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };

    const startDrag = (e) => {
        onFocus();

        dragRef.current = {
            isDragging: true,
            startMouseX: e.clientX,
            startMouseY: e.clientY,
            startX: x,
            startY: y
        };
    };

    useEffect(() => {
        const handleMouseMove = (e) => {
            if (!dragRef.current.isDragging) return;

            const movedX = e.clientX - dragRef.current.startMouseX;
            const movedY = e.clientY - dragRef.current.startMouseY;

            onMove(
                dragRef.current.startX + movedX,
                dragRef.current.startY + movedY
            );
        };

        const handleMouseUp = () => {
            dragRef.current.isDragging = false;
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);

        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, [onMove]);

    useEffect(() => {
        if (isPrependingPrevRef.current) {
            isPrependingPrevRef.current = false;
            return;
        }

        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [prevChattings]);

    async function loadOlderMessages() {
        if (isLoadingPrevRef.current) return;
        if (!hasMorePrevRef.current) return;

        const beforeMessageId = oldestMessageIdRef.current;
        if (!beforeMessageId) return;

        const chatListEl = chatListRef.current;
        const prevScrollHeight = chatListEl?.scrollHeight ?? 0;

        isLoadingPrevRef.current = true;
        isPrependingPrevRef.current = true;

        try {
            const res = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
                params: {
                    beforeMessageId,
                    limit: MESSAGE_PAGE_SIZE
                }
            });

            const olderMessages = res.data ?? [];

            if (olderMessages.length === 0) {
                hasMorePrevRef.current = false;
                isPrependingPrevRef.current = false;
                return;
            }

            setPrevChattings(prev => {
                const existingIds = new Set(prev.map(msg => msg.messageId));
                const dedupedOlder = olderMessages.filter(msg => !existingIds.has(msg.messageId));

                if (dedupedOlder.length === 0) {
                    hasMorePrevRef.current = false;
                    isPrependingPrevRef.current = false;
                    return prev;
                }

                const next = [...dedupedOlder, ...prev];
                oldestMessageIdRef.current = next[0]?.messageId ?? null;

                return next;
            });

            hasMorePrevRef.current = olderMessages.length === MESSAGE_PAGE_SIZE;

            requestAnimationFrame(() => {
                const currentEl = chatListRef.current;
                if (!currentEl) return;

                const newScrollHeight = currentEl.scrollHeight;
                currentEl.scrollTop = newScrollHeight - prevScrollHeight;
            });
        } catch (e) {
            console.error("이전 메시지 로딩 실패", e);
            isPrependingPrevRef.current = false;
        } finally {
            isLoadingPrevRef.current = false;
        }
    }

    function handleChatScroll() {
        setMessageContextMenu(null);

        const chatListEl = chatListRef.current;
        if (!chatListEl) return;

        if (chatListEl.scrollTop <= LOAD_PREV_THRESHOLD_PX) {
            loadOlderMessages();
        }
    }

    useEffect(() => {
        if (!roomId || !myPublicId) return;

        isLoadingPrevRef.current = false;
        hasMorePrevRef.current = true;
        oldestMessageIdRef.current = null;
        isPrependingPrevRef.current = false;

        setIsRoomMenuOpen(false);
        setIsInviteFriendPanelOpen(false);
        setSelectedInviteFriends([]);
        setProfileTargetMember(null);
        setMessageContextMenu(null);
        setLocallyRemovedMemberPublicIds(new Set());
        setLocallyAddedRoomMembers([]);

        const initChatRoom = async () => {
            try {
                // const loadedMessagesInRoom = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
                //     params: {
                //         limit: MESSAGE_PAGE_SIZE
                //     }
                // });

                const loadedMessagesInRoom = loadMessagesInRoomApi(roomId, MESSAGE_PAGE_SIZE);

                const messages = loadedMessagesInRoom.data ?? [];

                setPrevChattings(messages);

                hasMorePrevRef.current = messages.length === MESSAGE_PAGE_SIZE;
                oldestMessageIdRef.current = messages[0]?.messageId ?? null;

                if (messages.length) {
                    const lastOtherMsgInRoom = [...messages]
                        .reverse()
                        .find(msg => msg.senderPublicId !== myPublicId);

                    if (lastOtherMsgInRoom !== undefined) {
                        scheduleReadMessage(lastOtherMsgInRoom.messageId);
                    }
                }
            } catch (e) {
                console.error("메시지 조회 실패", e);

                if (e.response?.status === 401) {
                    console.log(`401 error`);
                } else {
                    console.log(e);
                }
            }
        };

        initChatRoom();

        registerRoomHandler(roomId, (wsResponse) => {
            if (wsResponse.wsType === "MSG_CREATED") {
                const newMsg = wsResponse.payload;

                setPrevChattings(prev => [...prev, newMsg]);

                if (newMsg.senderPublicId !== myPublicId) {
                    scheduleReadMessage(newMsg.messageId);
                }
            }

            if (wsResponse.wsType === "MSG_READ") {
                const readPositionResponse = wsResponse.payload;

                if (!readPositionResponse) return;

                const readerPublicId = readPositionResponse.readerPublicId;
                const serverOldLastReadMsgId = Number(readPositionResponse.oldLastReadMessageId ?? 0);
                const serverNewLastReadMsgId = Number(readPositionResponse.lastReadMessageId ?? 0);

                if (!readerPublicId || !serverNewLastReadMsgId) return;

                const knownLastReadMsgId = Number(readerReadPositionsRef.current[readerPublicId] ?? 0);

                if (serverNewLastReadMsgId <= knownLastReadMsgId) {
                    return;
                }

                const fromLastReadMsgId = Math.max(serverOldLastReadMsgId, knownLastReadMsgId);
                const toLastReadMsgId = serverNewLastReadMsgId;

                setPrevChattings(prev =>
                    prev.map(msg => {
                        if (msg.messageType === 'SYSTEM') {
                            return msg;
                        }

                        const messageId = Number(msg.messageId);

                        const shouldDecrease =
                            messageId > fromLastReadMsgId &&
                            messageId <= toLastReadMsgId &&
                            msg.senderPublicId !== readerPublicId;

                        if (!shouldDecrease) {
                            return msg;
                        }

                        return {
                            ...msg,
                            unreadCount: Math.max(Number(msg.unreadCount ?? 0) - 1, 0)
                        };
                    })
                );

                readerReadPositionsRef.current[readerPublicId] = Math.max(knownLastReadMsgId, toLastReadMsgId);
            }

            if (wsResponse.wsType === "TYPING_START") {
                const typingInfo = wsResponse.payload;

                if (typingInfo.publicId === myPublicId) {
                    return;
                }

                setTypingUsers(prev => {
                    const alreadyExists = prev.some(
                        user => user.publicId === typingInfo.publicId
                    );

                    if (alreadyExists) {
                        return prev;
                    }

                    return [...prev, typingInfo];
                });
            }

            if (wsResponse.wsType === "TYPING_STOP") {
                const typingInfo = wsResponse.payload;

                setTypingUsers(prev =>
                    prev.filter(user => user.publicId !== typingInfo.publicId)
                );
            }

            if (wsResponse.wsType === "ROOM_NOTICE") {
                const notice = wsResponse.payload;

                setPrevChattings(prev => [
                    ...prev,
                    {
                        messageId: `notice-${crypto.randomUUID()}`,
                        roomId: notice.roomId,
                        messageText: notice.message,
                        messageType: 'SYSTEM',
                        createdAt: notice.createdAt
                    }
                ]);
            }
        });

        return () => {
            flushPendingReadMessage();
            unregisterRoomHandler(roomId);
        };
    }, [roomId, myPublicId]);

    function sendChatMessage() {
        if (isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        const isEmitted = emitWsSendMessage(roomId, chatMessage);

        if (!isEmitted) {
            console.log(`!isEmitted`);
            return;
        }

        setChatMessage('');
    }

    const handleChatMessageChange = (e) => {
        const nextValue = e.target.value;

        setChatMessage(nextValue);

        const isNowTyping = nextValue.length > 0;

        if (isNowTyping && !isTypingRef.current) {
            isTypingRef.current = true;
            emitWsTypingStart(roomId);
        }

        if (!isNowTyping && isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
        }

        if (isNowTyping) {
            typingTimerRef.current = setTimeout(() => {
                if (isTypingRef.current) {
                    isTypingRef.current = false;
                    emitWsTypingStop(roomId);
                }
            }, 90000);
        }
    };

    function scheduleReadMessage(messageId) {
        if (!messageId) return;

        pendingReadMessageIdRef.current = Math.max(
            Number(pendingReadMessageIdRef.current ?? 0),
            Number(messageId)
        );

        if (readDebounceTimerRef.current) {
            clearTimeout(readDebounceTimerRef.current);
        }

        readDebounceTimerRef.current = setTimeout(() => {
            const lastReadMessageId = pendingReadMessageIdRef.current;

            if (lastReadMessageId) {
                emitWsReadMessage(roomId, lastReadMessageId);
            }

            pendingReadMessageIdRef.current = null;
            readDebounceTimerRef.current = null;
        }, 300);
    }

    function flushPendingReadMessage() {
        const lastReadMessageId = pendingReadMessageIdRef.current;

        if (readDebounceTimerRef.current) {
            clearTimeout(readDebounceTimerRef.current);
            readDebounceTimerRef.current = null;
        }

        if (lastReadMessageId) {
            emitWsReadMessage(roomId, lastReadMessageId);
        }

        pendingReadMessageIdRef.current = null;
    }

    const closeChatAndExitRoom = () => {
        flushPendingReadMessage();

        if (isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        emitWsExitRoom(roomId);
        exitChatRoom();
    };

    useEffect(() => {
        const handleDocumentMouseDown = (e) => {
            if (!chatRoomSectionRef.current) return;

            isChatBoxFocusedRef.current = chatRoomSectionRef.current.contains(e.target);

            if (!e.target.closest('.messageContextMenu')) {
                setMessageContextMenu(null);
            }
        };

        document.addEventListener('mousedown', handleDocumentMouseDown);

        return () => {
            document.removeEventListener('mousedown', handleDocumentMouseDown);
        };
    }, []);

    useEffect(() => {
        const handleEscKeyDown = (e) => {
            if (e.key !== 'Escape') return;

            if (messageContextMenu) {
                setMessageContextMenu(null);
                return;
            }

            if (profileTargetMember) {
                setProfileTargetMember(null);
                return;
            }

            if (isInviteFriendPanelOpen) {
                closeInviteFriendPanel();
                return;
            }

            if (isRoomMenuOpen) {
                setIsRoomMenuOpen(false);
                return;
            }

            if (isChatBoxFocusedRef.current) {
                closeChatAndExitRoom();
            }
        };

        window.addEventListener('keydown', handleEscKeyDown);

        return () => {
            window.removeEventListener('keydown', handleEscKeyDown);
        };
    }, [messageContextMenu, profileTargetMember, isInviteFriendPanelOpen, isRoomMenuOpen]);

    async function leftRoom() {
        try {
            flushPendingReadMessage();

            await leftRoomApi(roomId);

            emitWsLeftRoom(roomId);
            exitChatRoom();

            queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
        } catch (e) {
            console.error('방 나가기 실패', e);
            alert('방 나가기 실패');
        }
    }

    async function kickMember(member) {
        if (!member) return;

        const confirmed = window.confirm(`${member.nickname}님을 강퇴하시겠습니까?`);

        if (!confirmed) {
            return;
        }

        try {
            await axios.post('/room/kickMemberInRoom', {
                roomId,
                kickTargetPublicId: member.publicId
            });

            setLocallyRemovedMemberPublicIds(prev => {
                const next = new Set(prev);
                next.add(member.publicId);
                return next;
            });
        } catch (e) {
            console.error('강퇴 실패', e);
            alert(e.response?.data ?? '강퇴 실패');
        }
    }

    function openInviteMemberPanel() {
        if (roomType !== 'GROUP') return;

        setSelectedInviteFriends([]);
        setIsInviteFriendPanelOpen(true);
    }

    function closeInviteFriendPanel() {
        setSelectedInviteFriends([]);
        setIsInviteFriendPanelOpen(false);
    }

    function toggleInviteFriend(friend) {
        setSelectedInviteFriends(prev => {
            const alreadySelected = prev.some(selected => selected.publicId === friend.publicId);

            if (alreadySelected) {
                return prev.filter(selected => selected.publicId !== friend.publicId);
            }

            return [...prev, friend];
        });
    }

    function isInviteFriendSelected(publicId) {
        return selectedInviteFriends.some(friend => friend.publicId === publicId);
    }

    async function inviteSelectedFriends() {
        if (selectedInviteFriends.length === 0) return;

        try {
            setIsInvitingMembers(true);

            const inviteTargetMemberPublicIds = selectedInviteFriends.map(friend => friend.publicId);

            await axios.post('/room/inviteGroupRoom', {
                roomId,
                inviteTargetMemberPublicIds
            });

            setLocallyAddedRoomMembers(prev => {
                const map = new Map();

                prev.forEach(member => {
                    map.set(member.publicId, member);
                });

                selectedInviteFriends.forEach(friend => {
                    map.set(friend.publicId, {
                        publicId: friend.publicId,
                        nickname: friend.nickname,
                        friendCode: friend.friendCode,
                        profileImg: friend.profileImg,
                        role: 'MEMBER'
                    });
                });

                return Array.from(map.values());
            });

            setSelectedInviteFriends([]);
            setIsInviteFriendPanelOpen(false);

            queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
        } catch (e) {
            console.error('초대 실패', e);
            alert(e.response?.data ?? '초대 실패');
        } finally {
            setIsInvitingMembers(false);
        }
    }

    function openProfilePopup(member) {
        if (!member) return;
        if (member.publicId === myPublicId) return;

        setProfileTargetMember(member);
    }

    function closeProfilePopup() {
        setProfileTargetMember(null);
    }

    async function startDirectChatFromProfile() {
        if (!profileTargetMember) return;

        try {
            await getOrCreateDirectRoom(profileTargetMember);
            setProfileTargetMember(null);
        } catch (e) {
            console.error('1:1 채팅 열기 실패', e);
            alert('1:1 채팅 열기 실패');
        }
    }

    function openMessageContextMenu(e, message) {
        e.preventDefault();
        e.stopPropagation();

        if (!message || message.messageType === 'SYSTEM') return;

        const boxRect = chatRoomSectionRef.current?.getBoundingClientRect();

        if (!boxRect) return;

        const menuWidth = 158;
        const menuHeight = 180;

        let left = e.clientX - boxRect.left;
        let top = e.clientY - boxRect.top;

        left = Math.min(left, boxRect.width - menuWidth - 8);
        top = Math.min(top, boxRect.height - menuHeight - 8);

        left = Math.max(left, 8);
        top = Math.max(top, 44);

        setMessageContextMenu({
            message,
            left,
            top
        });
    }

    function handleMessageMenuAction(action) {
        const targetMessage = messageContextMenu?.message;

        setMessageContextMenu(null);

        if (!targetMessage) return;

        if (action === 'REACTION') {
            alert(`리액션: ${targetMessage.messageId}`);
            return;
        }

        if (action === 'REPLY') {
            alert(`답장하기: ${targetMessage.messageText}`);
            return;
        }

        if (action === 'DELETE') {
            alert(`삭제하기: ${targetMessage.messageId}`);
            return;
        }

        if (action === 'READERS') {
            alert(`이 메시지 읽은 사람: ${targetMessage.messageId}`);
            return;
        }

        if (action === 'SHARE') {
            alert(`공유하기: ${targetMessage.messageId}`);
            return;
        }

        if (action === 'NOTICE') {
            alert(`공지: ${targetMessage.messageId}`);
        }
    }

    return (
        <div className='chatBoxContainer'>
            <div
                ref={chatRoomSectionRef}
                className='chattingRoomSection'
                style={{
                    left: x,
                    top: y,
                    zIndex
                }}
                onMouseDown={() => {
                    isChatBoxFocusedRef.current = true;
                    onFocus();
                }}
            >
                <div className='chatListTitle' onMouseDown={startDrag}>
                    <div className="chatTitleLeftControls">
                        <button
                            className={`roomMenuButton ${isRoomMenuOpen ? 'active' : ''}`}
                            title="채팅방 메뉴"
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={() => setIsRoomMenuOpen(prev => !prev)}
                        >
                            <span className="roomMenuButtonIcon">☰</span>
                        </button>
                    </div>

                    <span className="chatRoomTitleText">{roomName}</span>

                    <div className="chatTitleRightControls">
                        <button
                            className="chatCloseButton"
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={closeChatAndExitRoom}
                        >
                            닫기
                        </button>
                    </div>
                </div>

                <div
                    className={`roomSidePanel ${isRoomMenuOpen ? 'open' : ''}`}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div className="roomSidePanelHeader">
                        <span>채팅방 메뉴</span>
                        <button onClick={() => setIsRoomMenuOpen(false)}>닫기</button>
                    </div>

                    <div className="roomSidePanelDangerZone">
                        <button
                            className="leaveRoomInMenuButton"
                            onClick={leftRoom}
                        >
                            방 나가기
                        </button>
                    </div>

                    <div className="roomSidePanelSubTitle">
                        채팅방 멤버
                    </div>

                    <div className="roomMemberList">
                        {visibleRoomMembers.map(member => {
                            const isMe = member.publicId === myPublicId;

                            return (
                                <div
                                    className={`roomMemberItem ${isMe ? 'me' : ''}`}
                                    key={member.publicId}
                                >
                                    <img
                                        className="roomMemberProfileImg"
                                        src={member.profileImg || '/images/mococo_question.png'}
                                        alt={member.nickname}
                                    />

                                    <div className="roomMemberInfo">
                                        <div className="roomMemberNicknameLine">
                                            <span className="roomMemberNickname">{member.nickname}</span>
                                            {isMe && <span className="roomMemberMeBadge">나</span>}
                                        </div>
                                    </div>

                                    <div className={`roomMemberRole role-${member.role}`}>
                                        {member.role}
                                    </div>

                                    {canKickMember(member) ? (
                                        <button
                                            className="kickMemberButton"
                                            onClick={() => kickMember(member)}
                                        >
                                            강퇴
                                        </button>
                                    ) : (
                                        <div className="kickMemberButtonPlaceholder" />
                                    )}
                                </div>
                            );
                        })}
                    </div>

                    {roomType === 'GROUP' && (
                        <button
                            className="inviteMemberButton"
                            onClick={openInviteMemberPanel}
                        >
                            초대하기
                        </button>
                    )}
                </div>

                {isInviteFriendPanelOpen && roomType === 'GROUP' && (
                    <div
                        className="inviteFriendPanel"
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <div className="inviteFriendPanelHeader">
                            <span>친구 초대</span>
                            <button onClick={closeInviteFriendPanel}>닫기</button>
                        </div>

                        <div className="selectedInviteFriendsBox">
                            <div className="selectedInviteFriendsTitle">선택된 친구</div>

                            {selectedInviteFriends.length > 0 ? (
                                <div className="selectedInviteFriendsList">
                                    {selectedInviteFriends.map(friend => (
                                        <span className="selectedInviteFriendChip" key={friend.publicId}>
                                            {friend.nickname}
                                        </span>
                                    ))}
                                </div>
                            ) : (
                                <div className="selectedInviteEmpty">선택된 친구가 없습니다.</div>
                            )}
                        </div>

                        <div className="inviteFriendList">
                            {isFriendListLoading ? (
                                <div className="inviteFriendLoading">친구 목록 불러오는 중...</div>
                            ) : inviteCandidateFriends.length > 0 ? (
                                inviteCandidateFriends.map(friend => (
                                    <label className="inviteFriendItem" key={friend.publicId}>
                                        <input
                                            type="checkbox"
                                            checked={isInviteFriendSelected(friend.publicId)}
                                            onChange={() => toggleInviteFriend(friend)}
                                        />

                                        <img
                                            className="inviteFriendProfileImg"
                                            src={friend.profileImg || '/images/mococo_question.png'}
                                            alt={friend.nickname}
                                        />

                                        <div className="inviteFriendInfo">
                                            <div className="inviteFriendNickname">{friend.nickname}</div>
                                            <div className="inviteFriendCode">{friend.friendCode}</div>
                                        </div>
                                    </label>
                                ))
                            ) : (
                                <div className="inviteFriendEmpty">초대 가능한 친구가 없습니다.</div>
                            )}
                        </div>

                        <button
                            className="inviteFriendSubmitButton"
                            disabled={selectedInviteFriends.length === 0 || isInvitingMembers}
                            onClick={inviteSelectedFriends}
                        >
                            {isInvitingMembers ? '초대 중...' : '초대하기'}
                        </button>
                    </div>
                )}

                {profileTargetMember && (
                    <div
                        className="profilePopupOverlay"
                        onMouseDown={(e) => {
                            e.stopPropagation();
                            closeProfilePopup();
                        }}
                    >
                        <div
                            className="profilePopup"
                            onMouseDown={(e) => e.stopPropagation()}
                        >
                            <button className="profilePopupCloseButton" onClick={closeProfilePopup}>
                                닫기
                            </button>

                            <div className="profilePopupImageWrap">
                                <img
                                    className="profilePopupImage"
                                    src={profileTargetMember.profileImg || '/images/mococo_question.png'}
                                    alt={profileTargetMember.nickname}
                                />
                            </div>

                            <div className="profilePopupNickname">
                                {profileTargetMember.nickname}
                            </div>

                            <div className="profilePopupActions">
                                <button
                                    className="profilePopupActionButton direct"
                                    onClick={startDirectChatFromProfile}
                                >
                                    1:1 채팅
                                </button>

                                <button
                                    className="profilePopupActionButton report"
                                    onClick={() => alert('임시 버튼')}
                                >
                                    rand_btn
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {messageContextMenu && (
                    <div
                        className="messageContextMenu"
                        style={{
                            left: messageContextMenu.left,
                            top: messageContextMenu.top
                        }}
                        onMouseDown={(e) => e.stopPropagation()}
                        onContextMenu={(e) => e.preventDefault()}
                    >
                        <button onClick={() => handleMessageMenuAction('REACTION')}>리액션</button>
                        <button onClick={() => handleMessageMenuAction('REPLY')}>답장하기</button>
                        <button onClick={() => handleMessageMenuAction('DELETE')}>삭제하기</button>
                        <button onClick={() => handleMessageMenuAction('READERS')}>이 메시지 읽은 사람</button>
                        <button onClick={() => handleMessageMenuAction('SHARE')}>공유하기</button>
                        <button onClick={() => handleMessageMenuAction('NOTICE')}>공지</button>
                    </div>
                )}

                <div
                    className='chattingBox'
                    ref={chatListRef}
                    onScroll={handleChatScroll}
                >
                    {prevChattings && prevChattings.length > 0 ?
                        prevChattings.map((d) => {
                            if (d.messageType === 'SYSTEM') {
                                return (
                                    <div key={d.messageId} className="systemMessage">
                                        {d.messageText}
                                    </div>
                                );
                            }

                            const sender = memberMap[d.senderPublicId];
                            const senderNickname = sender?.nickname ?? '알 수 없음';
                            const senderProfileImg = sender?.profileImg ?? '/images/mococo_question.png';

                            const isMine = d.senderPublicId === myPublicId;

                            return (
                                <div
                                    key={d.messageId}
                                    className={`chatRow ${isMine ? 'mine' : 'other'}`}
                                    onContextMenu={(e) => openMessageContextMenu(e, d)}
                                >
                                    {!isMine && (
                                        <img
                                            className="senderProfileImg clickableProfileImg"
                                            src={senderProfileImg}
                                            alt={senderNickname}
                                            onClick={() => openProfilePopup(sender)}
                                        />
                                    )}

                                    {isMine && (
                                        <div className='messageInfo'>
                                            <div className='unreadCount'>{d.unreadCount}</div>
                                            <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                        </div>
                                    )}

                                    <div className="messageContent">
                                        {!isMine && (
                                            <div className="senderNickname">{senderNickname}</div>
                                        )}

                                        <div className='messageWrap'>
                                            <div className="messageText">{d.messageText}</div>
                                        </div>
                                    </div>

                                    {!isMine && (
                                        <div className='messageInfo'>
                                            <div className='unreadCount'>{d.unreadCount}</div>
                                            <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                        </div>
                                    )}
                                </div>
                            );
                        })
                        :
                        <div className="emptyChatMessage">친구와 새로운 이야기를 시작해보세요.</div>
                    }

                    <div ref={chatEndRef} />
                </div>

                {typingUsers.length > 0 && (
                    <div className="typingNotice">
                        {typingUsers.map(typingUser => typingUser.nickname).join(', ')}님이 입력 중...
                    </div>
                )}

                <div className='inputChat'>
                    <textarea
                        value={chatMessage}
                        onChange={handleChatMessageChange}
                        placeholder='여기에 메세지 입력...'
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                sendChatMessage();
                            }
                        }}
                    />
                    <button onClick={sendChatMessage}>전송</button>
                </div>
            </div>
        </div>
    );
}

export default ChatBox;