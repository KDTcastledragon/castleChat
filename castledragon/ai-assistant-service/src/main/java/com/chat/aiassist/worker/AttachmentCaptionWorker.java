package com.chat.aiassist.worker;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * attach 캡셔닝 비동기 워커. [현재 정책상 미사용]
 *
 * 현재 AI Assist는 비용/복잡도 문제로 VLM/멀티모달 분석을 제외하고 LLM 텍스트 추천만 사용한다.
 * 나중에 이미지/영상 캡셔닝을 다시 넣기로 결정하면 이 worker를 연결한다.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class AttachmentCaptionWorker {

}
