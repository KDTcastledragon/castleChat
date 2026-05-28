import { Route, Routes } from "react-router-dom";

import ChatBox from "../Chattings/ChatBox";
import Home from "./Home";
import AdminPage from "../Admin/AdminPage";
import JoinPage from "../Join/JoinPage";

function RouteBody() {

    return (
        <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/ChatBox" element={<ChatBox />} />
            <Route path="/AdminPage" element={<AdminPage />} />
            <Route path="/JoinPage" element={<JoinPage />} />

        </Routes>
    );
}

export default RouteBody;