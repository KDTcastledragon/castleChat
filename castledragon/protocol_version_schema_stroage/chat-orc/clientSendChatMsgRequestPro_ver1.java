ws.send(JSON.stringify({
        requestId: crypto.randomUUID(),
        wsType: wsType,
        payload: payload
    }));


