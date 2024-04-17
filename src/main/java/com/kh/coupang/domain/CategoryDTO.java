package com.kh.coupang.domain;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoryDTO {

    private int cateCode;
    private String cateIcon;
    private String cateName;
    private String cateUrl;
    private List<Category> subCategories = new ArrayList<>();

}