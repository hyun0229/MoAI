package com.foureyes.moai.backend.domain.ai.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelEntry {
    private String modelId;
    private String provider;
}