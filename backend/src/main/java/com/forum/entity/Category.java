package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "position_order")
    private Integer positionOrder;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_group_id")
    private CategoryGroup categoryGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    @OrderBy("positionOrder ASC")
    @org.hibernate.annotations.BatchSize(size = 100)
    private java.util.List<Category> subCategories = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.REMOVE)
    @org.hibernate.annotations.BatchSize(size = 100)
    private java.util.List<Thread> threads = new java.util.ArrayList<>();
}
