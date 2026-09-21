package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;


/**
 * 知识源文档服务。
 */
public interface IKnowledgeSourceDocumentService extends IService<KitKnowledgeSourceDocument> {

    String saveDocument(KnowledgeDocumentSaveDTO dto);

    KnowledgeDocumentVO getDocument(String documentId);

    void updateEnabled(String documentId, boolean enabled);

    void purgeDocumentDerivedData(String documentId);

    void deleteDocumentData(String documentId);

}
