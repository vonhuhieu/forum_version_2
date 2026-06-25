package com.forum.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

@Document(indexName = "forum_search")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDocument {
    @Id
    private String id;           // Format: "thread_{id}" or "post_{id}"

    @Field(type = FieldType.Long)
    private Long originalId;     // Database ID of Thread or Post

    @Field(type = FieldType.Keyword)
    private String type;         // "thread" or "post"

    @Field(type = FieldType.Long)
    private Long threadId;       // Parent thread ID

    @Field(type = FieldType.Text, analyzer = "standard")
    private String threadTitle;  // Thread title

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;      // Thread content or Post content

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String authorName;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String scope;        // "PUBLIC" or "INTERNAL"

    @Field(type = FieldType.Boolean)
    private boolean active;
}
