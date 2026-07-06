package com.chat.domserv.usecase;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.domain.chatting.ChatAttachmentDTO;
import com.chat.contract.domain.user.SessionUserDTO;

public interface ChatCommandUseCase {
	List<ChatAttachmentDTO> uploadChatAttachments(Long roomId, SessionUserDTO uploader, List<MultipartFile> files);
}
