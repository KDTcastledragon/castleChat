import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    windows: []
};

function getChatWindowKey(room) {
    if (room.chatWindowKey) return room.chatWindowKey;
    if (room.roomId !== null && room.roomId !== undefined) return `room:${room.roomId}`;
    if (room.draftKey) return room.draftKey;
    return `draft:${Date.now()}`;
}

const chatWindowsSlice = createSlice({
    name: 'chatWindows',
    initialState,
    reducers: {
        openChatWindow: (state, action) => {
            const room = action.payload;
            const chatWindowKey = getChatWindowKey(room);

            const alreadyOpen = state.windows.some(
                win => win.chatWindowKey === chatWindowKey
            );

            if (alreadyOpen) {
                state.windows = state.windows.map(win =>
                    win.chatWindowKey === chatWindowKey
                        ? { ...win, zIndex: Date.now() }
                        : win
                );
                return;
            }

            state.windows.push({
                chatWindowKey,
                roomId: room.roomId,
                isDraft: room.isDraft ?? false,
                draftKey: room.draftKey ?? null,
                targetPublicId: room.targetPublicId ?? null,
                inviteMemberPublicIds: room.inviteMemberPublicIds || [],
                roomType: room.roomType,
                roomName: room.displayRoomName || room.roomName,
                roomThumbnail: room.roomThumbnail || room.customRoomThumbnail || null,
                customRoomBackground: room.customRoomBackground || null,
                messageNotificationEnabled: room.messageNotificationEnabled ?? true,
                roomNotice: room.roomNotice || null,
                friend: room.friend || null,
                memberList: room.memberList || [],
                initialMessages: room.initialMessages || [],
                x: 560 + state.windows.length * 30,
                y: 90 + state.windows.length * 30,
                zIndex: Date.now()
            });
        },

        closeChatWindow: (state, action) => {
            const chatWindowKey = action.payload;

            state.windows = state.windows.filter(
                win => win.chatWindowKey !== chatWindowKey
            );
        },

        moveChatWindow: (state, action) => {
            const { chatWindowKey, x, y } = action.payload;

            const target = state.windows.find(
                win => win.chatWindowKey === chatWindowKey
            );

            if (target) {
                target.x = x;
                target.y = y;
            }
        },

        focusChatWindow: (state, action) => {
            const chatWindowKey = action.payload;

            const target = state.windows.find(
                win => win.chatWindowKey === chatWindowKey
            );

            if (target) {
                target.zIndex = Date.now();
            }
        },

        clearChatWindows: (state) => {
            state.windows = [];
        }
    }
});

export const {
    openChatWindow,
    closeChatWindow,
    moveChatWindow,
    focusChatWindow,
    clearChatWindows
} = chatWindowsSlice.actions;

export default chatWindowsSlice.reducer;
