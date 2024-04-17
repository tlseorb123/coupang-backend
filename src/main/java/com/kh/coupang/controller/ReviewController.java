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