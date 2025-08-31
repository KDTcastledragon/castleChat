import { Route, Routes } from "react-router-dom";

import ChatBox from "../Chattings/ChatBox";
import Home from "./Home";

function RouteBody() {

    return (
        <Routes>
            <Route path="/Home" element={<Home />} />
            <Route path="/ChatBox" element={<ChatBox />} />
        </Routes>
    );
}

export default RouteBody;