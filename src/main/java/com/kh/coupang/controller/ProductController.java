package com.kh.coupang.controller;

import com.kh.coupang.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kh.coupang.service.ProductService;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/*")
public class ProductController {

    @Autowired
    private ProductService product;
    // 메서드명은 Service랑 동일!

    @PostMapping("/product")
    public ResponseEntity<Product> create(@RequestBody Product vo) {
        Product result = product.create(vo);
        if (result != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/product")
    public ResponseEntity<List<Product>> viewAll(@RequestParam(name="category", required = false)Integer category) {
       log.info("category : " + category);
        List<Product> list = product.viewAll();
        return category==null ? ResponseEntity.status(HttpStatus.OK).body(list) :
                                ResponseEntity.status(HttpStatus.OK).body(product.viewCategory(category));
    }

    @GetMapping("/product/{code}")
    public ResponseEntity<Product> view(@PathVariable(name = "code") int code) {
        Product vo = product.view(code);
        return ResponseEntity.status(HttpStatus.OK).body(vo);
    }

    @PutMapping("/product")
    public ResponseEntity<Product> update(@RequestBody Product vo) {
        Product result = product.update(vo);
        // 삼항연산자 활용
        return (result != null) ? ResponseEntity.status(HttpStatus.ACCEPTED).body(result) : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @DeleteMapping("/product/{code}")
    public ResponseEntity<Product> delete(@PathVariable(name="code")int code){
        Product result = product.delete(code);
        return (result != null) ? ResponseEntity.status(HttpStatus.ACCEPTED).body(result) : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
