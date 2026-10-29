package com.inspire.educlasses.controller;

import com.inspire.educlasses.dto.requestDto.CreateResourceRequest;
import com.inspire.educlasses.dto.responseDto.CreateResourceResponse;
import com.inspire.educlasses.entity.Difficulty;
import com.inspire.educlasses.entity.Resource;
import com.inspire.educlasses.entity.ResourceType;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.exception.ResourceAccessDeniedException;
import com.inspire.educlasses.exception.ResourceNotFoundException;
import com.inspire.educlasses.service.ResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
public class ResourceControllerTests {

    private static final String BASE_URL = "/api/v1/resources";
    private static final Long USER_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResourceService resourceService;



    //  helpers


    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(token -> token.subject(String.valueOf(USER_ID)));
    }

    private CreateResourceRequest buildRequest() {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle("Machine Learning Math");
        request.setDescription("Linear algebra for AI.");
        request.setCategory("Data Science");
        request.setType(ResourceType.VIDEO);
        request.setDifficulty(Difficulty.ADVANCED);
        request.setMetadata(Map.of("duration", "1h 20m"));
        return request;
    }

    private CreateResourceResponse buildResponse(Long id) {
        User user = new User();
        user.setId(USER_ID);
        user.setName("siva");

        Resource resource = new Resource();
        resource.setId(id);
        resource.setTitle("Machine Learning Math");
        resource.setDescription("Linear algebra for AI.");
        resource.setCategory("Data Science");
        resource.setType(ResourceType.VIDEO);
        resource.setDifficulty(Difficulty.ADVANCED);
        resource.setMetadata(Map.of("duration", "1h 20m"));
        resource.setCreatedBy(user);
        resource.setCreatedAt(LocalDateTime.of(2026, 10, 28, 20, 24, 59));
        resource.setUpdatedAt(LocalDateTime.of(2026, 10, 28, 20, 24, 59));
        return CreateResourceResponse.from(resource);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /** Each case breaks exactly one validation rule of CreateResourceRequest. */
    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("blank title", (Consumer<CreateResourceRequest>) r -> r.setTitle("")),
                Arguments.of("title over 255 chars", (Consumer<CreateResourceRequest>) r -> r.setTitle("a".repeat(256))),
                Arguments.of("description over 5000 chars", (Consumer<CreateResourceRequest>) r -> r.setDescription("a".repeat(5001))),
                Arguments.of("blank category", (Consumer<CreateResourceRequest>) r -> r.setCategory("")),
                Arguments.of("category over 100 chars", (Consumer<CreateResourceRequest>) r -> r.setCategory("a".repeat(101))),
                Arguments.of("null type", (Consumer<CreateResourceRequest>) r -> r.setType(null)),
                Arguments.of("null difficulty", (Consumer<CreateResourceRequest>) r -> r.setDifficulty(null)),
                Arguments.of("null metadata", (Consumer<CreateResourceRequest>) r -> r.setMetadata(null)),
                Arguments.of("empty metadata", (Consumer<CreateResourceRequest>) r -> r.setMetadata(Map.of()))
        );
    }

    //  create

    @Nested
    @DisplayName("POST /api/v1/resources")
    class CreateResource {

        @Test
        void createResource_whenRequestIsValid_returns201AndPassesUserIdFromJwt() throws Exception {
            // Arrange
            when(resourceService.create(any(CreateResourceRequest.class), eq(USER_ID)))
                    .thenReturn(buildResponse(10L));

            // Act + Assert
            mockMvc.perform(post(BASE_URL)
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.title").value("Machine Learning Math"))
                    .andExpect(jsonPath("$.type").value("VIDEO"))
                    .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                    .andExpect(jsonPath("$.createdBy").value("siva"));

            ArgumentCaptor<CreateResourceRequest> captor = ArgumentCaptor.forClass(CreateResourceRequest.class);
            verify(resourceService).create(captor.capture(), eq(USER_ID));
            assertEquals("Machine Learning Math", captor.getValue().getTitle());
            assertEquals("1h 20m", captor.getValue().getMetadata().get("duration"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.inspire.educlasses.controller.ResourceControllerTests#invalidRequests")
        void createResource_whenRequestIsInvalid_returns400AndDoesNotCallService(
                String caseName, Consumer<CreateResourceRequest> breakRequest) throws Exception {

            CreateResourceRequest request = buildRequest();
            breakRequest.accept(request);

            mockMvc.perform(post(BASE_URL)
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verify(resourceService, never()).create(any(), any());
        }

        @Test
        void createResource_whenTypeIsNotAValidEnumValue_returns400() throws Exception {
            String body = """
                    {"title":"t","category":"c","type":"BOOK","difficulty":"ADVANCED","metadata":{"k":"v"}}
                    """;

            mockMvc.perform(post(BASE_URL)
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createResource_whenBodyIsMalformedJson_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"title\": "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createResource_whenNotAuthenticated_returns401() throws Exception {

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildRequest())))
                    .andExpect(status().isUnauthorized());

            verify(resourceService, never()).create(any(), any());
        }
    }

    //  getAll

    @Nested
    @DisplayName("GET /api/v1/resources")
    class GetAllResources {

        @Test
        void getAllResources_whenNoParams_usesDefaultsAndReturnsList() throws Exception {
            // Arrange
            when(resourceService.getAllResources(any(Pageable.class)))
                    .thenReturn(List.of(buildResponse(1L), buildResponse(2L)));

            // Act + Assert
            mockMvc.perform(get(BASE_URL).with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(resourceService).getAllResources(captor.capture());
            Pageable pageable = captor.getValue();
            assertEquals(0, pageable.getPageNumber(), "pageNo is 1-based in the API, 0-based in Spring");
            assertEquals(10, pageable.getPageSize());
            assertEquals(Sort.by("createdAt").ascending(), pageable.getSort());
        }

        @Test
        void getAllResources_whenCustomParams_buildsMatchingPageable() throws Exception {
            when(resourceService.getAllResources(any(Pageable.class))).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                            .with(authenticatedUser())
                            .param("pageNo", "3")
                            .param("pageSize", "5")
                            .param("sortBy", "title")
                            .param("sortDir", "desc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(resourceService).getAllResources(captor.capture());
            Pageable pageable = captor.getValue();
            assertEquals(2, pageable.getPageNumber());
            assertEquals(5, pageable.getPageSize());
            assertEquals(Sort.by("title").descending(), pageable.getSort());
        }

        @Test
        void getAllResources_whenNoResources_returnsEmptyArray() throws Exception {
            when(resourceService.getAllResources(any(Pageable.class))).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL).with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void getAllResources_whenPageSizeIsNotANumber_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL).with(authenticatedUser()).param("pageSize", "abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    //  getResource

    @Nested
    @DisplayName("GET /api/v1/resources/{id}")
    class GetResource {

        @Test
        void getResource_whenResourceExists_returns200() throws Exception {
            when(resourceService.getResource(5L)).thenReturn(buildResponse(5L));

            mockMvc.perform(get(BASE_URL + "/5").with(authenticatedUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.title").value("Machine Learning Math"))
                    .andExpect(jsonPath("$.createdBy").value("siva"));
        }

        @Test
        void getResource_whenResourceNotExist_returns404() throws Exception {
            when(resourceService.getResource(999L))
                    .thenThrow(new ResourceNotFoundException("Resource does not exist for this id: 999"));

            mockMvc.perform(get(BASE_URL + "/999").with(authenticatedUser()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getResource_whenIdIsNotANumber_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/abc").with(authenticatedUser()))
                    .andExpect(status().isBadRequest());

            verify(resourceService, never()).getResource(any());
        }
    }

    //  update

    @Nested
    @DisplayName("PUT /api/v1/resources/{id}")
    class UpdateResource {

        @Test
        void updateResource_whenRequestIsValid_returns200AndPassesIdAndUserId() throws Exception {
            when(resourceService.updateResource(eq(7L), any(CreateResourceRequest.class), eq(USER_ID)))
                    .thenReturn(buildResponse(7L));

            mockMvc.perform(put(BASE_URL + "/7")
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(7));

            verify(resourceService).updateResource(eq(7L), any(CreateResourceRequest.class), eq(USER_ID));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.inspire.educlasses.controller.ResourceControllerTests#invalidRequests")
        void updateResource_whenRequestIsInvalid_returns400AndDoesNotCallService(
                String caseName, Consumer<CreateResourceRequest> breakRequest) throws Exception {

            CreateResourceRequest request = buildRequest();
            breakRequest.accept(request);

            mockMvc.perform(put(BASE_URL + "/7")
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verify(resourceService, never()).updateResource(any(), any(), any());
        }

        @Test
        void updateResource_whenResourceNotExist_returns404() throws Exception {
            when(resourceService.updateResource(eq(999L), any(CreateResourceRequest.class), eq(USER_ID)))
                    .thenThrow(new ResourceNotFoundException("Resource does not exist for this id: 999"));

            mockMvc.perform(put(BASE_URL + "/999")
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateResource_whenUserIsNotOwner_returns403() throws Exception {
            when(resourceService.updateResource(eq(7L), any(CreateResourceRequest.class), eq(USER_ID)))
                    .thenThrow(new ResourceAccessDeniedException("You are not the owner."));

            mockMvc.perform(put(BASE_URL + "/7")
                            .with(authenticatedUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    //  delete

    @Nested
    @DisplayName("DELETE /api/v1/resources/{id}")
    class DeleteResource {

        @Test
        void deleteResource_whenOwnerDeletes_returns204AndPassesIdAndUserId() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/7")
                            .with(authenticatedUser()).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(resourceService).deleteResource(7L, USER_ID);
        }

        @Test
        void deleteResource_whenResourceNotExist_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Resource does not exist for this id: 999"))
                    .when(resourceService).deleteResource(999L, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/999")
                            .with(authenticatedUser()).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteResource_whenUserIsNotOwner_returns403() throws Exception {
            doThrow(new ResourceAccessDeniedException("You are not the owner."))
                    .when(resourceService).deleteResource(7L, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/7")
                            .with(authenticatedUser()).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
