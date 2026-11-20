package com.linkedin.searchservice.Repository;

import com.linkedin.searchservice.model.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
}
