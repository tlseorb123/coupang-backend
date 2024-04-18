package com.kh.coupang.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@Builder
public class Review {

    @Id
    @Column(name="revi_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviCode;

    @Column
    private String id;

    @Column(name="prod_code")
    private int prodCode;

    @Column(name="revi_title")
    private String reviTitle;

    @Column(name="revi_desc")
    private String reviDesc;

    @Column(name="revi_date")
    private Date reviDate;

    @Column
    private int rating;

    @OneToMany(mappedBy = "review")
    private List<ReviewImage> images;

}