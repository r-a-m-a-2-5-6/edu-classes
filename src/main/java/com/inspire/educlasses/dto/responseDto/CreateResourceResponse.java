package com.inspire.educlasses.dto.responseDto;

import com.inspire.educlasses.entity.Difficulty;
import com.inspire.educlasses.entity.Resource;
import com.inspire.educlasses.entity.ResourceType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Difficulty difficulty;
    private ResourceType type;
    private Map<String, String> metadata;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CreateResourceResponse from(Resource resource){
        return CreateResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .category(resource.getCategory())
                .difficulty(resource.getDifficulty())
                .type(resource.getType())
                .metadata(resource.getMetadata())
                .createdBy(resource.getCreatedBy().getName())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

}

