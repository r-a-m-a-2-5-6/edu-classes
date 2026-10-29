package com.inspire.educlasses.service;

import com.inspire.educlasses.dto.requestDto.CreateResourceRequest;
import com.inspire.educlasses.dto.responseDto.CreateResourceResponse;
import com.inspire.educlasses.entity.Resource;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.exception.ResourceAccessDeniedException;
import com.inspire.educlasses.exception.ResourceNotFoundException;
import com.inspire.educlasses.repository.ResourceRepository;
import com.inspire.educlasses.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    // Create resource
    @Transactional
    public CreateResourceResponse create(CreateResourceRequest request,
                                         Long userId){

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setCategory(request.getCategory());
        resource.setType(request.getType());
        resource.setMetadata(request.getMetadata());
        resource.setDifficulty(request.getDifficulty());

        User createdByRef = userRepository.getReferenceById(userId);
        resource.setCreatedBy(createdByRef);

        return CreateResourceResponse.from(resourceRepository.save(resource));
    }

    //Get All Resources

    @Transactional(readOnly = true)
    public List<CreateResourceResponse> getAllResources(Pageable pageable){

        Page<Resource> resourcePage = resourceRepository.findAll(pageable);



        return resourcePage.map(CreateResourceResponse::from).getContent();
    }

    //Get a single Resource By id

    @Transactional(readOnly = true)
    public CreateResourceResponse getResource(Long id){

        return CreateResourceResponse.from(getResourceById(id));
    }

    // Update Resource
    @Transactional
    public CreateResourceResponse updateResource(Long id,
                                                 CreateResourceRequest request,
                                                 Long userId){

        Resource resource = getResourceById(id);

        if(!resource.getCreatedBy().getId().equals(userId)){
            throw  new ResourceAccessDeniedException("You do not have permission to update this resource. You are not the owner.");
        }

        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setCategory(request.getCategory());
        resource.setType(request.getType());
        resource.setMetadata(request.getMetadata());
        resource.setDifficulty(request.getDifficulty());

        return CreateResourceResponse.from(resourceRepository.save(resource));

    }

    //DeleteResource
    @Transactional
    public void deleteResource(Long id,Long userId){
        Resource resource = getResourceById(id);

        if(!resource.getCreatedBy().getId().equals(userId)){
            throw new ResourceAccessDeniedException("You do not have permission to delete this resource. You are not the owner.");
        }

        resourceRepository.delete(resource);

    }


    // function to return get and return resource object
    private Resource getResourceById(Long id){

        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource does not exist for this id: "+id));
    }

}
