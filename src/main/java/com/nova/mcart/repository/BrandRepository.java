package com.nova.mcart.repository;

import com.nova.mcart.entity.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    @Query("select lower(b.name) from Brand b")
    List<String> findAllNamesLowerCase();

    Optional<Brand> findByNameIgnoreCase(String name);
}
