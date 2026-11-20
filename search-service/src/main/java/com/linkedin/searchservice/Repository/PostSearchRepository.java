package com.linkedin.searchservice.Repository;

import com.linkedin.searchservice.model.PostDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * fuzziness measures how many single-character edits (insertions, deletions, substitutions, or transpositions of adjacent characters) are allowed for a search term to still match a term in the index—essentially typo / spelling tolerance.
 *
 *
 * "?0" is a positional parameter placeholder
 */
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    @Query("""
            {
              "match": {
                "content": {
                  "query": "?0",
                  "fuzziness": "AUTO"
                }
              }
            }
            """)
    List<PostDocument> searchPosts(String query);
}
