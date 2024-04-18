package com.kh.coupang.controller;

import com.kh.coupang.domain.*;
import com.kh.coupang.service.ReviewCommentService;
import com.kh.coupang.service.ReviewService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
public class ReviewController {

    @Autowired
    private ReviewService review;

    @Autowired
    private ReviewCommentService comment;

    @Value("${spring.servlet.multipart.location}")
    private String uploadPath;

    @PostMapping("/review")
    public ResponseEntity<Review> create(ReviewDTO dto) throws IOException {

        // review부터 추가하여 revi_code가 담긴 review!
        Review vo = new Review();

        vo.setId(dto.getId());
        vo.setProdCode(dto.getProdCode());
        vo.setReviTitle(dto.getReviTitle());
        vo.setReviDesc(dto.getReviDesc());
        vo.setRating(dto.getRating());

        Review result = review.create(vo);

        log.info("result : " + result);

        if(dto.getFiles() != null) {
            // review_image에는 revi_code가 필요!
            for(MultipartFile file : dto.getFiles()) {
                ReviewImage imgVo = new ReviewImage();

                String fileName = file.getOriginalFilename();
                String uuid = UUID.randomUUID().toString();
                String saveName = uploadPath + File.separator + "review" + File.separator + uuid + "_" + fileName;
                Path savePath = Paths.get(saveName);
                file.transferTo(savePath);

                imgVo.setReviUrl(saveName);
                imgVo.setReview(result);

                review.createImg(imgVo);
            }
        }


        return result!=null ?
                ResponseEntity.status(HttpStatus.CREATED).body(result) :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // http://localhost:8080/api/public/product/43/review
    // 상품 1개에 따른 리뷰 전체 보기
    @GetMapping("/public/product/{code}/review")
    public ResponseEntity<List<Review>> viewAll(@RequestParam(name="page", defaultValue = "1") int page, @PathVariable(name="code") int code) {

        Sort sort = Sort.by("reviCode").descending();
        Pageable pageable = PageRequest.of(page-1, 5, sort);

        QReview qReview = QReview.review;

        BooleanBuilder builder = new BooleanBuilder();
        BooleanExpression expression = qReview.prodCode.eq(code);
        builder.and(expression);

        return ResponseEntity.status(HttpStatus.OK).body(review.viewAll(pageable, builder).getContent());
    }

    @DeleteMapping("/review/{code}")
    public ResponseEntity delete(@PathVariable(name="code") int code) {
        // code : 리뷰 코드! reviCode
        // 이미지들 삭제하면서 리뷰 이미지 테이블에서도 삭제
        // 1. 이미지 테이블에서 해당 reviCode에 대한 이미지들 가지고 와야죠! (List<ReviewImage>)
        //      ---> SELECT 문 생각해보고! DAO에 추가해서 Service에 반영해서 가지고 오면 됨!
        //      ---> QueryDSL 방법으로도 가지고 올 수 있죠!
        List<ReviewImage> images = review.viewImages(code);

        for(ReviewImage image : images) {
            // 2. 반복문을 돌려서 각각의 image에 있는 URL(reviUrl)로 File 객체로 file.delete() 사용!
            //  ---> 실제 폴더에 있는 이미지 파일 삭제
            File file = new File(image.getReviUrl());
            file.delete();

            // 3. 반복문 안에서 그와 동시에 이미지 테이블에서 이미지의 Code로 삭제 기능 진행(reviImgCode)
            //  ---> DB에 저장한 이미지 정보 삭제
            review.deleteImage(image.getReviImgCode());
        }

        // 리뷰 삭제 --> reviCode로 삭제!
        review.delete(code);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/review")
    public ResponseEntity update(ReviewDTO dto) throws IOException {
        log.info("dto : " + dto);
        // 리뷰 코드를 가지고 와서 그 전에 있는 파일이 있고 없고의 따라서 이미지 관련된 것! 처리
        // dto.images에 있으면 삭제하지 않은 사진들..
        //              없으면 기존 사진 삭제한 것으로 판단..
        // 1. 기존 리뷰에 있던 이미지들 정보 가져오기
        List<ReviewImage> images = review.viewImages(dto.getReviCode());
        // 2. 반복문을 돌려서 dto.images에 해당 이미지가 포함되어 있는지 판단
        for(ReviewImage image : images) {
            //      dto.getImages().contains(image.getReviUrl()) <-- 이거 사용!
            if((dto.getImages()!=null && !dto.getImages().contains(image.getReviUrl())) || dto.getImages() == null) {
                // 3. 위에 해당하는 코드로 조건을 걸어서 폴더에 있는 실제 파일 삭제!
                File file = new File(image.getReviUrl());
                file.delete();

                // 4. 파일 삭제와 동시에 테이블에서도 해당 정보 삭제!
                review.deleteImage(image.getReviImgCode());
            }
        }

        // dto.files에 새로 추가된 사진들 추가만 하면 됩니다!
        if(dto.getFiles() != null) {
            // review_image에는 revi_code가 필요!
            for(MultipartFile file : dto.getFiles()) {
                ReviewImage imgVo = new ReviewImage();

                String fileName = file.getOriginalFilename();
                String uuid = UUID.randomUUID().toString();
                String saveName = uploadPath + File.separator + "review" + File.separator + uuid + "_" + fileName;
                Path savePath = Paths.get(saveName);
                file.transferTo(savePath);

                imgVo.setReviUrl(saveName);
                imgVo.setReview(Review.builder().reviCode(dto.getReviCode()).build());

                review.createImg(imgVo);
            }
        }
        // 리뷰 수정!
        Review vo = Review.builder()
                .reviCode(dto.getReviCode())
                .id(dto.getId())
                .prodCode(dto.getProdCode())
                .reviTitle(dto.getReviTitle())
                .reviDesc(dto.getReviDesc())
                .build();
        review.create(vo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/review/comment")
    public ResponseEntity create(@RequestBody ReviewComment vo) {

        Object principal = authentication();

        if(principal instanceof User) {
            User user = (User) principal;
            vo.setUser(user);
            return ResponseEntity.ok(comment.create(vo));
        }

        return ResponseEntity.badRequest().build();
    }

    public Object authentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        return authentication.getPrincipal();
    }

    @GetMapping("/public/review/{code}/comment")
    public ResponseEntity<List<ReviewCommentDTO>> viewComments(@PathVariable(name="code") int code) {
        List<ReviewComment> list = comment.getTopLevelComments(code);
        List<ReviewCommentDTO> response = new ArrayList<>();

        for(ReviewComment item : list) {

            List<ReviewComment> comments = comment.getRepliesComments(item.getReviComCode(), code);
            List<ReviewCommentDTO> replies = new ArrayList<>();

            for(ReviewComment comment : comments) {
                ReviewCommentDTO dto = ReviewCommentDTO.builder()
                        .reviCode(comment.getReviCode())
                        .reviComCode(comment.getReviComCode())
                        .reviComDesc(comment.getReviComDesc())
                        .reviComDate(comment.getReviComDate())
                        .user(UserDTO.builder()
                                .id(comment.getUser().getId())
                                .name(comment.getUser().getName())
                                .build())
                        .build();
                replies.add(dto);
            }

            ReviewCommentDTO dto = ReviewCommentDTO.builder()
                    .reviCode(item.getReviCode())
                    .reviComCode(item.getReviComCode())
                    .reviComDesc(item.getReviComDesc())
                    .reviComDate(item.getReviComDate())
                    .replies(replies)
                    .user(UserDTO.builder()
                            .id(item.getUser().getId())
                            .name(item.getUser().getName())
                            .build())
                    .build();
            response.add(dto);
        }

        return ResponseEntity.ok(response);
    }


}