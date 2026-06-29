package com.chat.wsgate.exception;

public class WsAuthException extends RuntimeException {
	private static final long serialVersionUID = 1L; // 아래는 이걸 선언하지 않았을때의 WsAuthException 경고.
	// The serializable class WsAuthException does not declare a static final serialVersionUID field of type long.
	// --> RuntimeException은 내부적으로 Serializable 계열이라서, Java/Eclipse가 이렇게 말하는 거야. 직렬화될 수도 있는 클래스인데 serialVersionUID 안 적었네?

	public WsAuthException(String exceptionMessage) {
		super(exceptionMessage);
	}
}