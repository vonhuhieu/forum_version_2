package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_titles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TitleType type;

    @Column(name = "min_points")
    private Integer minPoints;

    @Column
    private String description;

    @Column(name = "is_trusted")
    private Boolean isTrusted = false;
}
