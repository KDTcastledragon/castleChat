import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef, useMemo } from 'react';
import {
    emitWsChangeMemberRole,
    emitWsDeleteMessage,
    emitWsExitRoom,
    emitWsBanMember,
    emitWsInviteMember,
    emitWsKickMember,
    emitWsLeftRoom,
    emitWsReadMessage,
    emitWsReactMessage,
    emitWsSendMessage,
    emitWsStartDirectChat,
    emitWsTypingStart,
    emitWsTypingStop,
    registerRoomHandler,
    unregisterRoomHandler
} from '../../webSocket/wsClient';
import { useMe } from '../../hooks/useAuthUser';
import { useFriendList } from '../../hooks/useFriend';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import { useQueryClient } from '@tanstack/react-query';
import { recommendMessagesApi } from '../../api/aiAssistApi';
import { getMessageReactionMembersApi, getMessageReadersApi, getMessageUnreadCountsApi, loadMessagesInRoomApi, sendFileApi } from '../../api/chatApi';
import { updateMyRoomSettingsApi } from '../../api/roomApi';

function ChatBox({ roomId, isDraft, targetPublicId, roomType, roomName, roomThumbnail, customRoomBackground, messageNotificationEnabled, memberList, x, y, zIndex, exitChatRoom, onMove, onFocus }) {
    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const prevChattingsRef = useRef([]);
    const [typingUsers, setTypingUsers] = useState([]);

    const [isRoomMenuOpen, setIsRoomMenuOpen] = useState(false);
    const [isInviteFriendPanelOpen, setIsInviteFriendPanelOpen] = useState(false);
    const [selectedInviteFriends, setSelectedInviteFriends] = useState([]);
    const [isInvitingMembers, setIsInvitingMembers] = useState(false);

    const [profileTargetMember, setProfileTargetMember] = useState(null);
    const [imageViewerAttachment, setImageViewerAttachment] = useState(null);
    const [messageContextMenu, setMessageContextMenu] = useState(null);
    const [replyTargetMessage, setReplyTargetMessage] = useState(null);
    const [reactionTargetMessage, setReactionTargetMessage] = useState(null);
    const [reactionPickerPosition, setReactionPickerPosition] = useState(null);
    const [reactionViewer, setReactionViewer] = useState(null);
    const [messageReadersViewer, setMessageReadersViewer] = useState(null);
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [isUploadingFiles, setIsUploadingFiles] = useState(false);
    const [isAiRecommendLoading, setIsAiRecommendLoading] = useState(false);
    const [aiRecommendedMessages, setAiRecommendedMessages] = useState([]);
    const [myReactionMap, setMyReactionMap] = useState({});
    const [localRoomName, setLocalRoomName] = useState(roomName ?? '');
    const [roomThumbnailUrl, setRoomThumbnailUrl] = useState(roomThumbnail ?? '');
    const [roomBackgroundUrl, setRoomBackgroundUrl] = useState(customRoomBackground ?? '');
    const [isMessageNotificationEnabled, setIsMessageNotificationEnabled] = useState(messageNotificationEnabled ?? true);
    const [isSavingRoomSettings, setIsSavingRoomSettings] = useState(false);

    const [locallyRemovedMemberPublicIds, setLocallyRemovedMemberPublicIds] = useState(() => new Set());
    const [locallyAddedRoomMembers, setLocallyAddedRoomMembers] = useState([]);
    const [locallyChangedMemberRoles, setLocallyChangedMemberRoles] = useState({});

    const queryClient = useQueryClient();
    const { getOrCreateDirectRoom, openRoom } = useChatRoomActions();

    const { data: me } = useMe();
    const { publicId: myPublicId } = me || {};
    const isDraftDirectRoom = isDraft && roomType === 'DIRECT' && !roomId;

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

        return Array.from(map.values()).map(member => ({
            ...member,
            role: locallyChangedMemberRoles[member.publicId] ?? member.role
        }));
    }, [memberList, locallyAddedRoomMembers, locallyRemovedMemberPublicIds, locallyChangedMemberRoles]);

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

    function canChangeMemberRole(member) {
        if (!member) return false;
        if (roomType !== 'GROUP') return false;
        if (myRoomRole !== 'HOST') return false;
        if (member.publicId === myPublicId) return false;

        return member.role !== 'HOST';
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
        { label: '👍', code: 'like', title: '좋아요' },
        { label: '👎', code: 'dislike', title: '싫어요' },
        { label: '😿', code: 'cat_sad', title: '슬퍼요' },
        { label: '😢', code: 'sad', title: '눈물' },
        { label: '😈', code: 'devil', title: '장난' }
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
        prevChattingsRef.current = prevChattings;
    }, [prevChattings]);

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
        if (isDraftDirectRoom) {
            alert('첫 메시지 파일 첨부는 방 생성 후 전송해주세요.');
            return;
        }

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
        setReactionTargetMessage(null);
        setReactionPickerPosition(null);

        if (!roomId) return;

        const chatListEl = chatListRef.current;
        if (!chatListEl) return;

        if (chatListEl.scrollTop <= LOAD_PREV_THRESHOLD_PX) {
            loadOlderMessages();
        }
    }

    async function syncVisibleMessageUnreadCounts() {
        if (!roomId) return;

        const currentMessages = prevChattingsRef.current ?? [];
        const messageIds = currentMessages
            .map(msg => Number(msg.messageId))
            .filter(messageId => Number.isFinite(messageId));

        if (messageIds.length === 0) return;

        try {
            const unreadCountMap = await getMessageUnreadCountsApi(roomId, messageIds);

            setPrevChattings(prev =>
                prev.map(msg => {
                    const messageId = Number(msg.messageId);
                    const nextUnreadCount = unreadCountMap?.[messageId];

                    if (nextUnreadCount === undefined || nextUnreadCount === null) {
                        return msg;
                    }

                    return {
                        ...msg,
                        unreadCount: Number(nextUnreadCount)
                    };
                })
            );
        } catch (e) {
            console.error('메시지 unreadCount 재동기화 실패', e);
        }
    }

    useEffect(() => {
        isLoadingPrevRef.current = false;
        hasMorePrevRef.current = Boolean(roomId);
        oldestMessageIdRef.current = null;
        isPrependingPrevRef.current = false;

        setPrevChattings([]);
        setTypingUsers([]);
        setIsRoomMenuOpen(false);
        setIsInviteFriendPanelOpen(false);
        setSelectedInviteFriends([]);
        setProfileTargetMember(null);
        setImageViewerAttachment(null);
        setMessageContextMenu(null);
        setReplyTargetMessage(null);
        setReactionTargetMessage(null);
        setReactionPickerPosition(null);
        setReactionViewer(null);
        setMessageReadersViewer(null);
        setSelectedFiles([]);
        setUploadProgress(null);
        setIsUploadingFiles(false);
        setIsAiRecommendLoading(false);
        setAiRecommendedMessages([]);
        setLocallyRemovedMemberPublicIds(new Set());
        setLocallyAddedRoomMembers([]);
        setLocallyChangedMemberRoles({});
        setMyReactionMap({});

        if (!roomId || !myPublicId) return;

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
                    const targets = wsResponse.wsType === "LEFT_ROOM"
                        ? [feed?.requesterPublicId].filter(Boolean)
                        : feed?.targetPublicIds ?? [];

                    setLocallyRemovedMemberPublicIds(prev => {
                        const next = new Set(prev);
                        targets.forEach(publicId => next.add(publicId));
                        return next;
                    });
                }

                if (wsResponse.wsType !== "ROOM_MEMBER_ROLE_CHANGED") {
                    syncVisibleMessageUnreadCounts();
                }

                if (wsResponse.wsType === "ROOM_MEMBER_ROLE_CHANGED") {
                    const targetPublicId = feed?.targetPublicIds?.[0];
                    const targetRole = feed?.targetRole;

                    if (targetPublicId && targetRole) {
                        setLocallyChangedMemberRoles(prev => ({
                            ...prev,
                            [targetPublicId]: targetRole
                        }));
                    }
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

            if (roomId) {
                emitWsTypingStop(roomId);
            }
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        let uploadedAttachments = [];
        const messageType = resolveMessageTypeByFiles(selectedFiles);

        try {
            if (isDraftDirectRoom) {
                if (!targetPublicId) {
                    alert('1:1 채팅 대상 정보가 없습니다.');
                    return;
                }

                if (selectedFiles.length > 0) {
                    alert('첫 메시지 파일 첨부는 방 생성 후 전송해주세요.');
                    return;
                }

                const startResponse = await emitWsStartDirectChat(targetPublicId, chatMessage, {
                    messageType: 'TEXT',
                    replyToMessageId: null,
                    attachmentIds: []
                });

                const startChat = startResponse.payload;

                setChatMessage('');
                setSelectedFiles([]);
                setReplyTargetMessage(null);
                setUploadProgress(null);

                exitChatRoom();
                openRoom(startChat.enterRoomInfo);
                queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
                return;
            }

            if (!roomId) {
                alert('채팅방 정보가 없습니다.');
                return;
            }

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

    async function requestAiRecommendedMessages() {
        if (isAiRecommendLoading) return;

        if (!roomId) {
            alert('AI 추천은 실제 채팅방이 생성된 뒤 사용할 수 있습니다.');
            return;
        }

        try {
            setIsAiRecommendLoading(true);
            const recommendations = await recommendMessagesApi(roomId);

            setAiRecommendedMessages(Array.isArray(recommendations) ? recommendations : []);
        } catch (e) {
            console.error('AI 메시지 추천 실패', e);
            alert(e.response?.data?.message ?? e.response?.data ?? 'AI 메시지 추천 실패');
        } finally {
            setIsAiRecommendLoading(false);
        }
    }

    function applyAiRecommendedMessage(recommendedMessage) {
        setChatMessage(recommendedMessage);
        setAiRecommendedMessages([]);
    }

    const handleChatMessageChange = (e) => {
        const nextValue = e.target.value;

        setChatMessage(nextValue);

        if (!roomId) return;

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
        if (!roomId) return;
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

        if (!roomId) {
            pendingReadMessageIdRef.current = null;
            return;
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

            if (roomId) {
                emitWsTypingStop(roomId);
            }
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        if (roomId) {
            emitWsExitRoom(roomId);
        }

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
                setReactionPickerPosition(null);
                return;
            }

            if (reactionViewer) {
                setReactionViewer(null);
                return;
            }

            if (messageReadersViewer) {
                setMessageReadersViewer(null);
                return;
            }

            if (imageViewerAttachment) {
                closeImageViewer();
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
    }, [messageContextMenu, reactionTargetMessage, reactionViewer, messageReadersViewer, imageViewerAttachment, profileTargetMember, isInviteFriendPanelOpen, isRoomMenuOpen]);

    async function leftRoom() {
        if (!roomId) {
            exitChatRoom();
            return;
        }

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

    async function changeMemberRole(member, targetRole) {
        if (!member) return;
        if (!canChangeMemberRole(member)) return;
        if (member.role === targetRole) return;

        const confirmed = window.confirm(`${member.nickname}님의 권한을 ${targetRole}로 변경하시겠습니까?`);

        if (!confirmed) return;

        try {
            const emitted = emitWsChangeMemberRole(roomId, member.publicId, targetRole);

            if (!emitted) {
                alert('WebSocket 연결 안 됨');
                return;
            }
        } catch (e) {
            console.error('권한 변경 실패', e);
            alert(e.response?.data ?? '권한 변경 실패');
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

    function openImageViewer(attachment) {
        if (!attachment?.fileUrl) return;

        setImageViewerAttachment(attachment);
    }

    function closeImageViewer() {
        setImageViewerAttachment(null);
    }

    async function downloadImageFromViewer() {
        if (!imageViewerAttachment?.fileUrl) return;

        const fileName = imageViewerAttachment.originalFileName || `image-${imageViewerAttachment.attachmentId ?? Date.now()}`;
        const confirmed = window.confirm(`${fileName} 파일을 다운로드하시겠습니까?`);

        if (!confirmed) {
            return;
        }

        try {
            const response = await fetch(imageViewerAttachment.fileUrl);
            const blob = await response.blob();
            const blobUrl = URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = blobUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(blobUrl);
        } catch (e) {
            console.error('이미지 다운로드 실패', e);

            const link = document.createElement('a');
            link.href = imageViewerAttachment.fileUrl;
            link.download = fileName;
            link.target = '_blank';
            document.body.appendChild(link);
            link.click();
            link.remove();
        }
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

    function openReactionPicker(e, message) {
        if (e) {
            e.preventDefault();
            e.stopPropagation();
        }

        const boxRect = chatRoomSectionRef.current?.getBoundingClientRect();
        const clickX = e?.clientX ?? (boxRect ? boxRect.left + 24 : 24);
        const clickY = e?.clientY ?? (boxRect ? boxRect.top + 80 : 80);

        if (boxRect) {
            setReactionPickerPosition({
                left: Math.min(Math.max(clickX - boxRect.left, 8), boxRect.width - 220),
                top: Math.min(Math.max(clickY - boxRect.top, 52), boxRect.height - 120)
            });
        } else {
            setReactionPickerPosition(null);
        }

        setReactionTargetMessage(message);
        setMessageContextMenu(null);
    }

    async function openReactionMemberViewer(message) {
        if (!message?.messageId) return;

        setReactionViewer({
            message,
            members: [],
            selectedCode: null,
            isLoading: true
        });

        try {
            const members = await getMessageReactionMembersApi(roomId, message.messageId);
            const firstCode = members?.[0]?.reactionCode ?? null;

            setReactionViewer({
                message,
                members: members ?? [],
                selectedCode: firstCode,
                isLoading: false
            });
        } catch (e) {
            console.error('리액션 멤버 조회 실패', e);
            alert(e.response?.data ?? '리액션 멤버 조회 실패');
            setReactionViewer(null);
        }
    }

    async function openMessageReadersViewer(message) {
        if (!message?.messageId) return;

        setMessageReadersViewer({
            message,
            readers: [],
            isLoading: true
        });

        try {
            const readers = await getMessageReadersApi(roomId, message.messageId);

            setMessageReadersViewer({
                message,
                readers: readers ?? [],
                isLoading: false
            });
        } catch (e) {
            console.error('읽은 사람 조회 실패', e);
            alert(e.response?.data ?? '읽은 사람 조회 실패');
            setMessageReadersViewer(null);
        }
    }

    function handleMessageMenuAction(action) {
        const targetMessage = messageContextMenu?.message;
        const menuEventPosition = messageContextMenu;

        setMessageContextMenu(null);

        if (!targetMessage) return;

        if (action === 'REACT') {
            setReactionPickerPosition(menuEventPosition ? {
                left: menuEventPosition.left,
                top: menuEventPosition.top
            } : null);
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
            openMessageReadersViewer(targetMessage);
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
        setReactionPickerPosition(null);
    }

    function getReactionLabel(reactionCode) {
        return REACTION_OPTIONS.find(item => item.code === reactionCode)?.label ?? reactionCode;
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
                            title={isDraftDirectRoom ? '첫 메시지 전송 후 메뉴를 사용할 수 있습니다.' : '채팅방 메뉴'}
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={() => {
                                if (isDraftDirectRoom) return;
                                setIsRoomMenuOpen(prev => !prev);
                            }}
                            disabled={isDraftDirectRoom}
                        >
                            <span className="roomMenuButtonIcon">☰</span>
                        </button>
                    </div>

                    <span className="chatRoomTitleText">{localRoomName}</span>

                    <div className="chatTitleRightControls">
                        <button
                            className={`notificationBellButton ${isMessageNotificationEnabled ? 'on' : 'off'}`}
                            title={isDraftDirectRoom ? '첫 메시지 전송 후 알림 설정을 사용할 수 있습니다.' : isMessageNotificationEnabled ? '메시지 알림 켜짐' : '메시지 알림 꺼짐'}
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={toggleMessageNotification}
                            disabled={isDraftDirectRoom}
                        >
                            {isMessageNotificationEnabled ? '🔔' : '🔕'}
                        </button>

                        <button
                            className="chatCloseButton"
                            title="채팅창 닫기"
                            aria-label="채팅창 닫기"
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={closeChatAndExitRoom}
                        >
                            ×
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

                                    {canChangeMemberRole(member) ? (
                                        <details className="memberRoleChangeDetails">
                                            <summary>권한</summary>

                                            <div className="memberRoleChangeMenu">
                                                <button
                                                    className={member.role === 'MEMBER' ? 'active' : ''}
                                                    onClick={() => changeMemberRole(member, 'MEMBER')}
                                                >
                                                    MEMBER
                                                </button>

                                                <button
                                                    className={member.role === 'MANAGER' ? 'active' : ''}
                                                    onClick={() => changeMemberRole(member, 'MANAGER')}
                                                >
                                                    MANAGER
                                                </button>
                                            </div>
                                        </details>
                                    ) : (
                                        <div className="memberRoleChangePlaceholder" />
                                    )}

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
                        style={reactionPickerPosition ? {
                            left: reactionPickerPosition.left,
                            top: reactionPickerPosition.top
                        } : undefined}
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <div className="reactionPickerTitle">리액션</div>

                        <div className="reactionPickerButtons">
                            {REACTION_OPTIONS.map(reaction => (
                                <button
                                    key={reaction.code}
                                    title={reaction.title}
                                    onClick={() => applyReaction(reaction.code)}
                                >
                                    {reaction.label}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {reactionViewer && (
                    <div
                        className="reactionViewerOverlay"
                        onMouseDown={(e) => {
                            e.stopPropagation();
                            setReactionViewer(null);
                        }}
                    >
                        <div
                            className="reactionViewer"
                            onMouseDown={(e) => e.stopPropagation()}
                        >
                            <button className="reactionViewerCloseButton" onClick={() => setReactionViewer(null)}>
                                ×
                            </button>

                            <div className="reactionViewerTitle">리액션</div>

                            {reactionViewer.isLoading ? (
                                <div className="reactionViewerEmpty">불러오는 중...</div>
                            ) : (
                                <>
                                    <div className="reactionViewerTabs">
                                        {REACTION_OPTIONS
                                            .filter(reaction => reactionViewer.members.some(member => member.reactionCode === reaction.code))
                                            .map(reaction => {
                                                const count = reactionViewer.members.filter(member => member.reactionCode === reaction.code).length;

                                                return (
                                                    <button
                                                        key={reaction.code}
                                                        className={reactionViewer.selectedCode === reaction.code ? 'active' : ''}
                                                        title={reaction.title}
                                                        onClick={() => setReactionViewer(prev => ({
                                                            ...prev,
                                                            selectedCode: reaction.code
                                                        }))}
                                                    >
                                                        {reaction.label}
                                                        <span>{count}</span>
                                                    </button>
                                                );
                                            })}
                                    </div>

                                    <div className="reactionViewerCount">
                                        {reactionViewer.members.filter(member => !reactionViewer.selectedCode || member.reactionCode === reactionViewer.selectedCode).length}명
                                    </div>

                                    <div className="reactionViewerMemberList">
                                        {reactionViewer.members
                                            .filter(member => !reactionViewer.selectedCode || member.reactionCode === reactionViewer.selectedCode)
                                            .map(member => (
                                                <div className="reactionViewerMemberItem" key={`${member.reactionCode}-${member.requesterPublicId}`}>
                                                    <img
                                                        src={member.requesterProfileImg || '/images/mococo_question.png'}
                                                        alt={member.requesterNickname}
                                                    />

                                                    <span className="reactionViewerMemberReaction">
                                                        {getReactionLabel(member.reactionCode)}
                                                    </span>

                                                    <span>{member.requesterNickname}</span>
                                                </div>
                                            ))}
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )}

                {messageReadersViewer && (
                    <div
                        className="messageReadersOverlay"
                        onMouseDown={(e) => {
                            e.stopPropagation();
                            setMessageReadersViewer(null);
                        }}
                    >
                        <div
                            className="messageReadersBox"
                            onMouseDown={(e) => e.stopPropagation()}
                        >
                            <button className="messageReadersCloseButton" onClick={() => setMessageReadersViewer(null)}>
                                ×
                            </button>

                            <div className="messageReadersTitle">이 메시지를 읽은 사람</div>

                            {messageReadersViewer.isLoading ? (
                                <div className="messageReadersEmpty">불러오는 중...</div>
                            ) : messageReadersViewer.readers.length > 0 ? (
                                <div className="messageReadersList">
                                    {messageReadersViewer.readers.map(reader => (
                                        <div className="messageReaderItem" key={reader.publicId}>
                                            <img
                                                src={reader.profileImg || '/images/mococo_question.png'}
                                                alt={reader.nickname}
                                            />

                                            <div>
                                                <div className="messageReaderNickname">{reader.nickname}</div>
                                                <div className="messageReaderRole">{reader.role}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="messageReadersEmpty">아직 읽은 사람이 없습니다.</div>
                            )}
                        </div>
                    </div>
                )}

                {imageViewerAttachment && (
                    <div
                        className="imageViewerOverlay"
                        onMouseDown={(e) => {
                            e.stopPropagation();
                            closeImageViewer();
                        }}
                    >
                        <div
                            className="imageViewerBox"
                            onMouseDown={(e) => e.stopPropagation()}
                        >
                            <div className="imageViewerToolbar">
                                <div className="imageViewerFileName">
                                    {imageViewerAttachment.originalFileName || '이미지'}
                                </div>

                                <div className="imageViewerActions">
                                    <button
                                        className="imageViewerDownloadButton"
                                        title="다운로드"
                                        onClick={downloadImageFromViewer}
                                    >
                                        ⭳
                                    </button>

                                    <button
                                        className="imageViewerCloseButton"
                                        onClick={closeImageViewer}
                                    >
                                        ×
                                    </button>
                                </div>
                            </div>

                            <img
                                className="imageViewerImage"
                                src={imageViewerAttachment.fileUrl}
                                alt={imageViewerAttachment.originalFileName || 'image'}
                            />
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

                                    {isMine && !isDeletedMessage && (
                                        <button
                                            className="messageHoverReactionButton"
                                            title="리액션"
                                            onClick={(e) => openReactionPicker(e, d)}
                                        >
                                            ☺+
                                        </button>
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
                                                                    onClick={() => openImageViewer(attachment)}
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
                                            <div className="messageReactionBar">
                                                <button
                                                    className="messageReactionAddButton"
                                                    title="리액션 추가"
                                                    onClick={(e) => openReactionPicker(e, d)}
                                                >
                                                    ☺+
                                                </button>

                                                <button
                                                    className="messageReactionSummaryButton"
                                                    title="리액션 한 사람"
                                                    onClick={() => openReactionMemberViewer(d)}
                                                >
                                                    {reactions.map(reaction => (
                                                        <span className="messageReactionBadge" key={reaction.reactionCode}>
                                                            {getReactionLabel(reaction.reactionCode)}
                                                            {` ${Number(reaction.count ?? 0)}`}
                                                        </span>
                                                    ))}
                                                </button>
                                            </div>
                                        )}
                                    </div>

                                    {!isMine && !isDeletedMessage && (
                                        <button
                                            className="messageHoverReactionButton"
                                            title="리액션"
                                            onClick={(e) => openReactionPicker(e, d)}
                                        >
                                            ☺+
                                        </button>
                                    )}

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

                <div className="aiRecommendBox">
                    <button
                        className="aiRecommendButton"
                        onClick={requestAiRecommendedMessages}
                        disabled={isAiRecommendLoading || isUploadingFiles}
                    >
                        {isAiRecommendLoading ? 'AI 추천 받는 중...' : 'AI에게 메시지 추천받기'}
                    </button>

                    {aiRecommendedMessages.length > 0 && (
                        <div className="aiRecommendList">
                            {aiRecommendedMessages.map((recommendedMessage, index) => (
                                <button
                                    key={`${recommendedMessage}-${index}`}
                                    className="aiRecommendItem"
                                    onClick={() => applyAiRecommendedMessage(recommendedMessage)}
                                    title="클릭하면 입력창에 들어갑니다."
                                >
                                    {recommendedMessage}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

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
                        onClick={() => {
                            if (isDraftDirectRoom) {
                                alert('첫 메시지 파일 첨부는 방 생성 후 전송해주세요.');
                                return;
                            }

                            fileInputRef.current?.click();
                        }}
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
