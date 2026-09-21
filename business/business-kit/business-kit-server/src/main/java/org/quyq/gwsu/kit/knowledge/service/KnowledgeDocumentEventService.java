package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentEventVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeDocumentEvent;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeDocumentEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentEventService {
    private final KnowledgeDocumentEventMapper mapper;
    private final KnowledgeDirectoryService directoryService;

    public void append(String documentId, String taskId, String eventType, String message) {
        mapper.insert(new KitKnowledgeDocumentEvent().setDocumentId(documentId).setTaskId(taskId)
                .setEventType(eventType).setMessage(message).setOccurredAt(LocalDateTime.now()));
    }

    public void deleteByDocumentId(String documentId) {
        mapper.delete(new LambdaQueryWrapper<KitKnowledgeDocumentEvent>()
                .eq(KitKnowledgeDocumentEvent::getDocumentId, documentId)
                .eq(KitKnowledgeDocumentEvent::getDeleted, false));
    }

    public List<KnowledgeDocumentEventVO> list(String documentId) {
        directoryService.requireReadableDocument(documentId);
        return mapper.selectList(new LambdaQueryWrapper<KitKnowledgeDocumentEvent>()
                .eq(KitKnowledgeDocumentEvent::getDocumentId, documentId)
                .eq(KitKnowledgeDocumentEvent::getDeleted, false)
                .orderByDesc(KitKnowledgeDocumentEvent::getOccurredAt)).stream().map(event -> {
            KnowledgeDocumentEventVO vo = new KnowledgeDocumentEventVO().setId(event.getId())
                    .setTaskId(event.getTaskId()).setEventType(event.getEventType())
                    .setMessage(event.getMessage()).setOccurredAt(event.getOccurredAt());
            vo.copyBaseProperties(event);
            return vo;
        }).toList();
    }
}
