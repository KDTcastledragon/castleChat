import { configureStore } from '@reduxjs/toolkit';
import chatWindowsReducer from './chatWindowsSlice';

export const store = configureStore({
    reducer: {
        chatWindows: chatWindowsReducer
    }
});

