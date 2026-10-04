package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private long totalCategories;
    private long totalThreads;
    private long totalPosts;
    private long totalMembers;
    private String latestMember;
    private long officialMembers;
    private long unofficialMembers;
    private long totalOfficialAndUnofficial;
}
