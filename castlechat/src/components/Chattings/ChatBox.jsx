import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef, useMemo } from 'react';
import {
    emitWsApplyRoomNotice,
    emitWsChangeMemberRole,
    emitWsDeleteMessage,
    emitWsExitRoom,
    emitWsInviteMember,
    emitWsKickMember,
    emitWsLeftRoom,
    emitWsReadMessage,
    emitWsReactMessage,
    emitWsSendMessage,
    emitWsStartDirectChat,
    emitWsStartGroupChat,
    emitWsTypingStart,
    emitWsTypingStop,
    registerRoomHandler,
    unregisterRoomHandler
} from '../../webSocket/wsClient';
import { useMe } from '../../hooks/useAuthUser';
import { useFriendList } from '../../hooks/useFriend';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import { useQueryClient } from '@tanstack/react-query';
import { recommendMessagesApi, recommendPersonalizedMessagesApi, refineMessageToneApi } from '../../api/aiAssistApi';
import { getMessageReactionMembersApi, getMessageReadersApi, getMessageUnreadCountsApi, loadMessagesInRoomApi, sendFileApi, uploadImageApi } from '../../api/chatApi';
import { loadRoomNoticesApi, updateMyRoomSettingsApi } from '../../api/roomApi';

function ChatBox({ roomId, isDraft, targetPublicId, inviteMemberPublicIds = [], roomType, roomName, roomThumbnail, customRoomBackground, messageNotificationEnabled, roomNotice, memberList, initialMessages, isDocked = false, x, y, zIndex, exitChatRoom, onMove, onFocus }) {
    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const prevChattingsRef = useRef([]);
    const [typingUsers, setTypingUsers] = useState([]);

    const [isRoomMenuOpen, setIsRoomMenuOpen] = useState(false);
    const [isChatSearchOpen, setIsChatSearchOpen] = useState(false);
    const [chatSearchWord, setChatSearchWord] = useState('');
    const [chatSearchResultIndex, setChatSearchResultIndex] = useState(-1);
    const [isInviteFriendPanelOpen, setIsInviteFriendPanelOpen] = useState(false);
    const [selectedInviteFriends, setSelectedInviteFriends] = useState([]);
    const [isInvitingMembers, setIsInvitingMembers] = useState(false);

    const [profileTargetMember, setProfileTargetMember] = useState(null);
    const [roleChangeResultModal, setRoleChangeResultModal] = useState(null);
    const [confirmModal, setConfirmModal] = useState(null);
    const [imageViewerAttachment, setImageViewerAttachment] = useState(null);
    const [messageContextMenu, setMessageContextMenu] = useState(null);
    const [replyTargetMessage, setReplyTargetMessage] = useState(null);
    const [reactionTargetMessage, setReactionTargetMessage] = useState(null);
    const [reactionPickerPosition, setReactionPickerPosition] = useState(null);
    const [reactionViewer, setReactionViewer] = useState(null);
    const [messageReadersViewer, setMessageReadersViewer] = useState(null);
    const [highlightedMessageId, setHighlightedMessageId] = useState(null);
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [isUploadingFiles, setIsUploadingFiles] = useState(false);
    const [isStartingChat, setIsStartingChat] = useState(false);
    const startChatRequestLockRef = useRef(false);
    const [isAiRecommendLoading, setIsAiRecommendLoading] = useState(false);
    const [aiRecommendedMessages, setAiRecommendedMessages] = useState([]);
    const [isToneRefinePanelOpen, setIsToneRefinePanelOpen] = useState(false);
    const [refiningTone, setRefiningTone] = useState(null);
    const [toneRefineResultModal, setToneRefineResultModal] = useState(null);
    const [isPersonalizedRecommendOpen, setIsPersonalizedRecommendOpen] = useState(false);
    const [personalizedTargetPublicId, setPersonalizedTargetPublicId] = useState('');
    const [isPersonalizedRecommendLoading, setIsPersonalizedRecommendLoading] = useState(false);
    const [myReactionMap, setMyReactionMap] = useState({});
    const [localRoomName, setLocalRoomName] = useState(roomName ?? '');
    const [roomThumbnailUrl, setRoomThumbnailUrl] = useState(roomThumbnail ?? '');
    const [roomBackgroundUrl, setRoomBackgroundUrl] = useState(customRoomBackground ?? '');
    const [settingRoomName, setSettingRoomName] = useState(roomName ?? '');
    const [settingRoomThumbnailUrl, setSettingRoomThumbnailUrl] = useState(roomThumbnail ?? '');
    const [settingRoomBackgroundUrl, setSettingRoomBackgroundUrl] = useState(customRoomBackground ?? '');
    const [settingRoomThumbnailFileName, setSettingRoomThumbnailFileName] = useState('');
    const [settingRoomBackgroundFileName, setSettingRoomBackgroundFileName] = useState('');
    const [isRoomNameEditing, setIsRoomNameEditing] = useState(false);
    const [roomSettingsToast, setRoomSettingsToast] = useState('');
    const [isMessageNotificationEnabled, setIsMessageNotificationEnabled] = useState(messageNotificationEnabled ?? true);
    const [currentRoomNotice, setCurrentRoomNotice] = useState(roomNotice ?? null);
    const [isRoomNoticeVisible, setIsRoomNoticeVisible] = useState(true);
    const [isRoomNoticePanelOpen, setIsRoomNoticePanelOpen] = useState(false);
    const [roomNoticeList, setRoomNoticeList] = useState([]);
    const [isLoadingRoomNotices, setIsLoadingRoomNotices] = useState(false);
    const [hasMoreRoomNotices, setHasMoreRoomNotices] = useState(true);
    const [editingRoomNoticeId, setEditingRoomNoticeId] = useState(null);
    const [editingRoomNoticeContents, setEditingRoomNoticeContents] = useState('');
    const [isWritingRoomNotice, setIsWritingRoomNotice] = useState(false);
    const [newRoomNoticeContents, setNewRoomNoticeContents] = useState('');
    const [isSavingRoomSettings, setIsSavingRoomSettings] = useState(false);
    const [isScrolledAwayFromBottom, setIsScrolledAwayFromBottom] = useState(false);
    const [newMessageWhileScrolled, setNewMessageWhileScrolled] = useState(null);

    const [locallyRemovedMemberPublicIds, setLocallyRemovedMemberPublicIds] = useState(() => new Set());
    const [locallyAddedRoomMembers, setLocallyAddedRoomMembers] = useState([]);
    const [locallyChangedMemberRoles, setLocallyChangedMemberRoles] = useState({});

    const queryClient = useQueryClient();
    const { getOrCreateDirectRoom, openRoom } = useChatRoomActions();

    const { data: me } = useMe();
    const { publicId: myPublicId } = me || {};
    const isDraftDirectRoom = isDraft && roomType === 'DIRECT' && !roomId;
    const isDraftGroupRoom = isDraft && roomType === 'GROUP' && !roomId;
    const isDraftRoom = isDraft && !roomId;

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

    const messageSenderMap = useMemo(() => {
        const map = {};

        (memberList ?? []).forEach(member => {
            map[member.publicId] = member;
        });

        locallyAddedRoomMembers.forEach(member => {
            map[member.publicId] = member;
        });

        return map;
    }, [memberList, locallyAddedRoomMembers]);

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

    const chatSearchResults = useMemo(() => {
        const searchWord = chatSearchWord.trim().toLocaleLowerCase();

        if (!searchWord) return [];

        return prevChattings.filter(message =>
            message.messageType !== 'SYSTEM'
            && message.messageStatus !== 'DELETED'
            && (message.messageText ?? '').toLocaleLowerCase().includes(searchWord)
        ).reverse();
    }, [chatSearchWord, prevChattings]);

    const personalizedCandidateMembers = useMemo(() => visibleRoomMembers.filter(member => member.publicId !== myPublicId), [visibleRoomMembers, myPublicId]);
    const isAiProcessing = isAiRecommendLoading || isPersonalizedRecommendLoading || Boolean(refiningTone);

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

    function canChangeMemberRole(member) {
        if (!member) return false;
        if (roomType !== 'GROUP') return false;
        if (myRoomRole !== 'HOST') return false;
        if (member.publicId === myPublicId) return false;

        return member.role !== 'HOST';
    }

    const chatRoomSectionRef = useRef(null);
    const isChatBoxFocusedRef = useRef(false);

    const fileInputRef = useRef(null);
    const chatSearchInputRef = useRef(null);
    const roomThumbnailInputRef = useRef(null);
    const roomBackgroundInputRef = useRef(null);

    const isTypingRef = useRef(false);
    const typingTimerRef = useRef(null);

    const readerReadPositionsRef = useRef({});
    const pendingReadMessageIdRef = useRef(null);
    const readDebounceTimerRef = useRef(null);

    const MESSAGE_PAGE_SIZE = 50;
    const ROOM_NOTICE_PAGE_SIZE = 20;
    const LOAD_PREV_THRESHOLD_PX = 80;
    const MAX_UPLOAD_SIZE = 320 * 1024 * 1024;
    const REACTION_OPTIONS = [
        { label: '👍', code: 'like', title: '좋아요' },
        { label: '👎', code: 'dislike', title: '싫어요' },
        { label: '❤️', code: 'heart', title: '하트' },
        { label: '😂', code: 'laugh', title: '웃겨요' },
        { label: '😊', code: 'smile', title: '미소' },
        { label: '😉', code: 'wink', title: '윙크' },
        { label: '😮', code: 'wow', title: '놀라워요' },
        { label: '😢', code: 'sad', title: '슬퍼요' },
        { label: '😭', code: 'cry', title: '눈물' },
        { label: '😡', code: 'angry', title: '화나요' },
        { label: '😈', code: 'devil', title: '장난' },
        { label: '🥳', code: 'party', title: '축하해요' },
        { label: '👏', code: 'clap', title: '박수' },
        { label: '🔥', code: 'fire', title: '최고예요' },
        { label: '🚀', code: 'rocket', title: '가보자' },
        { label: '👀', code: 'eyes', title: '보고 있어요' },
        { label: '🤔', code: 'thinking', title: '생각 중' },
        { label: '👌', code: 'ok', title: '좋아요' },
        { label: '🙏', code: 'pray', title: '부탁해요' },
        { label: '💪', code: 'muscle', title: '힘내요' },
        { label: '🎉', code: 'celebrate', title: '축하' },
        { label: '🤗', code: 'hug', title: '응원해요' },
        { label: '😘', code: 'kiss', title: '애정' },
        { label: '😎', code: 'cool', title: '멋져요' },
        { label: '😴', code: 'sleep', title: '졸려요' },
        { label: '😵‍💫', code: 'confused', title: '혼란' },
        { label: '🤯', code: 'shock', title: '충격' },
        { label: '💩', code: 'poop', title: '별로예요' },
        { label: '😿', code: 'cat_sad', title: '고양이 눈물' },
        { label: '⭐', code: 'star', title: '별' },
        { label: '😍', code: 'heart_eyes', title: '반했어요' },
        { label: '🥰', code: 'lovely', title: '사랑스러워요' },
        { label: '😇', code: 'angel', title: '착해요' },
        { label: '🙃', code: 'upside_down', title: '머쓱해요' },
        { label: '😏', code: 'smirk', title: '흐뭇해요' },
        { label: '😒', code: 'unamused', title: '별로예요' },
        { label: '😤', code: 'huff', title: '흥' },
        { label: '🤩', code: 'star_struck', title: '대단해요' },
        { label: '🥺', code: 'pleading', title: '부탁해요' },
        { label: '🤭', code: 'giggle', title: '킥킥' },
        { label: '🫡', code: 'salute', title: '알겠습니다' },
        { label: '🫠', code: 'melting', title: '녹아요' },
        { label: '🤝', code: 'handshake', title: '약속' },
        { label: '🙌', code: 'hooray', title: '만세' },
        { label: '💯', code: 'hundred', title: '완벽해요' },
        { label: '✅', code: 'check', title: '확인' },
        { label: '🎁', code: 'gift', title: '선물' },
        { label: '🍻', code: 'cheers', title: '건배' },
        { label: '☕', code: 'coffee', title: '커피' },
        { label: '🌙', code: 'moon', title: '잘 자요' }
    ];

    const chatListRef = useRef(null);
    const chatMessageRef = useRef(chatMessage);
    const isLoadingPrevRef = useRef(false);
    const hasMorePrevRef = useRef(true);
    const oldestMessageIdRef = useRef(null);
    const isPrependingPrevRef = useRef(false);
    const shouldStickToBottomRef = useRef(true);
    const isInitialRoomScrollPendingRef = useRef(true);
    const messageHighlightTimerRef = useRef(null);
    const roomSettingsToastTimerRef = useRef(null);
    const toneRefineResultTimerRef = useRef(null);

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
        chatMessageRef.current = chatMessage;
    }, [chatMessage]);

    useEffect(() => {
        setLocalRoomName(roomName ?? '');
        setSettingRoomName(roomName ?? '');
    }, [roomName]);

    useEffect(() => {
        setRoomThumbnailUrl(roomThumbnail ?? '');
        setSettingRoomThumbnailUrl(roomThumbnail ?? '');
        setSettingRoomThumbnailFileName('');
    }, [roomThumbnail]);

    useEffect(() => {
        setRoomBackgroundUrl(customRoomBackground ?? '');
        setSettingRoomBackgroundUrl(customRoomBackground ?? '');
        setSettingRoomBackgroundFileName('');
    }, [customRoomBackground]);

    useEffect(() => {
        setIsMessageNotificationEnabled(messageNotificationEnabled ?? true);
    }, [messageNotificationEnabled]);

    useEffect(() => {
        setCurrentRoomNotice(roomNotice ?? null);
        setIsRoomNoticeVisible(true);
    }, [roomNotice]);

    useEffect(() => {
        if (isChatSearchOpen) {
            chatSearchInputRef.current?.focus();
        }
    }, [isChatSearchOpen]);

    useEffect(() => {
        setChatSearchResultIndex(-1);
    }, [chatSearchWord]);

    function mergeMyReactionState(messages) {
        setMyReactionMap(prev => {
            const next = { ...prev };

            (messages ?? []).forEach(message => {
                (message.reactions ?? []).forEach(reaction => {
                    if (reaction.reactedByMe) {
                        next[`${message.messageId}:${reaction.reactionCode}`] = true;
                    }
                });
            });

            return next;
        });
    }

    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };

    const formatNoticeDateTime = (isoString) => {
        const date = new Date(isoString);

        if (Number.isNaN(date.getTime())) return '';

        const pad = (value) => String(value).padStart(2, '0');

        return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
        if (isDraftRoom) {
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
            const fileUrl = await uploadImageApi(file, target === 'THUMBNAIL' ? 'ROOM_THUMBNAIL' : 'ROOM_BACKGROUND');

            if (!fileUrl) {
                alert('파일 URL을 받을 수 없습니다.');
                return;
            }

            if (target === 'THUMBNAIL') {
                setSettingRoomThumbnailUrl(fileUrl);
                setSettingRoomThumbnailFileName(file.name);
            }

            if (target === 'BACKGROUND') {
                setSettingRoomBackgroundUrl(fileUrl);
                setSettingRoomBackgroundFileName(file.name);
            }
        } catch (err) {
            console.error('방 설정 이미지 업로드 실패', err);
            alert(err.response?.data ?? '이미지 업로드 실패');
        }
    }

    async function saveRoomSettings(nextNotificationEnabled = isMessageNotificationEnabled, useDraftSettings = true) {
        const nextRoomName = (useDraftSettings ? settingRoomName : localRoomName).trim();
        const nextRoomThumbnailUrl = useDraftSettings ? settingRoomThumbnailUrl : roomThumbnailUrl;
        const nextRoomBackgroundUrl = useDraftSettings ? settingRoomBackgroundUrl : roomBackgroundUrl;

        if (!nextRoomName) {
            alert('방 이름을 입력해주세요.');
            return;
        }

        try {
            setIsSavingRoomSettings(true);

            await updateMyRoomSettingsApi({
                roomId,
                customRoomName: nextRoomName,
                customRoomThumbnail: nextRoomThumbnailUrl,
                customRoomBackground: nextRoomBackgroundUrl,
                messageNotificationEnabled: nextNotificationEnabled
            });

            if (useDraftSettings) {
                setLocalRoomName(nextRoomName);
                setRoomThumbnailUrl(nextRoomThumbnailUrl);
                setRoomBackgroundUrl(nextRoomBackgroundUrl);
                setIsRoomNameEditing(false);
            }

            if (roomSettingsToastTimerRef.current) {
                clearTimeout(roomSettingsToastTimerRef.current);
            }

            setRoomSettingsToast('적용되었습니다.');
            roomSettingsToastTimerRef.current = setTimeout(() => {
                setRoomSettingsToast('');
                roomSettingsToastTimerRef.current = null;
            }, 1800);

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
        await saveRoomSettings(nextEnabled, false);
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

    const latestMessageId = prevChattings[prevChattings.length - 1]?.messageId ?? null;

    useEffect(() => {
        if (isPrependingPrevRef.current) return;
        if (latestMessageId == null) return;
        if (!isInitialRoomScrollPendingRef.current && !shouldStickToBottomRef.current) return;

        requestAnimationFrame(() => {
            const chatListEl = chatListRef.current;

            if (!chatListEl) return;

            chatListEl.scrollTop = chatListEl.scrollHeight;
            shouldStickToBottomRef.current = true;
            isInitialRoomScrollPendingRef.current = false;
        });
    }, [roomId, latestMessageId, prevChattings.length]);

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
            const olderMessages = await loadMessagesInRoomApi(roomId, MESSAGE_PAGE_SIZE, beforeMessageId);

            mergeMyReactionState(olderMessages);

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
                if (!currentEl) {
                    isPrependingPrevRef.current = false;
                    return;
                }

                const newScrollHeight = currentEl.scrollHeight;
                currentEl.scrollTop = newScrollHeight - prevScrollHeight;
                isPrependingPrevRef.current = false;
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

        const distanceToBottom = chatListEl.scrollHeight - chatListEl.scrollTop - chatListEl.clientHeight;
        shouldStickToBottomRef.current = distanceToBottom <= 80;
        setIsScrolledAwayFromBottom(!shouldStickToBottomRef.current);

        if (shouldStickToBottomRef.current) {
            setNewMessageWhileScrolled(null);
        }

        if (chatListEl.scrollTop <= LOAD_PREV_THRESHOLD_PX) {
            loadOlderMessages();
        }
    }

    function scrollChatToBottom() {
        const chatListEl = chatListRef.current;
        if (!chatListEl) return;

        shouldStickToBottomRef.current = true;
        setIsScrolledAwayFromBottom(false);
        setNewMessageWhileScrolled(null);
        chatListEl.scrollTo({ top: chatListEl.scrollHeight, behavior: 'smooth' });
    }

    function keepLatestMessageVisibleAfterMediaLoad() {
        if (!shouldStickToBottomRef.current && !isInitialRoomScrollPendingRef.current) return;

        requestAnimationFrame(() => {
            const chatListEl = chatListRef.current;

            if (!chatListEl) return;

            chatListEl.scrollTop = chatListEl.scrollHeight;
            shouldStickToBottomRef.current = true;
            isInitialRoomScrollPendingRef.current = false;
        });
    }

    function scrollToMessage(messageId) {
        const chatListEl = chatListRef.current;

        if (!chatListEl || messageId == null) return;

        const targetMessageEl = chatListEl.querySelector(`[data-message-id="${String(messageId)}"]`);

        if (!targetMessageEl) return;

        const chatListRect = chatListEl.getBoundingClientRect();
        const targetMessageRect = targetMessageEl.getBoundingClientRect();
        const nextScrollTop = chatListEl.scrollTop
            + targetMessageRect.top
            - chatListRect.top
            - ((chatListEl.clientHeight - targetMessageRect.height) / 2);

        shouldStickToBottomRef.current = false;
        chatListEl.scrollTo({
            top: Math.max(nextScrollTop, 0),
            behavior: 'smooth'
        });

        setHighlightedMessageId(String(messageId));

        if (messageHighlightTimerRef.current) {
            clearTimeout(messageHighlightTimerRef.current);
        }

        messageHighlightTimerRef.current = setTimeout(() => {
            setHighlightedMessageId(null);
            messageHighlightTimerRef.current = null;
        }, 1400);
    }

    function moveChatSearchResult(direction = 1) {
        if (chatSearchResults.length === 0) return;

        const nextIndex = direction > 0
            ? (chatSearchResultIndex + 1) % chatSearchResults.length
            : (chatSearchResultIndex - 1 + chatSearchResults.length) % chatSearchResults.length;

        setChatSearchResultIndex(nextIndex);
        scrollToMessage(chatSearchResults[nextIndex].messageId);
    }

    function closeChatSearch() {
        setIsChatSearchOpen(false);
        setChatSearchWord('');
        setChatSearchResultIndex(-1);
    }

    function renderSearchHighlightedText(text) {
        const searchWord = chatSearchWord.trim();

        if (!searchWord || !text) return text;

        const escapedWord = searchWord.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return text.split(new RegExp(`(${escapedWord})`, 'gi')).map((part, index) =>
            part.toLocaleLowerCase() === searchWord.toLocaleLowerCase()
                ? <mark className="chatSearchMatch" key={`${part}-${index}`}>{part}</mark>
                : part
        );
    }

    function openConfirmModal(title, message, onConfirm, danger = false, confirmLabel = '확인') {
        setConfirmModal({ title, message, onConfirm, danger, confirmLabel });
    }

    async function confirmCurrentAction() {
        const action = confirmModal?.onConfirm;
        setConfirmModal(null);

        if (action) {
            await action();
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
        shouldStickToBottomRef.current = true;
        isInitialRoomScrollPendingRef.current = true;

        setPrevChattings(Array.isArray(initialMessages) ? initialMessages : []);
        setTypingUsers([]);
        setIsRoomMenuOpen(false);
        setIsChatSearchOpen(false);
        setChatSearchWord('');
        setChatSearchResultIndex(-1);
        setIsInviteFriendPanelOpen(false);
        setSelectedInviteFriends([]);
        setProfileTargetMember(null);
        setRoleChangeResultModal(null);
        setConfirmModal(null);
        setImageViewerAttachment(null);
        setMessageContextMenu(null);
        setReplyTargetMessage(null);
        setReactionTargetMessage(null);
        setReactionPickerPosition(null);
        setReactionViewer(null);
        setMessageReadersViewer(null);
        setHighlightedMessageId(null);
        setIsRoomNoticePanelOpen(false);
        setRoomNoticeList([]);
        setIsLoadingRoomNotices(false);
        setHasMoreRoomNotices(true);
        setEditingRoomNoticeId(null);
        setEditingRoomNoticeContents('');
        setIsWritingRoomNotice(false);
        setNewRoomNoticeContents('');
        setIsScrolledAwayFromBottom(false);
        setNewMessageWhileScrolled(null);
        setSelectedFiles([]);
        setUploadProgress(null);
        setIsUploadingFiles(false);
        setIsAiRecommendLoading(false);
        setAiRecommendedMessages([]);
        setIsToneRefinePanelOpen(false);
        setRefiningTone(null);
        setToneRefineResultModal(null);
        setIsPersonalizedRecommendOpen(false);
        setPersonalizedTargetPublicId('');
        setIsPersonalizedRecommendLoading(false);
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

                setPrevChattings(currentMessages => {
                    const mergedMessageMap = new Map();

                    (messages ?? []).forEach(message => {
                        mergedMessageMap.set(String(message.messageId), message);
                    });

                    currentMessages.forEach(message => {
                        mergedMessageMap.set(String(message.messageId), message);
                    });

                    const mergedMessages = Array.from(mergedMessageMap.values());
                    oldestMessageIdRef.current = mergedMessages[0]?.messageId ?? null;

                    return mergedMessages;
                });
                mergeMyReactionState(messages);

                hasMorePrevRef.current = messages.length === MESSAGE_PAGE_SIZE;

                if (messages.length) {
                    const lastOtherMsgInRoom = [...messages]
                        .reverse()
                        .find(msg => msg.messageType !== 'SYSTEM' && msg.senderPublicId !== myPublicId);

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

                if (!shouldStickToBottomRef.current && newMsg.senderPublicId !== myPublicId) {
                    setNewMessageWhileScrolled(newMsg);
                }

                setPrevChattings(prev => {
                    const alreadyRendered = prev.some(message => String(message.messageId) === String(newMsg.messageId));

                    return alreadyRendered ? prev : [...prev, newMsg];
                });

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

                if (wsResponse.wsType === "ROOM_MEMBER_INVITED" && Array.isArray(feed?.targetMembers)) {
                    setLocallyAddedRoomMembers(prev => {
                        const map = new Map(prev.map(member => [member.publicId, member]));
                        feed.targetMembers.forEach(member => map.set(member.publicId, member));
                        return Array.from(map.values());
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

                    // 내가 요청한 권한 변경이 성공했을 때만 결과 모달을 띄운다.
                    if (feed?.requesterPublicId === myPublicId && targetRole) {
                        setRoleChangeResultModal({
                            nickname: feed?.targetNicknames?.[0] ?? '멤버',
                            role: targetRole
                        });
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
                const noticeView = notice.roomNoticeView ?? null;
                const feed = notice.roomFeedResponse ?? notice.roomFeed ?? notice;

                if (noticeView) {
                    if (noticeView.roomNoticeStatus === 'ACTIVE') {
                        setIsRoomNoticeVisible(true);
                    }

                    setCurrentRoomNotice(prev => {
                        if (noticeView.roomNoticeStatus === 'ACTIVE') {
                            return noticeView;
                        }

                        if (Number(prev?.roomNoticeId) === Number(noticeView.roomNoticeId)) {
                            return null;
                        }

                        return prev;
                    });
                    setRoomNoticeList(prev => {
                        const exists = prev.some(item => Number(item.roomNoticeId) === Number(noticeView.roomNoticeId));

                        if (!exists) {
                            return [noticeView, ...prev];
                        }

                        return prev.map(item => Number(item.roomNoticeId) === Number(noticeView.roomNoticeId) ? noticeView : item);
                    });
                }

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
    }, [roomId, myPublicId, initialMessages]);

    useEffect(() => {
        return () => {
            if (messageHighlightTimerRef.current) {
                clearTimeout(messageHighlightTimerRef.current);
            }

            if (roomSettingsToastTimerRef.current) {
                clearTimeout(roomSettingsToastTimerRef.current);
            }

            if (toneRefineResultTimerRef.current) {
                clearTimeout(toneRefineResultTimerRef.current);
            }
        };
    }, []);

    async function sendChatMessage() {
        if (isUploadingFiles || isAiProcessing) return;

        if (isDraftRoom && startChatRequestLockRef.current) return;

        if (isEmptyMessage(chatMessage)) {
            return;
        }

        if (isDraftRoom) {
            startChatRequestLockRef.current = true;
            setIsStartingChat(true);
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
            if (isDraftRoom) {
                if (selectedFiles.length > 0) {
                    alert('첫 메시지 파일 첨부는 방 생성 후 전송해주세요.');
                    return;
                }

                let startResponse;

                if (isDraftDirectRoom) {
                    if (!targetPublicId) {
                        alert('1:1 채팅 대상 정보가 없습니다.');
                        return;
                    }

                    startResponse = await emitWsStartDirectChat(targetPublicId, chatMessage, {
                        messageType: 'TEXT',
                        replyToMessageId: null,
                        attachmentIds: []
                    });
                } else if (isDraftGroupRoom) {
                    if (!Array.isArray(inviteMemberPublicIds) || inviteMemberPublicIds.length === 0) {
                        alert('단톡방 초대 대상 정보가 없습니다.');
                        return;
                    }

                    startResponse = await emitWsStartGroupChat(localRoomName, roomThumbnailUrl, inviteMemberPublicIds, chatMessage);
                } else {
                    alert('draft 채팅방 정보가 올바르지 않습니다.');
                    return;
                }

                const startChat = startResponse.payload;

                setChatMessage('');
                setSelectedFiles([]);
                setReplyTargetMessage(null);
                setUploadProgress(null);

                exitChatRoom();
                openRoom(startChat.enterRoomInfo, [startChat.firstChatMessage]);
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

            if (isDraftRoom) {
                startChatRequestLockRef.current = false;
                setIsStartingChat(false);
            }
        }
    }

    async function requestAiRecommendedMessages() {
        if (isAiProcessing) return;

        if (!roomId) {
            alert('AI 추천은 실제 채팅방이 생성된 뒤 사용할 수 있습니다.');
            return;
        }

        try {
            setIsToneRefinePanelOpen(false);
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

    async function refineCurrentMessageTone(tone) {
        if (isAiProcessing) return;

        const sourceMessage = chatMessage;

        if (!sourceMessage.trim()) {
            alert('다듬을 메시지를 먼저 입력해 주세요.');
            return;
        }

        try {
            setRefiningTone(tone);
            const refinedMessage = await refineMessageToneApi(sourceMessage, tone);

            if (chatMessageRef.current !== sourceMessage) {
                alert('AI 처리 중 입력문이 변경되어 결과를 적용하지 않았습니다.');
                return;
            }

            setChatMessage(refinedMessage);
            setIsToneRefinePanelOpen(false);
            setToneRefineResultModal({
                toneLabel: getToneLabel(tone),
                before: sourceMessage,
                after: refinedMessage
            });

            if (toneRefineResultTimerRef.current) {
                clearTimeout(toneRefineResultTimerRef.current);
            }

            toneRefineResultTimerRef.current = setTimeout(closeToneRefineResultModal, 4000);
        } catch (e) {
            console.error('AI 메시지 말투 다듬기 실패', e);
            alert(e.response?.data?.message ?? e.response?.data ?? 'AI 메시지 말투 다듬기 실패');
        } finally {
            setRefiningTone(null);
        }
    }

    function getToneLabel(tone) {
        return { SOFT: '부드럽게', CONCISE: '간결하게', POLITE: '정중하게' }[tone] ?? tone;
    }

    function closeToneRefineResultModal() {
        if (toneRefineResultTimerRef.current) {
            clearTimeout(toneRefineResultTimerRef.current);
            toneRefineResultTimerRef.current = null;
        }

        setToneRefineResultModal(null);
    }

    async function requestPersonalizedMessages(relationshipType) {
        if (isAiProcessing || !personalizedTargetPublicId) return;

        try {
            setIsPersonalizedRecommendLoading(true);
            const recommendations = await recommendPersonalizedMessagesApi(roomId, personalizedTargetPublicId, relationshipType);
            setAiRecommendedMessages(Array.isArray(recommendations) ? recommendations : []);
            setIsPersonalizedRecommendOpen(false);
            setPersonalizedTargetPublicId('');
        } catch (e) {
            console.error('AI 섬세한 맞춤 메시지 추천 실패', e);
            alert(e.response?.data?.message ?? e.response?.data ?? 'AI 섬세한 맞춤 메시지 추천 실패');
        } finally {
            setIsPersonalizedRecommendLoading(false);
        }
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

            if (toneRefineResultModal) {
                closeToneRefineResultModal();
                return;
            }

            if (isPersonalizedRecommendOpen) {
                setIsPersonalizedRecommendOpen(false);
                setPersonalizedTargetPublicId('');
                return;
            }

            if (confirmModal) {
                setConfirmModal(null);
                return;
            }

            if (roleChangeResultModal) {
                setRoleChangeResultModal(null);
                return;
            }

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

            if (isRoomNoticePanelOpen) {
                closeRoomNoticePanel();
                return;
            }

            if (isRoomMenuOpen) {
                setIsRoomMenuOpen(false);
                return;
            }

            if (isToneRefinePanelOpen) {
                setIsToneRefinePanelOpen(false);
                return;
            }

            if (isChatSearchOpen) {
                closeChatSearch();
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
    }, [toneRefineResultModal, isPersonalizedRecommendOpen, confirmModal, roleChangeResultModal, messageContextMenu, reactionTargetMessage, reactionViewer, messageReadersViewer, imageViewerAttachment, profileTargetMember, isInviteFriendPanelOpen, isRoomNoticePanelOpen, isRoomMenuOpen, isToneRefinePanelOpen, isChatSearchOpen]);

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

    function kickMember(member) {
        if (!member) return;

        openConfirmModal(
            '멤버 추방',
            `${member.nickname}님을 채팅방에서 추방하시겠습니까?`,
            () => executeKickMember(member),
            true,
            '추방'
        );
    }

    async function executeKickMember(member) {
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

    function changeMemberRole(e, member, targetRole) {
        if (!member) return;
        if (!canChangeMemberRole(member)) return;
        if (member.role === targetRole) return;

        const roleChangeDetails = e.currentTarget.closest('details');

        if (roleChangeDetails) {
            roleChangeDetails.open = false;
        }

        openConfirmModal(
            '멤버 권한 변경',
            `${member.nickname}님의 권한을 ${targetRole}로 변경하시겠습니까?`,
            () => executeChangeMemberRole(member, targetRole)
        );
    }

    async function executeChangeMemberRole(member, targetRole) {
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

            await emitWsInviteMember(roomId, inviteTargetMemberPublicIds);

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

    function downloadImageFromViewer() {
        if (!imageViewerAttachment?.fileUrl) return;

        const fileName = imageViewerAttachment.originalFileName || `image-${imageViewerAttachment.attachmentId ?? Date.now()}`;

        openConfirmModal('파일 다운로드', `${fileName} 파일을 다운로드하시겠습니까?`, executeImageDownload);
    }

    async function executeImageDownload() {
        const fileName = imageViewerAttachment.originalFileName || `image-${imageViewerAttachment.attachmentId ?? Date.now()}`;

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
                left: Math.min(Math.max(clickX - boxRect.left, 8), Math.max(boxRect.width - 280, 8)),
                top: Math.min(Math.max(clickY - boxRect.top, 52), Math.max(boxRect.height - 270, 52))
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
            openConfirmModal(
                '메시지 삭제',
                '이 메시지를 삭제하시겠습니까?',
                () => emitWsDeleteMessage(roomId, targetMessage.messageId),
                true
            );
            return;
        }

        if (action === 'READERS') {
            openMessageReadersViewer(targetMessage);
            return;
        }

        if (action === 'SHARE') {
            const targetRoomId = window.prompt('공유할 채팅방 roomId를 입력해주세요.');

            if (!targetRoomId) {
                return;
            }

            const sharedText = targetMessage.messageText
                ? `[공유] ${targetMessage.messageText}`
                : '[공유] 첨부 메시지';

            const emitted = emitWsSendMessage(Number(targetRoomId), sharedText, {
                messageType: 'TEXT',
                replyToMessageId: null,
                attachmentIds: []
            });

            if (!emitted) {
                alert('WebSocket 연결 안 됨');
            }

            return;
        }

        if (action === 'NOTICE') {
            applyMessageAsRoomNotice(targetMessage);
        }
    }

    function applyMessageAsRoomNotice(targetMessage) {
        if (!roomId || !targetMessage?.messageId) return;

        openConfirmModal(
            '공지 등록',
            '이 메시지를 공지로 등록하시겠습니까?',
            () => executeApplyMessageAsRoomNotice(targetMessage)
        );
    }

    async function executeApplyMessageAsRoomNotice(targetMessage) {
        const contents = targetMessage.messageText || '첨부 메시지';

        try {
            await emitWsApplyRoomNotice({
                roomId,
                roomNoticeAction: 'CREATE',
                roomNoticeType: 'MESSAGE',
                sourceMessageId: targetMessage.messageId,
                roomNoticeContents: contents
            });
        } catch (e) {
            console.error('공지 등록 실패', e);
            alert(e.message || '공지 등록 실패');
        }
    }

    async function loadRoomNotices(reset = false) {
        if (!roomId || isLoadingRoomNotices) return;
        if (!reset && !hasMoreRoomNotices) return;

        const beforeRoomNoticeId = reset
            ? null
            : roomNoticeList[roomNoticeList.length - 1]?.roomNoticeId ?? null;

        try {
            setIsLoadingRoomNotices(true);
            const notices = await loadRoomNoticesApi(roomId, beforeRoomNoticeId, ROOM_NOTICE_PAGE_SIZE);
            const nextNotices = Array.isArray(notices) ? notices : [];

            setRoomNoticeList(prev => {
                if (reset) return nextNotices;

                const map = new Map(prev.map(notice => [Number(notice.roomNoticeId), notice]));
                nextNotices.forEach(notice => map.set(Number(notice.roomNoticeId), notice));
                return Array.from(map.values());
            });
            setHasMoreRoomNotices(nextNotices.length === ROOM_NOTICE_PAGE_SIZE);
        } catch (e) {
            console.error('공지사항 목록 조회 실패', e);
            alert(e.response?.data ?? '공지사항 목록 조회 실패');
        } finally {
            setIsLoadingRoomNotices(false);
        }
    }

    function openRoomNoticePanel() {
        if (!roomId) return;

        setIsRoomMenuOpen(false);
        setIsRoomNoticePanelOpen(true);
        setEditingRoomNoticeId(null);
        setEditingRoomNoticeContents('');
        loadRoomNotices(true);
    }

    function closeRoomNoticePanel() {
        setIsRoomNoticePanelOpen(false);
        setEditingRoomNoticeId(null);
        setEditingRoomNoticeContents('');
        setIsWritingRoomNotice(false);
        setNewRoomNoticeContents('');
    }

    async function createCustomRoomNotice() {
        const contents = newRoomNoticeContents.trim();

        if (!contents) {
            alert('공지 내용을 입력해주세요.');
            return;
        }

        try {
            await emitWsApplyRoomNotice({
                roomId,
                roomNoticeAction: 'CREATE',
                roomNoticeType: 'CUSTOM',
                roomNoticeContents: contents
            });
            setIsWritingRoomNotice(false);
            setNewRoomNoticeContents('');
        } catch (e) {
            console.error('새 공지 등록 실패', e);
            alert(e.message || '새 공지 등록 실패');
        }
    }

    function handleRoomNoticeScroll(e) {
        const target = e.currentTarget;

        if (target.scrollHeight - target.scrollTop - target.clientHeight <= 60) {
            loadRoomNotices(false);
        }
    }

    function getRoomNoticeRequesterNickname(notice) {
        return notice?.requesterNickname
            || memberMap[notice?.requesterPublicId]?.nickname
            || '알 수 없음';
    }

    function getRoomNoticeDisplayContents(notice) {
        return notice?.roomNoticeStatus === 'DELETED'
            ? '삭제된 공지사항입니다.'
            : notice?.roomNoticeContents ?? '';
    }

    function startEditingRoomNotice(notice) {
        setEditingRoomNoticeId(notice.roomNoticeId);
        setEditingRoomNoticeContents(notice.roomNoticeContents ?? '');
    }

    async function applyRoomNoticeAction(roomNoticeAction, targetNotice = currentRoomNotice, nextContents = null, confirmed = false) {
        if (!targetNotice?.roomNoticeId) {
            alert('처리할 공지가 없습니다.');
            return;
        }

        if (!confirmed && (roomNoticeAction === 'DELETE' || roomNoticeAction === 'INACTIVATE')) {
            const isDelete = roomNoticeAction === 'DELETE';
            openConfirmModal(
                isDelete ? '공지 삭제' : '공지 내리기',
                isDelete ? '이 공지를 삭제하시겠습니까?' : '이 공지를 내리시겠습니까?',
                () => applyRoomNoticeAction(roomNoticeAction, targetNotice, nextContents, true),
                isDelete
            );
            return;
        }

        const roomNoticeContents = nextContents ?? targetNotice.roomNoticeContents;

        try {
            await emitWsApplyRoomNotice({
                roomId,
                roomNoticeAction,
                targetRoomNoticeId: targetNotice.roomNoticeId,
                roomNoticeType: targetNotice.roomNoticeType || 'CUSTOM',
                roomNoticeContents
            });

            setEditingRoomNoticeId(null);
            setEditingRoomNoticeContents('');
        } catch (e) {
            console.error('공지 처리 실패', e);
            alert(e.message || '공지 처리 실패');
        }
    }

    async function saveEditedRoomNotice(notice) {
        const nextContents = editingRoomNoticeContents.trim();

        if (!nextContents) {
            alert('공지 내용을 입력해주세요.');
            return;
        }

        await applyRoomNoticeAction('UPDATE', notice, nextContents);
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
                className={`${isDocked ? 'chattingRoomSection docked' : 'chattingRoomSection'} ${isChatSearchOpen ? 'searchOpen' : ''}`}
                style={isDocked ? undefined : {
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
                <div className='chatListTitle' onMouseDown={isDocked ? undefined : startDrag}>
                    <div className="chatTitleLeftControls">
                        <button
                            className={`roomMenuButton ${isRoomMenuOpen ? 'active' : ''}`}
                            title={isDraftRoom ? '첫 메시지 전송 후 메뉴를 사용할 수 있습니다.' : '채팅방 메뉴'}
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={() => {
                                if (isDraftRoom) return;
                                setIsRoomMenuOpen(prev => !prev);
                            }}
                            disabled={isDraftRoom}
                        >
                            <span className="roomMenuButtonIcon">☰</span>
                        </button>
                    </div>

                    <span className="chatRoomTitleText">{localRoomName}</span>

                    <div className="chatTitleRightControls">
                        <button
                            className={`chatSearchButton ${isChatSearchOpen ? 'active' : ''}`}
                            title="채팅방 메시지 검색"
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={() => setIsChatSearchOpen(prev => !prev)}
                        >
                            <span aria-hidden="true">🔍</span>
                        </button>

                        <button
                            className={`notificationBellButton ${isMessageNotificationEnabled ? 'on' : 'off'}`}
                            title={isDraftRoom ? '첫 메시지 전송 후 알림 설정을 사용할 수 있습니다.' : isMessageNotificationEnabled ? '메시지 알림 켜짐' : '메시지 알림 꺼짐'}
                            onMouseDown={(e) => e.stopPropagation()}
                            onClick={toggleMessageNotification}
                            disabled={isDraftRoom}
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

                {isChatSearchOpen && (
                    <div className="chatSearchBar">
                        <span aria-hidden="true">🔍</span>
                        <input
                            ref={chatSearchInputRef}
                            value={chatSearchWord}
                            onChange={(e) => setChatSearchWord(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    e.preventDefault();
                                    moveChatSearchResult(e.shiftKey ? -1 : 1);
                                }
                            }}
                            placeholder="메시지 검색"
                        />
                        <span className="chatSearchCount">
                            {chatSearchResults.length > 0 ? `${chatSearchResultIndex + 1}/${chatSearchResults.length}` : '0/0'}
                        </span>
                        <button onClick={() => moveChatSearchResult(-1)} disabled={chatSearchResults.length === 0}>↑</button>
                        <button onClick={() => moveChatSearchResult(1)} disabled={chatSearchResults.length === 0}>↓</button>
                        <button aria-label="검색 닫기" onClick={closeChatSearch}>×</button>
                    </div>
                )}

                {currentRoomNotice && (
                    <div className={`activeRoomNoticeBar ${isRoomNoticeVisible ? '' : 'collapsed'}`}>
                        {isRoomNoticeVisible && (
                            <button className="activeRoomNoticeContents" onClick={openRoomNoticePanel}>
                                <span className="activeRoomNoticeNickname">
                                    {getRoomNoticeRequesterNickname(currentRoomNotice)}
                                </span>
                                <span className="activeRoomNoticeDivider">:</span>
                                <span className="activeRoomNoticeText">
                                    {currentRoomNotice.roomNoticeContents}
                                </span>
                            </button>
                        )}

                        <button
                            className="activeRoomNoticeToggleButton"
                            onClick={() => setIsRoomNoticeVisible(prev => !prev)}
                        >
                            {isRoomNoticeVisible ? '숨기기' : '공지표시'}
                        </button>
                    </div>
                )}

                <div
                    className={`roomSidePanel ${isRoomMenuOpen ? 'open' : ''}`}
                    onMouseDown={(e) => e.stopPropagation()}
                >
                    <div className="roomSidePanelHeader">
                        <span>채팅방 메뉴</span>
                        <button onClick={() => setIsRoomMenuOpen(false)}>닫기</button>
                    </div>

                    {roomSettingsToast && (
                        <div className="roomSettingsToast">
                            {roomSettingsToast}
                        </div>
                    )}

                    <div className="roomSidePanelBody">
                        <div className="roomSidePanelRoomColumn">
                            <div className="roomSidePanelRoomScroll">
                                <div className="roomSettingsBox">
                                    <div className="roomNameSettingHeader">
                                        <span>방 이름</span>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                if (isRoomNameEditing) {
                                                    setSettingRoomName(localRoomName);
                                                    setIsRoomNameEditing(false);
                                                    return;
                                                }

                                                setIsRoomNameEditing(true);
                                            }}
                                        >
                                            {isRoomNameEditing ? '취소' : '변경'}
                                        </button>
                                    </div>

                                    <label className="roomSettingLabel">
                                        <input
                                            value={settingRoomName}
                                            onChange={(e) => setSettingRoomName(e.target.value)}
                                            placeholder="방 이름"
                                            disabled={!isRoomNameEditing}
                                        />
                                    </label>

                                    <div className="roomImageSettingRow">
                                        <button onClick={() => roomThumbnailInputRef.current?.click()}>썸네일 사진</button>
                                        <span>{settingRoomThumbnailFileName || (settingRoomThumbnailUrl ? '기존 썸네일 사용 중' : '기본 이미지')}</span>
                                        <input ref={roomThumbnailInputRef} type="file" accept="image/*" hidden onChange={(e) => uploadRoomSettingImage(e, 'THUMBNAIL')} />
                                    </div>

                                    <div className="roomImageSettingRow">
                                        <button onClick={() => roomBackgroundInputRef.current?.click()}>배경 사진</button>
                                        <span>{settingRoomBackgroundFileName || (settingRoomBackgroundUrl ? '기존 배경 사용 중' : '기본 배경')}</span>
                                        <input ref={roomBackgroundInputRef} type="file" accept="image/*" hidden onChange={(e) => uploadRoomSettingImage(e, 'BACKGROUND')} />
                                    </div>

                                    <button className="saveRoomSettingsButton" onClick={() => saveRoomSettings()} disabled={isSavingRoomSettings}>
                                        방 설정 저장
                                    </button>
                                </div>

                                <div className="roomNoticeSettingBox">
                                    <div className="roomNoticeSettingHeader">공지사항</div>
                                    {currentRoomNotice ? (
                                        <button className="roomNoticeContents" onClick={openRoomNoticePanel}>{currentRoomNotice.roomNoticeContents}</button>
                                    ) : (
                                        <button className="roomNoticeEmpty" onClick={openRoomNoticePanel}>등록된 공지 없음</button>
                                    )}
                                </div>
                            </div>

                            <div className="roomSidePanelDangerZone">
                                <button className="leaveRoomInMenuButton" onClick={leftRoom}>방 나가기</button>
                            </div>
                        </div>

                        <div className="roomSidePanelMemberColumn">
                            <div className="roomSidePanelSubTitle">채팅방 멤버</div>
                            <div className="roomMemberList">
                                {visibleRoomMembers.map(member => {
                                    const isMe = member.publicId === myPublicId;

                                    return (
                                        <div className={`roomMemberItem ${isMe ? 'me' : ''}`} key={member.publicId}>
                                            <img className="roomMemberProfileImg" src={member.profileImg || '/images/mococo_question.png'} alt={member.nickname} />
                                            <div className="roomMemberInfo">
                                                <div className="roomMemberNicknameLine">
                                                    <span className="roomMemberNickname">{member.nickname}</span>
                                                    {isMe && <span className="roomMemberMeBadge">나</span>}
                                                </div>
                                            </div>
                                            <div className={`roomMemberRole role-${member.role}`}>{member.role}</div>

                                            {canChangeMemberRole(member) ? (
                                                <details className="memberRoleChangeDetails">
                                                    <summary>권한</summary>
                                                    <div className="memberRoleChangeMenu">
                                                        <button className={member.role === 'MEMBER' ? 'active' : ''} onClick={(e) => changeMemberRole(e, member, 'MEMBER')}>MEMBER</button>
                                                        <button className={member.role === 'MANAGER' ? 'active' : ''} onClick={(e) => changeMemberRole(e, member, 'MANAGER')}>MANAGER</button>
                                                    </div>
                                                </details>
                                            ) : (
                                                <div className="memberRoleChangePlaceholder" />
                                            )}

                                            {canKickMember(member) ? (
                                                <div className="memberDangerActions">
                                                    <button className="kickMemberButton" onClick={() => kickMember(member)}>추방</button>
                                                </div>
                                            ) : (
                                                <div className="kickMemberButtonPlaceholder" />
                                            )}
                                        </div>
                                    );
                                })}
                            </div>

                            {roomType === 'GROUP' && (
                                <button className="inviteMemberButton" onClick={openInviteMemberPanel}>초대하기</button>
                            )}
                        </div>
                    </div>
                </div>

                {isRoomNoticePanelOpen && (
                    <div className="roomNoticePanel" onMouseDown={(e) => e.stopPropagation()}>
                        <div className="roomNoticePanelHeader">
                            <span>공지사항</span>
                            <div className="roomNoticePanelHeaderActions">
                                <button className="write" onClick={() => setIsWritingRoomNotice(prev => !prev)}>새 공지</button>
                                <button onClick={closeRoomNoticePanel}>×</button>
                            </div>
                        </div>

                        {isWritingRoomNotice && (
                            <div className="newRoomNoticeEditor">
                                <textarea
                                    value={newRoomNoticeContents}
                                    onChange={(e) => setNewRoomNoticeContents(e.target.value)}
                                    maxLength={1500}
                                    placeholder="새 공지 내용을 입력해주세요."
                                />
                                <div>
                                    <button onClick={createCustomRoomNotice}>등록</button>
                                    <button className="cancel" onClick={() => { setIsWritingRoomNotice(false); setNewRoomNoticeContents(''); }}>취소</button>
                                </div>
                            </div>
                        )}

                        <div className="roomNoticePanelList" onScroll={handleRoomNoticeScroll}>
                            {roomNoticeList.map(notice => {
                                const isOwner = notice.requesterPublicId === myPublicId;
                                const isEditing = Number(editingRoomNoticeId) === Number(notice.roomNoticeId);

                                return (
                                    <div className={`roomNoticeHistoryItem status-${notice.roomNoticeStatus}`} key={notice.roomNoticeId}>
                                        <div className="roomNoticeHistoryMeta">
                                            <span className="roomNoticeHistoryNickname">{getRoomNoticeRequesterNickname(notice)}</span>
                                            <span>{notice.lastAppliedAt ? formatNoticeDateTime(notice.lastAppliedAt) : ''}</span>
                                        </div>

                                        {isEditing ? (
                                            <textarea
                                                className="roomNoticeEditTextarea"
                                                value={editingRoomNoticeContents}
                                                onChange={(e) => setEditingRoomNoticeContents(e.target.value)}
                                                maxLength={1500}
                                            />
                                        ) : (
                                            <div className="roomNoticeHistoryContents">{getRoomNoticeDisplayContents(notice)}</div>
                                        )}

                                        {isOwner && notice.roomNoticeStatus !== 'DELETED' && (
                                            <div className="roomNoticeHistoryActions">
                                                {isEditing ? (
                                                    <>
                                                        <button onClick={() => saveEditedRoomNotice(notice)}>저장</button>
                                                        <button onClick={() => setEditingRoomNoticeId(null)}>취소</button>
                                                    </>
                                                ) : (
                                                    <>
                                                        <button onClick={() => startEditingRoomNotice(notice)}>수정</button>
                                                        {notice.roomNoticeStatus === 'ACTIVE' ? (
                                                            <button onClick={() => applyRoomNoticeAction('INACTIVATE', notice)}>내리기</button>
                                                        ) : (
                                                            <button onClick={() => applyRoomNoticeAction('REACTIVATE', notice)}>재공지</button>
                                                        )}
                                                        <button className="delete" onClick={() => applyRoomNoticeAction('DELETE', notice)}>삭제</button>
                                                    </>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}

                            {isLoadingRoomNotices && <div className="roomNoticePanelState">불러오는 중...</div>}
                            {!isLoadingRoomNotices && roomNoticeList.length === 0 && <div className="roomNoticePanelState">공지사항이 없습니다.</div>}
                            {!isLoadingRoomNotices && roomNoticeList.length > 0 && !hasMoreRoomNotices && <div className="roomNoticePanelState">마지막 공지입니다.</div>}
                        </div>
                    </div>
                )}

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
                            <button className="profilePopupCloseButton" aria-label="닫기" onClick={closeProfilePopup}>
                                ×
                            </button>

                            <div className="profilePopupImageWrap">
                                <img
                                    className="profilePopupImage"
                                    src={profileTargetMember.profileImg || '/images/mococo_question.png'}
                                    alt={profileTargetMember.nickname}
                                />
                            </div>

                            <div className="profilePopupMainInfo">
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
                    </div>
                )}

                {roleChangeResultModal && (
                    <div
                        className="roleChangeResultOverlay"
                        onMouseDown={(e) => {
                            e.stopPropagation();
                            setRoleChangeResultModal(null);
                        }}
                    >
                        <div
                            className="roleChangeResultModal"
                            onMouseDown={(e) => e.stopPropagation()}
                        >
                            <div className="roleChangeResultText">
                                {roleChangeResultModal.nickname}님의 권한을 {roleChangeResultModal.role}로 변경하였습니다.
                            </div>
                            <button onClick={() => setRoleChangeResultModal(null)}>확인</button>
                        </div>
                    </div>
                )}

                {confirmModal && (
                    <div className="chatConfirmOverlay" onMouseDown={() => setConfirmModal(null)}>
                        <div className="chatConfirmModal" onMouseDown={(e) => e.stopPropagation()}>
                            <div className="chatConfirmTitle">{confirmModal.title}</div>
                            <div className="chatConfirmMessage">{confirmModal.message}</div>
                            <div className="chatConfirmActions">
                                <button className={confirmModal.danger ? 'danger' : ''} onClick={confirmCurrentAction}>{confirmModal.confirmLabel}</button>
                                <button className="cancel" onClick={() => setConfirmModal(null)}>취소</button>
                            </div>
                        </div>
                    </div>
                )}

                {toneRefineResultModal && (
                    <div className="toneRefineResultOverlay" onMouseDown={closeToneRefineResultModal}>
                        <div className="toneRefineResultModal" onMouseDown={(e) => e.stopPropagation()}>
                            <button className="toneRefineResultClose" aria-label="결과 닫기" onClick={closeToneRefineResultModal}>×</button>
                            <strong>[{toneRefineResultModal.toneLabel}]</strong>
                            <div>{toneRefineResultModal.before}</div>
                            <span>→</span>
                            <div>{toneRefineResultModal.after}</div>
                            <b>[적용되었습니다]</b>
                        </div>
                    </div>
                )}

                {isPersonalizedRecommendOpen && (
                    <div className="personalizedRecommendOverlay" onMouseDown={() => setIsPersonalizedRecommendOpen(false)}>
                        <div className="personalizedRecommendModal" onMouseDown={(e) => e.stopPropagation()}>
                            <div className="personalizedRecommendHeader">
                                <strong>섬세한 맞춤 메시지</strong>
                                <button onClick={() => setIsPersonalizedRecommendOpen(false)}>×</button>
                            </div>
                            <p>현재 이 방의 어떤 분에게 맞춤 메시지를 추천받고 싶으신가요?</p>
                            <div className="personalizedMemberList">
                                {personalizedCandidateMembers.map(member => (
                                    <button
                                        key={member.publicId}
                                        className={personalizedTargetPublicId === member.publicId ? 'selected' : ''}
                                        onClick={() => setPersonalizedTargetPublicId(member.publicId)}
                                    >
                                        <img src={member.profileImg || '/images/mococo_question.png'} alt="" />
                                        <span>{member.nickname}</span>
                                    </button>
                                ))}
                            </div>
                            {personalizedTargetPublicId && (
                                <div className="personalizedRelationshipButtons">
                                    <button onClick={() => requestPersonalizedMessages('FLIRTING')}>썸타는중</button>
                                    <button onClick={() => requestPersonalizedMessages('CRUSH')}>짝사랑</button>
                                    <button onClick={() => requestPersonalizedMessages('RESPECT')}>존경</button>
                                    <button onClick={() => requestPersonalizedMessages('STRATEGIC')}>가식</button>
                                </div>
                            )}
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
                        {messageContextMenu.message?.messageType === 'TEXT'
                            && !(messageContextMenu.message?.attachments?.length > 0)
                            && <button onClick={() => handleMessageMenuAction('NOTICE')}>공지</button>}
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
                                    className={myReactionMap[`${reactionTargetMessage.messageId}:${reaction.code}`] ? 'selected' : ''}
                                    title={reaction.title}
                                    aria-pressed={Boolean(myReactionMap[`${reactionTargetMessage.messageId}:${reaction.code}`])}
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

                <div className="chattingViewport">
                    <div
                        className='chattingBox'
                        ref={chatListRef}
                        onScroll={handleChatScroll}
                        style={roomBackgroundUrl ? {
                            backgroundImage: `url(${roomBackgroundUrl})`,
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

                            const sender = messageSenderMap[d.senderPublicId] ?? memberMap[d.senderPublicId];
                            const senderNickname = sender?.nickname ?? d.senderNickname ?? '알 수 없음';
                            const senderProfileImg = sender?.profileImg ?? d.senderProfileImg ?? '/images/mococo_question.png';
                            const senderProfile = sender ?? {
                                publicId: d.senderPublicId,
                                nickname: senderNickname,
                                profileImg: senderProfileImg
                            };

                            const isMine = d.senderPublicId === myPublicId;
                            const isDeletedMessage = d.messageStatus === 'DELETED';
                            const replyMessage = d.replyToMessageId
                                ? messageByIdMap.get(Number(d.replyToMessageId))
                                : null;
                            const attachments = Array.isArray(d.attachments) ? d.attachments : [];
                            const reactions = Array.isArray(d.reactions) ? d.reactions : [];
                            const hasImageOnlyAttachment = attachments.length > 0
                                && attachments.every(attachment => attachment.attachmentKind === 'IMAGE')
                                && !d.messageText
                                && !replyMessage;
                            const replyPreviewMessage = replyMessage ?? (
                                d.replyToMessageId
                                    ? {
                                        messageId: d.replyToMessageId,
                                        senderPublicId: null,
                                        messageText: '이전 답장 메시지'
                                    }
                                    : null
                            );

                            return (
                                <div
                                    key={d.messageId}
                                    className={`chatRow ${isMine ? 'mine' : 'other'} ${String(highlightedMessageId) === String(d.messageId) ? 'replyTargetHighlighted' : ''}`}
                                    data-message-id={String(d.messageId)}
                                    onContextMenu={(e) => openMessageContextMenu(e, d)}
                                >
                                    {!isMine && (
                                        <img
                                            className="senderProfileImg clickableProfileImg"
                                            src={senderProfileImg}
                                            alt={senderNickname}
                                            onClick={() => openProfilePopup(senderProfile)}
                                        />
                                    )}

                                    <div className="messageContent">
                                        {!isMine && (
                                            <div className="senderNickname">{senderNickname}</div>
                                        )}

                                        {/* urc/시각은 리액션 유무와 무관하게 항상 말풍선 바로 옆에 붙도록 말풍선과 같은 라인에 둔다. */}
                                        <div className="messageBubbleLine">
                                            {isMine && (
                                                <div className='messageInfo'>
                                                    <div className='unreadCount'>{d.unreadCount}</div>
                                                    <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                                </div>
                                            )}

                                            {isMine && !isDeletedMessage && reactions.length === 0 && (
                                                <button
                                                    className="messageHoverReactionButton"
                                                    title="리액션"
                                                    onClick={(e) => openReactionPicker(e, d)}
                                                >
                                                    ☺
                                                </button>
                                            )}

                                            <div className={`messageWrap ${isDeletedMessage ? 'deleted' : ''} ${hasImageOnlyAttachment ? 'imageOnlyMessage' : ''}`}>
                                                {replyPreviewMessage && !isDeletedMessage && (
                                                    <div
                                                        className="replyPreviewInMessage"
                                                        role="button"
                                                        tabIndex={0}
                                                        onClick={() => scrollToMessage(replyPreviewMessage.messageId)}
                                                        onKeyDown={(e) => {
                                                            if (e.key === 'Enter' || e.key === ' ') {
                                                                e.preventDefault();
                                                                scrollToMessage(replyPreviewMessage.messageId);
                                                            }
                                                        }}
                                                    >
                                                        <div className="replyPreviewSender">
                                                            {messageSenderMap[replyPreviewMessage.senderPublicId]?.nickname
                                                                ?? memberMap[replyPreviewMessage.senderPublicId]?.nickname
                                                                ?? '답장'}
                                                        </div>
                                                        <div className="replyPreviewText">
                                                            {replyPreviewMessage.messageText || '첨부 메시지'}
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
                                                                        onLoad={keepLatestMessageVisibleAfterMediaLoad}
                                                                    />
                                                                ) : attachment.attachmentKind === 'VIDEO' ? (
                                                                    <video
                                                                        className="attachmentVideoPreview"
                                                                        src={attachment.fileUrl}
                                                                        controls
                                                                        onLoadedMetadata={keepLatestMessageVisibleAfterMediaLoad}
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

                                                {(isDeletedMessage || d.messageText) && (
                                                    <div className="messageText">
                                                        {isDeletedMessage ? '삭제된 메시지입니다.' : renderSearchHighlightedText(d.messageText)}
                                                    </div>
                                                )}
                                            </div>

                                            {!isMine && !isDeletedMessage && reactions.length === 0 && (
                                                <button
                                                    className="messageHoverReactionButton"
                                                    title="리액션"
                                                    onClick={(e) => openReactionPicker(e, d)}
                                                >
                                                    ☺
                                                </button>
                                            )}

                                            {!isMine && (
                                                <div className='messageInfo'>
                                                    <div className='unreadCount'>{d.unreadCount}</div>
                                                    <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                                </div>
                                            )}
                                        </div>

                                        {reactions.length > 0 && (
                                            <div className="messageReactionBar">
                                                <div className="messageReactionSummaryList">
                                                    {reactions.map(reaction => (
                                                        <span className="messageReactionBadge" key={reaction.reactionCode}>
                                                            {getReactionLabel(reaction.reactionCode)}
                                                            {` ${Number(reaction.count ?? 0)}`}
                                                        </span>
                                                    ))}
                                                </div>

                                                <button
                                                    className="messageReactionAddButton"
                                                    title="리액션 추가"
                                                    onClick={(e) => openReactionPicker(e, d)}
                                                >
                                                    ☺
                                                </button>

                                                <button
                                                    className="messageReactionMembersButton"
                                                    title="리액션 한 사람"
                                                    onClick={() => openReactionMemberViewer(d)}
                                                >
                                                    <span aria-hidden="true">👤</span>
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })
                        :
                        <div className="emptyChatMessage">친구와 새로운 이야기를 시작해보세요.</div>
                    }

                    </div>

                    {isScrolledAwayFromBottom && (
                        <div className="chatScrollAssist">
                            {newMessageWhileScrolled && (
                                <button className="newMessageWhileScrolled" onClick={scrollChatToBottom}>
                                    <strong>{messageSenderMap[newMessageWhileScrolled.senderPublicId]?.nickname ?? newMessageWhileScrolled.senderNickname ?? '새 메시지'}</strong>
                                    <span>{newMessageWhileScrolled.messageText || (newMessageWhileScrolled.attachments?.length ? '첨부파일을 보냈습니다.' : '새 메시지가 도착했습니다.')}</span>
                                </button>
                            )}
                            <button className="scrollToBottomButton" aria-label="맨 아래로 이동" onClick={scrollChatToBottom}>↓</button>
                        </div>
                    )}
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
                                        background: `conic-gradient(var(--castle-primary) ${uploadProgress.percent * 3.6}deg, #e2e8f0 0deg)`
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
                    <div className="aiActionButtons">
                        <div className="aiRecommendButtonWrap">
                            <button
                                className="aiRecommendButton"
                                onClick={requestAiRecommendedMessages}
                                disabled={isAiProcessing || isUploadingFiles}
                            >
                                {isAiRecommendLoading ? 'AI 추천 받는 중...' : 'AI에게 메시지 추천받기'}
                            </button>

                            <div className="aiRecommendTooltip" role="tooltip">
                                <div>1. 현재 채팅방 메시지를 기준으로 AI가 추천합니다.</div>
                                <div>2. Gemini 3.5 기반이며 503 오류 시 작동하지 않을 수 있습니다.</div>
                                <div>3. 약 12~20초 소요됩니다.</div>
                            </div>
                        </div>

                        <button
                            className="aiToneRefineButton"
                            onClick={() => {
                                setAiRecommendedMessages([]);
                                setIsToneRefinePanelOpen(prev => !prev);
                            }}
                            disabled={isAiProcessing || isUploadingFiles}
                        >
                            {refiningTone ? '말투 다듬는 중...' : 'AI로 말투 다듬기'}
                        </button>

                        <button
                            className="aiPersonalizedRecommendButton"
                            onClick={() => {
                                setIsToneRefinePanelOpen(false);
                                setPersonalizedTargetPublicId('');
                                setIsPersonalizedRecommendOpen(true);
                            }}
                            disabled={isAiProcessing || isUploadingFiles || !roomId}
                        >
                            {isPersonalizedRecommendLoading ? '맞춤 추천 중...' : '섬세한 맞춤 메시지'}
                        </button>
                    </div>

                    {isToneRefinePanelOpen && (
                        <div className="toneRefinePanel">
                            <div className="toneRefinePanelHeader">
                                <strong>변경할 말투 선택</strong>
                                <button aria-label="말투 선택 닫기" onClick={() => setIsToneRefinePanelOpen(false)}>×</button>
                            </div>
                            <div className="toneRefineButtons">
                                <button onClick={() => refineCurrentMessageTone('SOFT')} disabled={Boolean(refiningTone)}>부드럽게</button>
                                <button onClick={() => refineCurrentMessageTone('CONCISE')} disabled={Boolean(refiningTone)}>간결하게</button>
                                <button onClick={() => refineCurrentMessageTone('POLITE')} disabled={Boolean(refiningTone)}>정중하게</button>
                            </div>
                        </div>
                    )}

                    {aiRecommendedMessages.length > 0 && (
                        <div className="aiRecommendList">
                            <button
                                className="aiRecommendCloseButton"
                                aria-label="AI 추천 목록 닫기"
                                onClick={() => setAiRecommendedMessages([])}
                            >
                                ×
                            </button>
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
                            if (isDraftRoom) {
                                alert('첫 메시지 파일 첨부는 방 생성 후 전송해주세요.');
                                return;
                            }

                            fileInputRef.current?.click();
                        }}
                        disabled={isUploadingFiles || isAiProcessing}
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
                    <button onClick={sendChatMessage} disabled={isUploadingFiles || isStartingChat || isAiProcessing}>
                        {isUploadingFiles || isStartingChat ? '전송 중' : '전송'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ChatBox;
