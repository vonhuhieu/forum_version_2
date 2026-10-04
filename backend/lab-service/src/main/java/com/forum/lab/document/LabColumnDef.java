package com.forum.lab.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabColumnDef {
    private String code;
    private String name;
    private String type;     // text, number, image, boolean
    private String align;    // left, center, right
    private boolean required;
}
