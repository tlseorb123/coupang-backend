package com.kh.coupang.service;

import com.kh.coupang.domain.Review;
import com.kh.coupang.domain.ReviewImage;
import com.kh.coupang.repo.ReviewDAO;
import com.kh.coupang.repo.ReviewImageDAO;
import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewDAO review;

    @Autowired
    private ReviewImageDAO image;

    public Review create(Review vo) {
        return review.save(vo);
    }

    public ReviewImage createImg(ReviewImage vo) {
        return image.save(vo);
    }

    public Page<Review> viewAll(Pageable pageable, BooleanBuilder builder) {
        return review.findAll(builder, pageable);
    }

    // 이미지들 조회
    public List<ReviewImage> viewImages(int code) {
        return image.findByReviCode(code);
    }

    // 이미지 삭제
    public void deleteImage(int code) {
        image.deleteById(code);
    }

    // 리뷰 삭제
    public void delete(int code) {
        review.deleteById(code);
    }
}