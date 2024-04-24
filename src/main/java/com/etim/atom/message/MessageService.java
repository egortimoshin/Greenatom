package com.etim.atom.message;

import com.etim.atom.topic.Topic;
import com.etim.atom.topic.TopicRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@AllArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    private final TopicRepository topicRepository;

    public Message save(Message message, String topicId) {
        validateMessage(message);
        message.setCreatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        message.setTopic(topicRepository.findByTopicUuid(topicId).orElse(null));
        message.setAuthor(getCurrentUsername());
        return messageRepository.save(message);
    }

    public Topic update(String messageIdToUpdate, Message updatedMessage) {
        Message messageToUpdate = findByUuid(messageIdToUpdate);
        validateMessage(updatedMessage);

        if (getCurrentUsername().equals(messageToUpdate.getAuthor()) || ifAdmin()) {
            messageToUpdate.setText(updatedMessage.getText());
            messageRepository.save(messageToUpdate);
        }

        return topicRepository.findByTopicUuid(messageToUpdate.getTopic().getTopicUuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    public void delete(String id) {
        Message message = findByUuid(id);
        if (getCurrentUsername().equals(message.getAuthor()) || ifAdmin()) {
            messageRepository.delete(message);
        }
    }

    private String getCurrentUsername() {
        UserDetails personDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return personDetails.getUsername();
    }

    private Boolean ifAdmin() {
        UserDetails personDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return personDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ADMIN"));
    }

    private void validateMessage(Message message) {
        if (message.getText().isEmpty() || message.getText().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Text of message is invalid");
        }
    }

    private Message findByUuid(String id) {
        return messageRepository.findByMessageUuid(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found with ID: " + id));
    }
}