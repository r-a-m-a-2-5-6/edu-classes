package com.inspire.educlasses.service;

import com.inspire.educlasses.dto.requestDto.CreateResourceRequest;
import com.inspire.educlasses.dto.responseDto.CreateResourceResponse;
import com.inspire.educlasses.entity.Difficulty;
import com.inspire.educlasses.entity.Resource;
import com.inspire.educlasses.entity.ResourceType;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.exception.ResourceAccessDeniedException;
import com.inspire.educlasses.exception.ResourceNotFoundException;
import com.inspire.educlasses.repository.ResourceRepository;
import com.inspire.educlasses.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTests {

    private static final Long OWNER_ID = 5L;
    private static final Long OTHER_USER_ID = 6L;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceService resourceService;

    private User buildUser() {
        User user = new User();
        user.setName("siva");
        user.setId(OWNER_ID);
        return user;
    }

    private Resource buildResource(Long id) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setTitle("Machine Learning Math");
        resource.setDescription("Linear algebra for AI.");
        resource.setCategory("Data Science");
        resource.setDifficulty(Difficulty.ADVANCED);
        resource.setType(ResourceType.VIDEO);
        resource.setMetadata(Map.of("duration", "1h 20m"));
        resource.setCreatedBy(buildUser());
        resource.setCreatedAt(LocalDateTime.of(2026, 10, 28, 20, 24, 59));
        resource.setUpdatedAt(LocalDateTime.of(2026, 10, 28, 20, 24, 59));
        return resource;
    }

    private CreateResourceRequest buildResourceRequest() {
        CreateResourceRequest resourceRequest = new CreateResourceRequest();
        resourceRequest.setTitle("Machine Learning Math");
        resourceRequest.setDescription("Linear algebra for AI.");
        resourceRequest.setCategory("Data Science");
        resourceRequest.setDifficulty(Difficulty.ADVANCED);
        resourceRequest.setType(ResourceType.VIDEO);
        resourceRequest.setMetadata(Map.of("duration", "1h 20m"));
        return resourceRequest;
    }

    //  create

    @Nested
    @DisplayName("create")
    class CreateResource {

        @Test
        void create_whenRequestIsValid_savesResourceWithCreator() {
            // Arrange
            User user = buildUser();
            CreateResourceRequest request = buildResourceRequest();

            when(userRepository.getReferenceById(OWNER_ID)).thenReturn(user);
            when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
                Resource saved = invocation.getArgument(0);
                saved.setId(10L);
                saved.setCreatedAt(LocalDateTime.of(2026, 10, 28, 20, 24, 59));
                return saved;
            });

            // Act
            CreateResourceResponse result = resourceService.create(request, OWNER_ID);

            // Assert
            ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
            verify(resourceRepository).save(captor.capture());
            Resource savedEntity = captor.getValue();

            assertEquals("Machine Learning Math", savedEntity.getTitle());
            assertEquals("Linear algebra for AI.", savedEntity.getDescription());
            assertEquals("Data Science", savedEntity.getCategory());
            assertEquals(ResourceType.VIDEO, savedEntity.getType());
            assertEquals(Difficulty.ADVANCED, savedEntity.getDifficulty());
            assertEquals("1h 20m", savedEntity.getMetadata().get("duration"));
            assertSame(user, savedEntity.getCreatedBy());

            assertEquals(10L, result.getId());
            assertEquals("siva", result.getCreatedBy());
            verify(userRepository).getReferenceById(OWNER_ID);
        }

        @Test
        void create_whenDBIsDown_throwsException() {
            // Arrange
            when(userRepository.getReferenceById(OWNER_ID)).thenReturn(buildUser());
            when(resourceRepository.save(any(Resource.class)))
                    .thenThrow(new DataAccessResourceFailureException("The system is temporarily unable to reach required backend services"));

            // Act + Assert
            assertThrows(
                    DataAccessResourceFailureException.class,
                    () -> resourceService.create(buildResourceRequest(), OWNER_ID)
            );
            verify(resourceRepository).save(any(Resource.class));
        }
    }

    // getResource

    @Nested
    @DisplayName("getResource")
    class GetResource {

        @Test
        void getResource_whenResourceExist() {
            // Arrange
            Resource resource = buildResource(5L);

            when(resourceRepository.findById(5L))
                    .thenReturn(Optional.of(resource));

            // Act
            CreateResourceResponse result = resourceService.getResource(5L);

            // Assert
            assertEquals(5L, result.getId());
            assertEquals("Machine Learning Math", result.getTitle());
            assertEquals(ResourceType.VIDEO, result.getType());
            assertEquals(Difficulty.ADVANCED, result.getDifficulty());
            assertEquals("siva", result.getCreatedBy(), "Creator name should be extracted from User entity");
            assertEquals(2026, result.getCreatedAt().getYear());
            assertEquals(24, result.getCreatedAt().getMinute());

            verify(resourceRepository).findById(5L);
        }

        @Test
        void getResource_whenResourceNotExist_throwsException() {
            // Arrange
            when(resourceRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // Act + Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> resourceService.getResource(999L)
            );
            assertTrue(exception.getMessage().contains("999"));
        }
    }

    //  getAllResources

    @Nested
    @DisplayName("getAllResources")
    class GetAllResources {

        @Test
        void getAllResources_whenResourcesExist_returnsMappedList() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Resource> page = new PageImpl<>(
                    List.of(buildResource(1L), buildResource(2L)), pageable, 2);

            when(resourceRepository.findAll(pageable)).thenReturn(page);

            // Act
            List<CreateResourceResponse> result = resourceService.getAllResources(pageable);

            // Assert
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
            assertEquals("siva", result.get(0).getCreatedBy());
            verify(resourceRepository).findAll(pageable);
        }

        @Test
        void getAllResources_whenNoResources_returnsEmptyList() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(resourceRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            // Act
            List<CreateResourceResponse> result = resourceService.getAllResources(pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    //  updateResource

    @Nested
    @DisplayName("updateResource")
    class UpdateResource {

        @Test
        void updateResource_whenOwnerUpdates_updatesFieldsAndSaves() {
            // Arrange
            Resource existing = buildResource(5L);
            CreateResourceRequest request = buildResourceRequest();
            request.setTitle("Linear Algebra Deep Dive");
            request.setDescription("Updated description");
            request.setCategory("Mathematics");
            request.setMetadata(Map.of("duration", "2h"));

            when(resourceRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(resourceRepository.save(any(Resource.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CreateResourceResponse result = resourceService.updateResource(5L, request, OWNER_ID);

            // Assert
            assertEquals("Linear Algebra Deep Dive", result.getTitle());
            assertEquals("Updated description", existing.getDescription());
            assertEquals("Mathematics", existing.getCategory());
            assertEquals("2h", existing.getMetadata().get("duration"));
            assertEquals("siva", result.getCreatedBy(), "Creator must not change on update");
            verify(resourceRepository).save(existing);
        }

        @Test
        void updateResource_whenResourceNotExist_throwsExceptionAndDoesNotSave() {
            // Arrange
            when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> resourceService.updateResource(999L, buildResourceRequest(), OWNER_ID)
            );
            verify(resourceRepository, never()).save(any());
        }

        @Test
        void updateResource_whenUserIsNotOwner_throwsAccessDeniedAndDoesNotModify() {
            // Arrange
            Resource existing = buildResource(5L);
            CreateResourceRequest request = buildResourceRequest();
            request.setTitle("Hacked Title");

            when(resourceRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act + Assert
            assertThrows(
                    ResourceAccessDeniedException.class,
                    () -> resourceService.updateResource(5L, request, OTHER_USER_ID)
            );
            assertEquals("Machine Learning Math", existing.getTitle(), "Entity must stay untouched");
            verify(resourceRepository, never()).save(any());
        }
    }

    //  deleteResource

    @Nested
    @DisplayName("deleteResource")
    class DeleteResource {

        @Test
        void deleteResource_whenOwnerDeletes_deletesResource() {
            // Arrange
            Resource existing = buildResource(5L);
            when(resourceRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act
            resourceService.deleteResource(5L, OWNER_ID);

            // Assert
            verify(resourceRepository).delete(existing);
        }

        @Test
        void deleteResource_whenResourceNotExist_throwsExceptionAndDoesNotDelete() {
            // Arrange
            when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> resourceService.deleteResource(999L, OWNER_ID)
            );
            verify(resourceRepository, never()).delete(any());
        }

        @Test
        void deleteResource_whenUserIsNotOwner_throwsAccessDeniedAndDoesNotDelete() {
            // Arrange
            Resource existing = buildResource(5L);
            when(resourceRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act + Assert
            assertThrows(
                    ResourceAccessDeniedException.class,
                    () -> resourceService.deleteResource(5L, OTHER_USER_ID)
            );
            verify(resourceRepository, never()).delete(any());
        }
    }
}