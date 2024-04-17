package com.kh.coupang.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class ReviewCommentDTO {
    private int reviComCode;
    private String reviComDesc;
    private Date reviComDate;
    private int reviCode;
    private UserDTO user;
    private List<ReviewCommentDTO> replies = new ArrayList<>();
}