package com.kh.coupang.controller;

import com.kh.coupang.domain.*;
import com.kh.coupang.service.CategoryService;
import com.kh.coupang.service.ProductCommentService;
import com.kh.coupang.service.ProductService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/*")
@CrossOrigin(origins = {"*"}, maxAge = 6000)
public class ProductController {

    @Autowired
    private ProductService product;

    @Autowired
    private ProductCommentService comment;

    @Autowired
    private CategoryService category;

    @Value("${spring.servlet.multipart.location}")
    private String uploadPath; // D:\\upload

    // 카테고리 가져오기
    @GetMapping("/public/category")
    public ResponseEntity<List<CategoryDTO>> categoryView() {
        List<Category> topList = category.getTopLevelCategory();
        List<CategoryDTO> response = new ArrayList<>();

        for(Category item : topList) {
            CategoryDTO dto = CategoryDTO.builder()
                    .cateIcon(item.getCateIcon())
                    .cateName(item.getCateName())
                    .cateCode(item.getCateCode())
                    .cateUrl(item.getCateUrl())
                    .subCategories(category.getBottomLevelCategory(item.getCateCode()))
                    .build();
            response.add(dto);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/product")
    public ResponseEntity<Product> create(ProductDTO dto) throws IOException {

        // 파일 업로드
        String fileName = dto.getFile().getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String saveName = uploadPath + File.separator + "product" + File.separator + uuid + "_" + fileName;
        Path savePath = Paths.get(saveName);
        dto.getFile().transferTo(savePath); // 파일 업로드 실제로 일어나고 있음!

        // Product vo 값들 담아서 요청!
        Product vo = new Product();
        vo.setProdName(dto.getProdName());
        vo.setPrice(dto.getPrice());
        vo.setProdPhoto(saveName);

        Category category = new Category();
        category.setCateCode(dto.getCateCode());
        vo.setCategory(category);

        Product result = product.create(vo);
        if (result != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/public/product")
    public ResponseEntity<List<Product>> viewAll(@RequestParam(name="category", required = false) Integer category, @RequestParam(name="page", defaultValue = "1") int page) {
        Sort sort = Sort.by("prodCode").descending();
        Pageable pageable = PageRequest.of(page-1, 10, sort);

        // QueryDSL
        // 1. 가장 먼저 동적 처리하기 위한 Q도메인 클래스 얻어오기
        // Q도메인 클래스를 이용하면 Entity 클래스에 선언된 필드들을 변수로 활용할 수 있음
        QProduct qProduct = QProduct.product;

        // 2. BooleanBuilder : where문에 들어가는 조건들을 넣어주는 컨테이너
        BooleanBuilder builder = new BooleanBuilder();

        if(category!=null) {
            // 3. 원하는 조건은 필드값과 같이 결합해서 생성
            BooleanExpression expression = qProduct.category.cateCode.eq(category);

            // 4. 만들어진 조건은 where문에 and나 or 같은 키워드와 결합
            builder.and(expression);
        }

        // 5. BooleanBuilder는 QuerydslPredicateExcutor 인터페이스의 findAll() 사용
        Page<Product> list = product.viewAll(pageable, builder);

        return ResponseEntity.status(HttpStatus.OK).body(list.getContent());
    }

    @PutMapping("/product")
    public ResponseEntity<Product> update(ProductDTO dto) throws IOException {

        Product vo = new Product();
        vo.setProdCode(dto.getProdCode());
        vo.setPrice(dto.getPrice());
        vo.setProdName(dto.getProdName());

        Category category = new Category();
        category.setCateCode(dto.getCateCode());
        vo.setCategory(category);

        // 기존 데이터를 가져와야 하는 상황!
        Product prev = product.view(dto.getProdCode());

        if(dto.getFile().isEmpty()) {
            // 만약 새로운 사진이 없는 경우 -> 기존 사진 경로 그대로 vo로 담아내야 한다!
            vo.setProdPhoto(prev.getProdPhoto());
        } else {
            // 기존 사진은 삭제하고, 새로운 사진을 추가
            File file = new File(prev.getProdPhoto());
            file.delete();

            String fileName = dto.getFile().getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String saveName = uploadPath + File.separator + "product" + File.separator + uuid + "_" + fileName;
            Path savePath = Paths.get(saveName);
            dto.getFile().transferTo(savePath);

            vo.setProdPhoto(saveName);
        }

        Product result = product.update(vo);
        // 삼항연산자 활용
        return (result != null) ?
                ResponseEntity.status(HttpStatus.ACCEPTED).body(result) :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @DeleteMapping("/product/{code}")
    public ResponseEntity<Product> delete(@PathVariable(name="code") int code) {
        // 파일 삭제 로직
        Product prev = product.view(code);
        File file = new File(prev.getProdPhoto());
        file.delete();

        Product result = product.delete(code);
        return (result != null) ?
                ResponseEntity.status(HttpStatus.ACCEPTED).body(result) :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // 상품 1개 조회
    @GetMapping("public/product/{code}")
    public ResponseEntity<Product> view(@PathVariable(name = "code") int code) {
        Product vo = product.view(code);
        return ResponseEntity.status(HttpStatus.OK).body(vo);
    }

    // 상품 댓글 추가
    @PostMapping("/product/comment")
    public ResponseEntity createComment(@RequestBody ProductComment vo) {

        // 시큐리티에 담은 로그인한 사용자의 정보 가져오기
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        Object principal = authentication.getPrincipal();

        if(principal instanceof User) {
            User user = (User) principal;
            vo.setUser(user);
            return ResponseEntity.ok(comment.create(vo));
        }

        return ResponseEntity.badRequest().build();
    }

    // 상품 1개에 따른 댓글 조회 -> 전체 다 보여줘야 하는 상황!
    @GetMapping("/public/product/{code}/comment")
    public ResponseEntity<List<ProductCommentDTO>> viewComment(@PathVariable(name="code") int code) {
        List<ProductComment> topList = comment.getTopLevelComments(code);
        List<ProductCommentDTO> response = commentDetailList(topList, code);

        return ResponseEntity.ok(response);
    }

    public List<ProductCommentDTO> commentDetailList(List<ProductComment> comments, int code) {
        List<ProductCommentDTO> response = new ArrayList<>();

        for(ProductComment item : comments) {
            List<ProductComment> replies = comment.getRepliesComments(item.getProComCode(), code);
            List<ProductCommentDTO> repliesDTO = commentDetailList(replies, code);
            ProductCommentDTO dto = commentDetail(item);
            dto.setReplies(repliesDTO);
            response.add(dto);
        }

        return response;
    }

    public ProductCommentDTO commentDetail(ProductComment vo) {
        return ProductCommentDTO.builder()
                .prodCode(vo.getProdCode())
                .proComCode(vo.getProComCode())
                .proComDesc(vo.getProComDesc())
                .proComDate(vo.getProComDate())
                .user(UserDTO.builder()
                        .id(vo.getUser().getId())
                        .name(vo.getUser().getName())
                        .build())
                .build();
    }

}