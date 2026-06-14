import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    windows: []
};

const chatWindowsSlice = createSlice({
    name: 'chatWindows',
    initialState,
    reducers: {
        openChatWindow: (state, action) => {
            const room = action.payload;

            const alreadyOpen = state.windows.some(
                win => Number(win.roomId) === Number(room.roomId)
            );

            if (alreadyOpen) {
                state.windows = state.windows.map(win =>
                    Number(win.roomId) === Number(room.roomId)
                        ? { ...win, zIndex: Date.now() }
                        : win
                );
                return;
            }

            state.windows.push({
                roomId: room.roomId,
                roomType: room.roomType,
                roomName: room.displayRoomName || room.roomName,
                friend: room.friend || null,
                memberList: room.memberList || [],
                x: 560 + state.windows.length * 30,
                y: 90 + state.windows.length * 30,
                zIndex: Date.now()
            });
        },

        closeChatWindow: (state, action) => {
            const roomId = action.payload;

            state.windows = state.windows.filter(
                win => Number(win.roomId) !== Number(roomId)
            );
        },

        moveChatWindow: (state, action) => {
            const { roomId, x, y } = action.payload;

            const target = state.windows.find(
                win => Number(win.roomId) === Number(roomId)
            );

            if (target) {
                target.x = x;
                target.y = y;
            }
        },

        focusChatWindow: (state, action) => {
            const roomId = action.payload;

            const target = state.windows.find(
                win => Number(win.roomId) === Number(roomId)
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
