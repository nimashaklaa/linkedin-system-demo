package com.linkedin.searchservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * Documents for user search
 * Indexed when yser registers or updates profile
 * Enables for text search across name, headline, skills, location
 * .
 * <----FeildType.Keyword is used for fields that should be searched exactly.
 * FeildType.Text is used for text fields.------>
 * Elasticsearch runs the string through an analyzer before indexing: it lowercases the text, strips punctuation, and splits it into individual tokens/words (for example, "Senior Software Engineer" is indexed as ["senior", "software", "engineer"]).
 * Not suitable for exact matching, sorting, or aggregations
 *
 *
 * Elasticsearch stores the entire string as a single, exact, case-sensitive token without splitting or lowercasing it (for example, "Senior Software Engineer" stays "Senior Software Engineer").
 */
@Document(indexName = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    @Id
    private String id;
    @Field(type = FieldType.Text)

    private String firstName;
    @Field(type = FieldType.Text)

    private String lastName;
    @Field(type = FieldType.Text)

    private String headline;
    @Field(type = FieldType.Keyword)

    private String location;
    @Field(type = FieldType.Keyword)

    private List<String> skills;
    @Field(type = FieldType.Keyword)

    private String email;

    private String profilePhotoUrl;

}
