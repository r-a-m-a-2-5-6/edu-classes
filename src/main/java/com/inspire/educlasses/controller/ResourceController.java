package com.inspire.educlasses.controller;

import com.inspire.educlasses.dto.requestDto.CreateResourceRequest;
import com.inspire.educlasses.dto.responseDto.CreateResourceResponse;
import com.inspire.educlasses.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {


    private final ResourceService resourceService;

    //1.create a resource

    @PostMapping
    public ResponseEntity<CreateResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest resourceRequest,
            @AuthenticationPrincipal Jwt jwt
            ){

        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(resourceRequest,userId));
    }

    //2.get all resources
    @GetMapping
    public ResponseEntity<List<CreateResourceResponse>> getAllResources(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ){

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.DESC.name())
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending() ;

        Pageable pageRequest = PageRequest.of(pageNo-1,pageSize,sort);

        return ResponseEntity.ok(resourceService.getAllResources(pageRequest));
    }

    //3. get resources by id
    @GetMapping("/{id}")
    public ResponseEntity<CreateResourceResponse> getResource(
            @PathVariable Long id){

        return ResponseEntity.ok(resourceService.getResource(id));
    }

    //4.update resources

    @PutMapping("/{id}")
    public ResponseEntity<CreateResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody CreateResourceRequest resourceRequest,
            @AuthenticationPrincipal Jwt jwt){

        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject()));

        return ResponseEntity.ok(resourceService.updateResource(id,resourceRequest,userId));
    }

    //5.Delete resources
    @DeleteMapping("/{id}")
    public ResponseEntity<CreateResourceResponse> deleteResource(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt){

        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
        resourceService.deleteResource(id,userId);
        return ResponseEntity.noContent().build();
    }

}
