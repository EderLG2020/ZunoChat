package com.example.backend.module.usermanagement.application;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.usermanagement.domain.BlockedUserModel;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.BlockedUserResponse;
import com.example.backend.module.usermanagement.persistence.BlockedUserRepository;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;
    private final BlockedPairCache blockedPairCache;

    @Transactional
    public void block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId))
            throw new AppException(AppCode.USER_SELF_ACTION);

        userRepository.findById(targetId).orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

        if (blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, targetId))
            throw new AppException(AppCode.USER_ALREADY_BLOCKED);

        blockedUserRepository.save(BlockedUserModel.builder().blockerId(blockerId).blockedId(targetId).build());
        blockedPairCache.invalidate(blockerId, targetId);
    }

    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        if (!blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, targetId))
            throw new AppException(AppCode.USER_NOT_BLOCKED);

        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, targetId);
        blockedPairCache.invalidate(blockerId, targetId);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> listBlocked(Long blockerId) {
        return blockedUserRepository.findAllByBlockerIdOrderByCreatedAtDesc(blockerId).stream()
                .map(b -> {
                    UserModel target = userRepository.findById(b.getBlockedId()).orElse(null);
                    String username = target != null ? target.getUsername() : "usuario eliminado";
                    return new BlockedUserResponse(b.getBlockedId(), username, b.getCreatedAt());
                })
                .toList();
    }
}
