package com.inspire.educlasses.repository;

import com.inspire.educlasses.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    Page<Resource> findAll(Pageable pageable);
}
