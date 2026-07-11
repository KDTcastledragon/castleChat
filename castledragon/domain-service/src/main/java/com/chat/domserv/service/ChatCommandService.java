package com.chat.domserv.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.room.domain.RoomMembersDTO;
import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.domserv.mapper.DomServChatMapper;
import com.chat.domserv.mapper.RoomMapper;
import com.chat.domserv.usecase.ChatCommandUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatCommandService implements ChatCommandUseCase {
	private static final long MAX_TOTAL_UPLOAD_BYTES = 320L * 1024 * 1024;
	private static final long MAX_PROFILE_IMAGE_BYTES = 10L * 1024 * 1024;
	private static final int MAX_FILE_COUNT = 20;

	private final DomServChatMapper domServChatMapper;
	private final RoomMapper roomMapper;

	@Value("${chat.attachment.upload-dir:uploads/chat}")
	private String uploadDir;

	@Value("${chat.attachment.public-prefix:/uploads/chat}")
	private String publicPrefix;

	@Override
	@Transactional
	public List<ChatAttachmentDTO> uploadChatAttachments(Long roomId, SessionUserDTO uploader, List<MultipartFile> files) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (uploader == null || uploader.getUserId() == null) {
			throw new IllegalArgumentException("로그인 정보가 없습니다.");
		}

		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("업로드할 파일이 없습니다.");
		}

		if (files.size() > MAX_FILE_COUNT) {
			throw new IllegalArgumentException("한 번에 업로드 가능한 파일 수를 초과했습니다.");
		}

		RoomMembersDTO memberInfo = roomMapper.getActiveRoomMemberInfoInRoom(roomId, uploader.getUserId());

		if (memberInfo == null) {
			throw new IllegalArgumentException("현재 채팅방의 멤버가 아닙니다.");
		}

		long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();

		if (totalSize > MAX_TOTAL_UPLOAD_BYTES) {
			throw new IllegalArgumentException("업로드 용량은 최대 320MB까지 가능합니다.");
		}

		List<ChatAttachmentDTO> uploadedAttachments = new ArrayList<>();

		for (int i = 0; i < files.size(); i++) {
			MultipartFile file = files.get(i);

			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
			}

			uploadedAttachments.add(saveOneAttachment(roomId, uploader.getUserId(), file, i));
		}

		return uploadedAttachments;
	}

	@Override
	public String uploadCommonImage(SessionUserDTO uploader, MultipartFile file, String imageTarget) {
		if (uploader == null || uploader.getUserId() == null) {
			throw new IllegalArgumentException("로그인 정보가 없습니다.");
		}

		return saveCommonImage(file, imageTarget, MAX_TOTAL_UPLOAD_BYTES, "업로드 용량은 최대 320MB까지 가능합니다.");
	}

	@Override
	public String uploadJoinProfileImage(MultipartFile file) {
		return saveCommonImage(file, "PROFILE_IMAGE", MAX_PROFILE_IMAGE_BYTES, "프로필 이미지는 최대 10MB까지 가능합니다.");
	}

	private String saveCommonImage(MultipartFile file, String imageTarget, long maxUploadBytes, String maxUploadMessage) {

		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
		}

		if (file.getSize() > maxUploadBytes) {
			throw new IllegalArgumentException(maxUploadMessage);
		}

		String contentType = file.getContentType();

		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
		}

		String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());

		if (originalFileName.contains("..")) {
			throw new IllegalArgumentException("잘못된 파일명입니다.");
		}

		String safeTarget = hasText(imageTarget) ? imageTarget.toUpperCase().replaceAll("[^A-Z0-9_-]", "_") : "COMMON";
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

		Path commonUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("common").resolve(safeTarget);
		Path targetPath = commonUploadDir.resolve(storedFileName).normalize();

		try {
			Files.createDirectories(commonUploadDir);

			try (InputStream fileInputStream = file.getInputStream()) {
				Files.copy(fileInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}

			return publicPrefix + "/common/" + safeTarget + "/" + storedFileName;
		} catch (IOException e) {
			throw new IllegalStateException("이미지 저장 실패", e);
		}
	}

	private ChatAttachmentDTO saveOneAttachment(Long roomId, Long uploaderUserId, MultipartFile file, int sortOrder) {
		String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());

		if (originalFileName.contains("..")) {
			throw new IllegalArgumentException("잘못된 파일명입니다.");
		}

		if (originalFileName.length() > 255) {
			throw new IllegalArgumentException("파일명이 너무 깁니다.");
		}

		String contentType = file.getContentType();

		if (contentType == null || contentType.isBlank()) {
			contentType = "application/octet-stream";
		}

		String attachmentKind = resolveAttachmentKind(contentType);
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

		Path roomUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(String.valueOf(roomId));
		Path targetPath = roomUploadDir.resolve(storedFileName).normalize();

		Integer width = null;
		Integer height = null;

		try {
			Files.createDirectories(roomUploadDir);

			if ("IMAGE".equals(attachmentKind)) {
				try (InputStream imageInputStream = file.getInputStream()) {
					BufferedImage image = ImageIO.read(imageInputStream);

					if (image != null) {
						width = image.getWidth();
						height = image.getHeight();
					}
				}
			}

			try (InputStream fileInputStream = file.getInputStream()) {
				Files.copy(fileInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}

			String fileUrl = publicPrefix + "/" + roomId + "/" + storedFileName;

			ChatAttachmentDTO dto = new ChatAttachmentDTO();
			dto.setMessageId(null);
			dto.setRoomId(roomId);
			dto.setUploaderUserId(uploaderUserId);
			dto.setFileUrl(fileUrl);
			dto.setOriginalFileName(originalFileName);
			dto.setContentType(contentType);
			dto.setFileSize(file.getSize());
			dto.setAttachmentKind(attachmentKind);
			dto.setAttachmentStatus("TEMP");
			dto.setWidth(width);
			dto.setHeight(height);
			dto.setDurationMs(null);
			dto.setSortOrder(sortOrder);

			int inserted = domServChatMapper.insertChatAttachment(dto);

			if (inserted != 1 || dto.getAttachmentId() == null) {
				Files.deleteIfExists(targetPath);
				throw new IllegalStateException("첨부파일 DB 저장 실패");
			}

			return dto;
		} catch (IOException e) {
			throw new IllegalStateException("첨부파일 저장 실패", e);
		}
	}

	private String resolveAttachmentKind(String contentType) {
		if (contentType.startsWith("image/")) {
			return "IMAGE";
		}

		if (contentType.startsWith("video/")) {
			return "VIDEO";
		}

		if (contentType.startsWith("audio/")) {
			return "AUDIO";
		}

		return "FILE";
	}

	private String extractExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");

		if (dotIndex < 0) {
			return "";
		}

		return fileName.substring(dotIndex).toLowerCase();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
