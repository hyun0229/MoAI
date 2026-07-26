package com.foureyes.moai.backend.domain.ai.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ai")
@RequiredArgsConstructor
@Getter
public class AiModelsProperties {
    private final List<ModelEntry> models;
}