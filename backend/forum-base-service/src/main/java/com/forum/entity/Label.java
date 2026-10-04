package com.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "labels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String colorCode; // e.g. "#3498db"

    @Column(nullable = false)
    private String textColor; // e.g. "#ffffff"

    @Column(nullable = true)
    private String borderColor; // e.g. "transparent" or "#000000"

    @Column(nullable = false)
    private Boolean adminOnly = false;
}
