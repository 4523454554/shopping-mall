package com.jay.shoppingmall.domain.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByNameContaining(@Param("q") String name, Pageable pageable);

    Page<Item> findBySellerId(Long id, Pageable pageable);

//    List<Item> findFirst3BySellerId(Long sellerId);

//    Slice<Item> findAll(Pageable pageable);
}
