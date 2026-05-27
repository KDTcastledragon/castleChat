import { Route, Routes } from "react-router-dom";

import ChatBox from "../Chattings/ChatBox";
import Home from "./Home";
import AdminPage from "../Admin/AdminPage";

function RouteBody() {

    return (
        <Routes>
            <Route path="/Home" element={<Home />} />
            <Route path="/ChatBox" element={<ChatBox />} />
            <Route path="/AdminPage" element={<AdminPage />} />
        </Routes>
    );
}

export default RouteBody;