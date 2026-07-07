import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef, useMemo } from 'react';
import {
    emitWsDeleteMessage,
    emitWsExitRoom,
    emitWsBanMember,
    emitWsInviteMember,
    emitWsKickMember,
    emitWsLeftRoom,
    emitWsReadMessage,
    emitWsReactMessage,
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
import { loadMessagesInRoomApi, sendFileApi } from '../../api/chatApi';
import { updateMyRoomSettingsApi } from '../../api/roomApi';

function ChatBox({ roomId, roomType, roomName, roomThumbnail, customRoomBackground, messageNotificationEnabled, memberList, x, y, zIndex, exitChatRoom, onMove, onFocus }) {
    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const [typingUsers, setTypingUsers] = useState([]);

    const [isRoomMenuOpen, setIsRoomMenuOpen] = useState(false);
    const [isInviteFriendPanelOpen, setIsInviteFriendPanelOpen] = useState(false);
    const [selectedInviteFriends, setSelectedInviteFriends] = useState([]);
    const [isInvitingMembers, setIsInvitingMembers] = useState(false);

    const [profileTargetMember, setProfileTargetMember] = useState(null);
    const [messageContextMenu, setMessageContextMenu] = useState(null);
    const [replyTargetMessage, setReplyTargetMessage] = useState(null);
    const [reactionTargetMessage, setReactionTargetMessage] = useState(null);
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [isUploadingFiles, setIsUploadingFiles] = useState(false);
    const [myReactionMap, setMyReactionMap] = useState({});
    const [localRoomName, setLocalRoomName] = useState(roomName ?? '');
    const [roomThumbnailUrl, setRoomThumbnailUrl] = useState(roomThumbnail ?? '');
    const [roomBackgroundUrl, setRoomBackgroundUrl] = useState(customRoomBackground ?? '');
    const [isMessageNotificationEnabled, setIsMessageNotificationEnabled] = useState(messageNotificationEnabled ?? true);
    const [isSavingRoomSettings, setIsSavingRoomSettings] = useState(false);

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

    const messageByIdMap = useMemo(() => {
        const map = new Map();

        prevChattings.forEach(message => {
            map.set(Number(message.messageId), message);
        });

        return map;
    }, [prevChattings]);

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

    function canBanMember(member) {
        if (!canKickMember(member)) return false;

        if (myRoomRole === 'HOST') {
            return member.role !== 'HOST';
        }

        return myRoomRole === 'MANAGER' && member.role === 'MEMBER';
    }

    const chatRoomSectionRef = useRef(null);
    const isChatBoxFocusedRef = useRef(false);

    const chatEndRef = useRef(null);
    const fileInputRef = useRef(null);
    const roomThumbnailInputRef = useRef(null);
    const roomBackgroundInputRef = useRef(null);

    const isTypingRef = useRef(false);
    const typingTimerRef = useRef(null);

    const readerReadPositionsRef = useRef({});
    const pendingReadMessageIdRef = useRef(null);
    const readDebounceTimerRef = useRef(null);

    const MESSAGE_PAGE_SIZE = 50;
    const LOAD_PREV_THRESHOLD_PX = 80;
    const MAX_UPLOAD_SIZE = 320 * 1024 * 1024;
    const REACTION_OPTIONS = [
        { label: '좋아요', code: 'like' },
        { label: '싫어요', code: 'dislike' },
        { label: '슬퍼요', code: 'sad' }
    ];

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

    useEffect(() => {
        setLocalRoomName(roomName ?? '');
    }, [roomName]);

    useEffect(() => {
        setRoomThumbnailUrl(roomThumbnail ?? '');
    }, [roomThumbnail]);

    useEffect(() => {
        setRoomBackgroundUrl(customRoomBackground ?? '');
    }, [customRoomBackground]);

    useEffect(() => {
        setIsMessageNotificationEnabled(messageNotificationEnabled ?? true);
    }, [messageNotificationEnabled]);

    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };

    function formatFileSize(size) {
        if (size >= 1024 * 1024) {
            return `${(size / 1024 / 1024).toFixed(1)}MB`;
        }

        if (size >= 1024) {
            return `${(size / 1024).toFixed(1)}KB`;
        }

        return `${size}B`;
    }

    function getSelectedFilesTotalSize(files = selectedFiles) {
        return files.reduce((sum, file) => sum + file.size, 0);
    }

    function isEmptyMessage(messageText, files = selectedFiles) {
        return messageText.trim().length === 0 && files.length === 0;
    }

    function resolveMessageTypeByFiles(files) {
        if (!files || files.length === 0) return 'TEXT';

        const firstFile = files[0];
        const contentType = firstFile.type ?? '';

        if (contentType.startsWith('image/')) return 'IMAGE';
        if (contentType.startsWith('video/')) return 'VIDEO';
        if (contentType.startsWith('audio/')) return 'AUDIO';

        return 'FILE';
    }

    function addSelectedFiles(fileList) {
        const incomingFiles = Array.from(fileList ?? []);
        if (incomingFiles.length === 0) return;

        setSelectedFiles(prev => {
            const nextFiles = [...prev, ...incomingFiles];
            const nextTotalSize = getSelectedFilesTotalSize(nextFiles);

            if (nextTotalSize > MAX_UPLOAD_SIZE) {
                alert(`첨부파일은 한 번에 최대 320MB까지만 올릴 수 있습니다. 현재: ${formatFileSize(nextTotalSize)}`);
                return prev;
            }

            return nextFiles;
        });
    }

    function removeSelectedFile(index) {
        setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    }

    function handleFileInputChange(e) {
        addSelectedFiles(e.target.files);
        e.target.value = '';
    }

    function handleChatDrop(e) {
        e.preventDefault();
        e.stopPropagation();

        addSelectedFiles(e.dataTransfer.files);
    }

    function handleChatDragOver(e) {
        e.preventDefault();
    }

    function handlePasteFiles(e) {
        const pastedFiles = Array.from(e.clipboardData?.files ?? []);

        if (pastedFiles.length === 0) return;

        addSelectedFiles(pastedFiles);
    }

    async function uploadRoomSettingImage(e, target) {
        const file = e.target.files?.[0];
        e.target.value = '';

        if (!file) return;

        if (file.size > MAX_UPLOAD_SIZE) {
            alert('320MB 이하 파일만 올릴 수 있습니다.');
            return;
        }

        try {
            const uploaded = await sendFileApi(roomId, [file]);
            const fileUrl = uploaded?.[0]?.fileUrl;

            if (!fileUrl) {
                alert('파일 URL을 받을 수 없습니다.');
                return;
            }

            if (target === 'THUMBNAIL') {
                setRoomThumbnailUrl(fileUrl);
            }

            if (target === 'BACKGROUND') {
                setRoomBackgroundUrl(fileUrl);
            }
        } catch (err) {
            console.error('방 설정 이미지 업로드 실패', err);
            alert(err.response?.data ?? '이미지 업로드 실패');
        }
    }

    async function saveRoomSettings(nextNotificationEnabled = isMessageNotificationEnabled) {
        try {
            setIsSavingRoomSettings(true);

            await updateMyRoomSettingsApi({
                roomId,
                customRoomName: localRoomName,
                customRoomThumbnail: roomThumbnailUrl,
                customRoomBackground: roomBackgroundUrl,
                messageNotificationEnabled: nextNotificationEnabled
            });

            queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
        } catch (err) {
            console.error('방 설정 저장 실패', err);
            alert(err.response?.data ?? '방 설정 저장 실패');
        } finally {
            setIsSavingRoomSettings(false);
        }
    }

    async function toggleMessageNotification() {
        const nextEnabled = !isMessageNotificationEnabled;
        setIsMessageNotificationEnabled(nextEnabled);
        await saveRoomSettings(nextEnabled);
    }

    function buildUploadProgressText() {
        if (!uploadProgress) return '';

        return `${formatFileSize(uploadProgress.loaded)} / ${formatFileSize(uploadProgress.total)}`;
    }

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
        setReplyTargetMessage(null);
        setReactionTargetMessage(null);
        setSelectedFiles([]);
        setUploadProgress(null);
        setIsUploadingFiles(false);
        setLocallyRemovedMemberPublicIds(new Set());
        setLocallyAddedRoomMembers([]);
        setMyReactionMap({});

        const initChatRoom = async () => {
            try {
                // const loadedMessagesInRoom = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
                //     params: {
                //         limit: MESSAGE_PAGE_SIZE
                //     }
                // });

                const messages = await loadMessagesInRoomApi(roomId, MESSAGE_PAGE_SIZE);

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

            if (wsResponse.wsType === "MSG_DELETED") {
                const deletedMessage = wsResponse.payload;

                setPrevChattings(prev =>
                    prev.map(msg => {
                        if (Number(msg.messageId) !== Number(deletedMessage.messageId)) {
                            return msg;
                        }

                        return {
                            ...msg,
                            messageStatus: deletedMessage.messageStatus ?? 'DELETED',
                            messageText: '삭제된 메시지입니다.',
                            deletedAt: deletedMessage.deletedAt
                        };
                    })
                );
            }

            if (wsResponse.wsType === "REACT_EVENTED" || wsResponse.wsType === "MSG_REACTION_UPDATED") {
                const reaction = wsResponse.payload;

                if (reaction.requesterPublicId === myPublicId) {
                    const reactionKey = `${reaction.messageId}:${reaction.reactionCode}`;

                    setMyReactionMap(prev => ({
                        ...prev,
                        [reactionKey]: reaction.added
                    }));
                }

                setPrevChattings(prev =>
                    prev.map(msg => {
                        if (Number(msg.messageId) !== Number(reaction.messageId)) {
                            return msg;
                        }

                        const currentReactions = Array.isArray(msg.reactions) ? msg.reactions : [];
                        const foundReaction = currentReactions.find(item => item.reactionCode === reaction.reactionCode);

                        let nextReactions;

                        if (reaction.added) {
                            if (foundReaction) {
                                nextReactions = currentReactions.map(item =>
                                    item.reactionCode === reaction.reactionCode
                                        ? { ...item, count: Number(item.count ?? 0) + 1 }
                                        : item
                                );
                            } else {
                                nextReactions = [
                                    ...currentReactions,
                                    {
                                        reactionType: reaction.reactionType,
                                        reactionCode: reaction.reactionCode,
                                        count: 1
                                    }
                                ];
                            }
                        } else {
                            nextReactions = currentReactions
                                .map(item =>
                                    item.reactionCode === reaction.reactionCode
                                        ? { ...item, count: Math.max(Number(item.count ?? 0) - 1, 0) }
                                        : item
                                )
                                .filter(item => Number(item.count ?? 0) > 0);
                        }

                        return {
                            ...msg,
                            reactions: nextReactions
                        };
                    })
                );
            }

            if (
                wsResponse.wsType === "ROOM_MEMBER_INVITED" ||
                wsResponse.wsType === "ROOM_MEMBER_KICKED" ||
                wsResponse.wsType === "ROOM_MEMBER_BANNED" ||
                wsResponse.wsType === "ROOM_MEMBER_ROLE_CHANGED" ||
                wsResponse.wsType === "LEFT_ROOM"
            ) {
                const feed = wsResponse.payload;

                if (feed?.feedText) {
                    setPrevChattings(prev => [
                        ...prev,
                        {
                            messageId: `feed-${Date.now()}-${Math.random()}`,
                            messageType: 'SYSTEM',
                            messageText: feed.feedText
                        }
                    ]);
                }

                if (wsResponse.wsType === "ROOM_MEMBER_KICKED" || wsResponse.wsType === "ROOM_MEMBER_BANNED" || wsResponse.wsType === "LEFT_ROOM") {
                    const targets = feed?.targetPublicIds ?? [];

                    setLocallyRemovedMemberPublicIds(prev => {
                        const next = new Set(prev);
                        targets.forEach(publicId => next.add(publicId));
                        return next;
                    });
                }
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

            if (wsResponse.wsType === "ROOM_NOTICE" || wsResponse.wsType === "ROOM_NOTICE_APPLIED") {
                const notice = wsResponse.payload;
                const feed = notice.roomFeed ?? notice;

                setPrevChattings(prev => [
                    ...prev,
                    {
                        messageId: `notice-${crypto.randomUUID()}`,
                        roomId: feed.roomId,
                        messageText: feed.feedText ?? notice.message,
                        messageType: 'SYSTEM',
                        createdAt: feed.feedAt ?? notice.createdAt
                    }
                ]);
            }
        });

        return () => {
            flushPendingReadMessage();
            unregisterRoomHandler(roomId);
        };
    }, [roomId, myPublicId]);

    async function sendChatMessage() {
        if (isUploadingFiles) return;

        if (isEmptyMessage(chatMessage)) {
            return;
        }

        if (isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        let uploadedAttachments = [];
        const messageType = resolveMessageTypeByFiles(selectedFiles);

        try {
            if (selectedFiles.length > 0) {
                setIsUploadingFiles(true);
                setUploadProgress({
                    loaded: 0,
                    total: getSelectedFilesTotalSize(),
                    percent: 0
                });

                uploadedAttachments = await sendFileApi(roomId, selectedFiles, (progressEvent) => {
                    const loaded = progressEvent.loaded ?? 0;
                    const total = progressEvent.total ?? getSelectedFilesTotalSize();
                    const percent = total > 0 ? Math.round((loaded / total) * 100) : 0;

                    setUploadProgress({
                        loaded,
                        total,
                        percent
                    });
                });
            }

            const attachmentIds = uploadedAttachments.map(attachment => attachment.attachmentId);

            const isEmitted = emitWsSendMessage(roomId, chatMessage, {
                messageType,
                replyToMessageId: replyTargetMessage?.messageId,
                attachmentIds
            });

            if (!isEmitted) {
                console.log(`!isEmitted`);
                return;
            }

            setChatMessage('');
            setSelectedFiles([]);
            setReplyTargetMessage(null);
            setUploadProgress(null);
        } catch (e) {
            console.error('파일/메시지 전송 실패', e);
            alert(e.response?.data ?? '메시지 전송 실패');
        } finally {
            setIsUploadingFiles(false);
        }
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

            if (!e.target.closest('.messageContextMenu') && !e.target.closest('.reactionPicker')) {
                setMessageContextMenu(null);
                setReactionTargetMessage(null);
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

            if (reactionTargetMessage) {
                setReactionTargetMessage(null);
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
    }, [messageContextMenu, reactionTargetMessage, profileTargetMember, isInviteFriendPanelOpen, isRoomMenuOpen]);

    async function leftRoom() {
        try {
            flushPendingReadMessage();

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

        const confirmed = window.confirm(`${member.nickname}님을 추방하시겠습니까?`);

        if (!confirmed) {
            return;
        }

        try {
            const emitted = emitWsKickMember(roomId, member.publicId);

            if (!emitted) {
                alert('WebSocket 연결 안 됨');
                return;
            }

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

    async function banMember(member) {
        if (!member) return;

        const firstConfirmed = window.confirm(`${member.nickname}님을 영구강퇴하시겠습니까?`);
        if (!firstConfirmed) return;

        const secondConfirmed = window.confirm('영구강퇴는 재초대가 제한될 수 있습니다. 정말 진행할까요?');
        if (!secondConfirmed) return;

        try {
            const emitted = emitWsBanMember(roomId, member.publicId);

            if (!emitted) {
                alert('WebSocket 연결 안 됨');
                return;
            }

            setLocallyRemovedMemberPublicIds(prev => {
                const next = new Set(prev);
                next.add(member.publicId);
                return next;
            });
        } catch (e) {
            console.error('영구강퇴 실패', e);
            alert(e.response?.data ?? '영구강퇴 실패');
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

            const emitted = emitWsInviteMember(roomId, inviteTargetMemberPublicIds);

            if (!emitted) {
                alert('WebSocket 연결 안 됨');
                return;
            }

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

        if (action === 'REACT') {
            setReactionTargetMessage(targetMessage);
            return;
        }

        if (action === 'REPLY') {
            setReplyTargetMessage(targetMessage);
            return;
        }

        if (action === 'DELETE') {
            const confirmed = window.confirm('이 메시지를 삭제하시겠습니까?');

            if (confirmed) {
                emitWsDeleteMessage(roomId, targetMessage.messageId);
            }

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

    function applyReaction(reactionCode) {
        if (!reactionTargetMessage) return;

        const reactionKey = `${reactionTargetMessage.messageId}:${reactionCode}`;
        const addRequested = !myReactionMap[reactionKey];

        emitWsReactMessage(roomId, reactionTargetMessage.messageId, reactionCode, addRequested);
        setReactionTargetMessage(null);
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
                onDrop={handleChatDrop}
                onDragOver={handleChatDragOver}
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

                    <span className="chatRoomTitleText">{localRoomName}</span>

                    <div className="chatTitleRightControls">
                        <button
                            className={`notificationBellButton ${isMessageNotificationEnabled ? 'on' : 'off'}`}
                            title={isMessageNotificationEnabled ? '메시지 알림 켜짐' : '메시지 알림 꺼짐'}
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={toggleMessageNotification}
                        >
                            {isMessageNotificationEnabled ? '🔔' : '🔕'}
                        </button>

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

                    <div className="roomSettingsBox">
                        <div className="roomSidePanelSubTitle">방 설정</div>

                        <label className="roomSettingLabel">
                            방 이름
                            <input
                                value={localRoomName}
                                onChange={(e) => setLocalRoomName(e.target.value)}
                                placeholder="방 이름"
                            />
                        </label>

                        <div className="roomImageSettingRow">
                            <button onClick={() => roomThumbnailInputRef.current?.click()}>
                                썸네일 사진
                            </button>
                            <span>{roomThumbnailUrl ? '선택됨' : '기본 이미지'}</span>
                            <input
                                ref={roomThumbnailInputRef}
                                type="file"
                                accept="image/*"
                                hidden
                                onChange={(e) => uploadRoomSettingImage(e, 'THUMBNAIL')}
                            />
                        </div>

                        <div className="roomImageSettingRow">
                            <button onClick={() => roomBackgroundInputRef.current?.click()}>
                                배경 사진
                            </button>
                            <span>{roomBackgroundUrl ? '선택됨' : '기본 배경'}</span>
                            <input
                                ref={roomBackgroundInputRef}
                                type="file"
                                accept="image/*"
                                hidden
                                onChange={(e) => uploadRoomSettingImage(e, 'BACKGROUND')}
                            />
                        </div>

                        <button
                            className="saveRoomSettingsButton"
                            onClick={() => saveRoomSettings()}
                            disabled={isSavingRoomSettings}
                        >
                            방 설정 저장
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
                                        <div className="memberDangerActions">
                                            <button
                                                className="kickMemberButton"
                                                onClick={() => kickMember(member)}
                                            >
                                                추방
                                            </button>

                                            {canBanMember(member) && (
                                                <details className="banMemberDetails">
                                                    <summary>더보기</summary>
                                                    <button
                                                        className="banMemberButton"
                                                        onClick={() => banMember(member)}
                                                    >
                                                        영구강퇴
                                                    </button>
                                                </details>
                                            )}
                                        </div>
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
                        <button onClick={() => handleMessageMenuAction('REACT')}>리액션</button>
                        <button onClick={() => handleMessageMenuAction('REPLY')}>답장하기</button>
                        <button onClick={() => handleMessageMenuAction('DELETE')}>삭제하기</button>
                        <button onClick={() => handleMessageMenuAction('READERS')}>이 메시지 읽은 사람</button>
                        <button onClick={() => handleMessageMenuAction('SHARE')}>공유하기</button>
                        <button onClick={() => handleMessageMenuAction('NOTICE')}>공지</button>
                    </div>
                )}

                {reactionTargetMessage && (
                    <div
                        className="reactionPicker"
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <div className="reactionPickerTitle">리액션</div>

                        <div className="reactionPickerButtons">
                            {REACTION_OPTIONS.map(reaction => (
                                <button
                                    key={reaction.code}
                                    onClick={() => applyReaction(reaction.code)}
                                >
                                    {reaction.label}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                <div
                    className='chattingBox'
                    ref={chatListRef}
                    onScroll={handleChatScroll}
                    style={roomBackgroundUrl ? {
                        backgroundImage: `linear-gradient(rgba(255,255,255,0.72), rgba(255,255,255,0.72)), url(${roomBackgroundUrl})`,
                        backgroundSize: 'cover',
                        backgroundPosition: 'center'
                    } : undefined}
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
                            const isDeletedMessage = d.messageStatus === 'DELETED';
                            const replyMessage = d.replyToMessageId
                                ? messageByIdMap.get(Number(d.replyToMessageId))
                                : null;
                            const attachments = Array.isArray(d.attachments) ? d.attachments : [];
                            const reactions = Array.isArray(d.reactions) ? d.reactions : [];

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

                                        <div className={`messageWrap ${isDeletedMessage ? 'deleted' : ''}`}>
                                            {replyMessage && !isDeletedMessage && (
                                                <div className="replyPreviewInMessage">
                                                    <div className="replyPreviewSender">
                                                        {memberMap[replyMessage.senderPublicId]?.nickname ?? '답장'}
                                                    </div>
                                                    <div className="replyPreviewText">
                                                        {replyMessage.messageText || '첨부 메시지'}
                                                    </div>
                                                </div>
                                            )}

                                            {attachments.length > 0 && !isDeletedMessage && (
                                                <div className="attachmentPreviewList">
                                                    {attachments.map(attachment => (
                                                        <div className="attachmentPreviewItem" key={attachment.attachmentId}>
                                                            {attachment.attachmentKind === 'IMAGE' ? (
                                                                <img
                                                                    className="attachmentImagePreview"
                                                                    src={attachment.fileUrl}
                                                                    alt={attachment.originalFileName}
                                                                />
                                                            ) : attachment.attachmentKind === 'VIDEO' ? (
                                                                <video
                                                                    className="attachmentVideoPreview"
                                                                    src={attachment.fileUrl}
                                                                    controls
                                                                />
                                                            ) : (
                                                                <a
                                                                    className="attachmentFilePreview"
                                                                    href={attachment.fileUrl}
                                                                    target="_blank"
                                                                    rel="noreferrer"
                                                                >
                                                                    {attachment.originalFileName}
                                                                </a>
                                                            )}
                                                        </div>
                                                    ))}
                                                </div>
                                            )}

                                            <div className="messageText">
                                                {isDeletedMessage ? '삭제된 메시지입니다.' : d.messageText}
                                            </div>
                                        </div>

                                        {reactions.length > 0 && (
                                            <div className="messageReactionList">
                                                {reactions.map(reaction => (
                                                    <span className="messageReactionBadge" key={reaction.reactionCode}>
                                                        {REACTION_OPTIONS.find(item => item.code === reaction.reactionCode)?.label ?? reaction.reactionCode}
                                                        {Number(reaction.count ?? 0) > 1 ? ` ${reaction.count}` : ''}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
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

                {(replyTargetMessage || selectedFiles.length > 0 || uploadProgress) && (
                    <div className="composerMetaPanel">
                        {replyTargetMessage && (
                            <div className="replyComposerBar">
                                <div className="replyComposerTextBox">
                                    <div className="replyComposerTitle">
                                        {memberMap[replyTargetMessage.senderPublicId]?.nickname ?? '상대'}에게 답장
                                    </div>
                                    <div className="replyComposerText">
                                        {replyTargetMessage.messageText || '첨부 메시지'}
                                    </div>
                                </div>

                                <button
                                    className="replyComposerCloseButton"
                                    onClick={() => setReplyTargetMessage(null)}
                                >
                                    ×
                                </button>
                            </div>
                        )}

                        {selectedFiles.length > 0 && (
                            <div className="selectedFileList">
                                {selectedFiles.map((file, index) => (
                                    <div className="selectedFileItem" key={`${file.name}-${index}`}>
                                        <span className="selectedFileName">{file.name}</span>
                                        <span className="selectedFileSize">{formatFileSize(file.size)}</span>
                                        <button onClick={() => removeSelectedFile(index)}>삭제</button>
                                    </div>
                                ))}
                            </div>
                        )}

                        {uploadProgress && (
                            <div className="uploadProgressBox">
                                <div
                                    className="uploadProgressCircle"
                                    style={{
                                        background: `conic-gradient(#fee500 ${uploadProgress.percent * 3.6}deg, #e9e9e9 0deg)`
                                    }}
                                >
                                    <span>{uploadProgress.percent}%</span>
                                </div>

                                <div className="uploadProgressText">
                                    {buildUploadProgressText()}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                <div className='inputChat'>
                    <input
                        ref={fileInputRef}
                        type="file"
                        multiple
                        hidden
                        onChange={handleFileInputChange}
                    />

                    <button
                        className="attachFileButton"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={isUploadingFiles}
                    >
                        +
                    </button>

                    <textarea
                        value={chatMessage}
                        onChange={handleChatMessageChange}
                        onPaste={handlePasteFiles}
                        placeholder='여기에 메세지 입력...'
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                sendChatMessage();
                            }
                        }}
                    />
                    <button onClick={sendChatMessage} disabled={isUploadingFiles}>
                        {isUploadingFiles ? '전송 중' : '전송'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ChatBox;
