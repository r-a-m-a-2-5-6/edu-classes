package com.inspire.educlasses.dto.requestDto;

import com.inspire.educlasses.entity.Difficulty;
import com.inspire.educlasses.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class CreateResourceRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    // Optional field, so no @NotBlank, but we can limit the size just in case
    @Size(max = 5000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @NotNull(message = "Resource type is required (e.g., VIDEO, PDF, ARTICLE)")
    private ResourceType type;

    @NotNull(message = "Difficulty level is required (e.g., BEGINNER, INTERMEDIATE, ADVANCED)")
    private Difficulty difficulty;

    @NotNull(message = "Metadata cannot be null")
    @NotEmpty(message = "Metadata must contain at least one key-value pair")
    private Map<String, String> metadata;
}
