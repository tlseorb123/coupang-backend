package com.kh.coupang.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert  // 추가 때문에 넣었다
public class Product {

    @Id
    @Column(name="prod_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int prodCode; //prod_code

    @Column(name = "prod_name")
    private String prodName;  // prod_name

    @Column
    private int price;

    @ManyToOne
    @JoinColumn(name="cate_code")
    private Category category;

}
