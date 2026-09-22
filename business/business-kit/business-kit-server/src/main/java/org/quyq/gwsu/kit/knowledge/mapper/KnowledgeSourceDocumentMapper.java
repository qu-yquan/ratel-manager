package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 知识源文档 Mapper。
 */
@Mapper
public interface KnowledgeSourceDocumentMapper extends BaseMapper<KitKnowledgeSourceDocument> {
    KitKnowledgeSourceDocument selectByIdForUpdate(@Param("id") String id);

    List<Map<String, Object>> countDocumentsByParent();

    IPage<KitKnowledgeSourceDocument> searchAccessibleDocuments(Page<KitKnowledgeSourceDocument> page,
                                                                  @Param("name") String name,
                                                                  @Param("allDirectories") boolean allDirectories,
                                                                  @Param("roleCodes") Collection<String> roleCodes);
}
