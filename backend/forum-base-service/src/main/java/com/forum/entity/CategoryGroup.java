package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "position_order")
    private Integer positionOrder;

    private boolean active = true;

    @OneToMany(mappedBy = "categoryGroup")
    @OrderBy("positionOrder ASC")
    private List<Category> categories = new ArrayList<>();
}
