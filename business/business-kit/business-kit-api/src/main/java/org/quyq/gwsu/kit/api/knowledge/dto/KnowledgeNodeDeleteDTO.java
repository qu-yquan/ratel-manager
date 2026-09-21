package org.quyq.gwsu.kit.api.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeNodeDeleteDTO {
    private List<String> ids;
}
