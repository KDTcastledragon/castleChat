import { Route, Routes } from "react-router-dom";

import JoinPage from "../Join/JoinPage";
import LoginPage from "../LogIn/LoginPage";
import Home from "./Home";
import FriendList from "../Friend/FriendList";
import ChatBox from "../Chattings/ChatBox";
import ChatList from "../Chattings/ChatList";
import Settings from "../Settings/Settings";
import AdminPage from "../Admin/AdminPage";

function RouteBody() {

    // ======< return >=======================================================================================================
    return (
        <Routes>

            <Route path="/join" element={<JoinPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<Home />} />
            <Route path="/friendList" element={<FriendList />} />
            <Route path="/ChatBox" element={<ChatBox />} />
            <Route path="/chatList" element={<ChatList />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/admin" element={<AdminPage />} />

        </Routes>
    );
}

export default RouteBody;