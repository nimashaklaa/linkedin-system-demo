package com.linkedin.searchservice.service;

import com.linkedin.searchservice.Repository.PostSearchRepository;
import com.linkedin.searchservice.Repository.UserSearchRepository;
import com.linkedin.searchservice.model.PostDocument;
import com.linkedin.searchservice.model.UserDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final UserSearchRepository userSearchRepository;
    private final PostSearchRepository postSearchRepository;

    public List<UserDocument> searchUsers(String query) {
        log.info("Searching users for query: {}", query);
        return userSearchRepository.searchUsers(query);

    }

    public List<UserDocument> searchBySKill(String skill) {
    }

    public List<PostDocument> searchParams(String query) {
    }
}
