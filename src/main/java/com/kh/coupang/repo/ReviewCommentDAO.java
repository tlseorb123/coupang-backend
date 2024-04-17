package com.kh.coupang.repo;

import com.kh.coupang.domain.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentDAO extends JpaRepository<ReviewComment, Integer> {
}