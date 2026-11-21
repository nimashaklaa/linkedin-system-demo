package com.linkedin.searchservice.service;

import com.linkedin.searchservice.Repository.UserSearchRepository;
import com.linkedin.searchservice.model.UserDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor

public class SearchEvenConsumer {
    private final UserSearchRepository userSearchRepository;

    @KafkaListener(topics = "user.created")
    // Map<String, Object> tells Jackson (or your configured deserializer) to parse the incoming JSON into a key-value
    public void consumeUserCreated(
            @Payload Map<String ,Object> payload
    ){
        try{
            log.info("Received user created event");
            UserDocument document = UserDocument.builder()
                    .id((String) payload.get("userId"))
                    .firstName((String) payload.get("firstName"))
                    .lastName((String) payload.get("lastName"))
                    .email((String) payload.get("email"))
                    .headline((String) payload.get("headline"))
                    .location((String) payload.get("location"))
                    .build();
            userSearchRepository.save(document);
            log.info("User indexed successfully");
        }catch (Exception e){
            log.error("Error indexing user: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "user.updated")
    public void consumeUserUpdated(
            @Payload Map<String ,Object> payload
    ){
        log.info("Received user updated event");
        try{
            String userId = (String) payload.get("userId");
            userSearchRepository.findById(userId).ifPresent(
                    doc->{
                        doc.setFirstName((String) payload.get("firstName"));
                        doc.setLastName((String) payload.get("lastName"));
                        doc.setHeadline((String) payload.get("headline"));
                        doc.setLocation((String) payload.get("location"));

                        if(payload.get("skills") instanceof List<?> skills){
                            doc.setSkills(skills.stream()
                                    .map(String.class::cast)
                                    .toList());
                        }

                        userSearchRepository.save(doc);
                        log.info("User updated successfully");
                    }
            );

        }catch (Exception e){
            log.error("Error updating user: {}", e.getMessage());
        }
    }
}
