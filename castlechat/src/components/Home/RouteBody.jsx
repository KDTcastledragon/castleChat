import { Route, Routes } from "react-router-dom";

import JoinPage from "../Join/JoinPage";
import LoginPage from "../LogIn/LoginPage";
import FriendList from "./Friends";
import ChatBox from "../Chattings/ChatBox";
import ChatList from "../Chattings/ChatList";
import Settings from "../settings/Settings";
import AdminPage from "../Admin/AdminPage";

function RouteBody({ me, isCheckingLogin, wsRef, isWsConnectedRef, roomHandlersRef }) {

    // ======< return >=======================================================================================================
    return (
        <Routes>

            <Route path="/join" element={<JoinPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<FriendList />} />
            <Route path="/ChatBox" element={<ChatBox />} />
            <Route path="/chatList" element={<ChatList />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/admin" element={<AdminPage />} />

            {/* <Route path="/" element={<Home />} /> */}
        </Routes>
    );
}

export default RouteBody;