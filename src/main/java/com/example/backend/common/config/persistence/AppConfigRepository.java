package com.example.backend.common.config.persistence;

import com.example.backend.common.config.domain.AppConfigModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfigModel, String> {
}