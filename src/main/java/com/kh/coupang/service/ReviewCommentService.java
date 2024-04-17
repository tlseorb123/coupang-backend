package com.kh.coupang.service;

import com.kh.coupang.domain.QReviewComment;
import com.kh.coupang.domain.ReviewComment;
import com.kh.coupang.repo.ReviewCommentDAO;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewCommentService {

    @Autowired
    private ReviewCommentDAO dao;

    @Autowired
    private JPAQueryFactory queryFactory;

    private final QReviewComment qReviewComment = QReviewComment.reviewComment;

    public ReviewComment create(ReviewComment vo) {
        return dao.save(vo);
    }

    public List<ReviewComment> getTopLevelComments(int code) {
        return queryFactory.selectFrom(qReviewComment)
                .where(qReviewComment.reviComParent.eq(0))
                .where(qReviewComment.reviCode.eq(code))
                .orderBy(qReviewComment.reviComDate.asc())
                .fetch();
    }

    public List<ReviewComment> getRepliesComments(int parent, int code) {
        return queryFactory.selectFrom(qReviewComment)
                .where(qReviewComment.reviComParent.eq(parent))
                .where(qReviewComment.reviCode.eq(code))
                .orderBy(qReviewComment.reviComDate.asc())
                .fetch();
    }
}