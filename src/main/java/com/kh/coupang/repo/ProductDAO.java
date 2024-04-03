package com.kh.coupang.repo;

import com.kh.coupang.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface ProductDAO extends JpaRepository<Product, Integer>, QuerydslPredicateExecutor<Product> {

    // 특정 카테고리의 모든 상품 조회
    @Query(value="SELECT * FROM product WHERE cate_code=:code", nativeQuery = true)
    Page<Product> findByCateCode(@Param("code") Integer code, Pageable pageable);

}