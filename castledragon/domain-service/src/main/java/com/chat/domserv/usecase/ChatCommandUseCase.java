package com.chat.domserv.usecase;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.domain.ChatAttachmentDTO;
import com.chat.contract.domain.SessionUserDTO;

public interface ChatCommandUseCase {
	List<ChatAttachmentDTO> uploadChatAttachments(Long roomId, SessionUserDTO uploader, List<MultipartFile> files);
}
