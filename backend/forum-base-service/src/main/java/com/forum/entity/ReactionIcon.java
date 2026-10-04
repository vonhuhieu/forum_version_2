package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reaction_icons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionIcon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String icon; // Stores identifier/code (e.g., "like", "love", "haha")

    @Column(nullable = false)
    private String tooltip; // Stores display label (e.g., "Thích", "Haha")

    @Column(nullable = false)
    private String color; // Stores active text HEX color (e.g., "#1877F2")

    @Column(nullable = false)
    private int orderIndex = 0; // Stores sorting hierarchy
}
