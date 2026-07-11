package com.chat.domserv.usecase;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.user.domain.SessionUserDTO;

public interface ChatCommandUseCase {
	List<ChatAttachmentDTO> uploadChatAttachments(Long roomId, SessionUserDTO uploader, List<MultipartFile> files);

	String uploadCommonImage(SessionUserDTO uploader, MultipartFile file, String imageTarget);

	String uploadJoinProfileImage(MultipartFile file);
}
