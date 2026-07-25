package com.foureyes.moai.backend.domain.ai.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiModelsProperties {
    private List<ModelEntry> models;
}