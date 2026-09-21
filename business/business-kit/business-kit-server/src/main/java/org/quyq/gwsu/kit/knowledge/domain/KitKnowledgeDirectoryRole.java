package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_directory_role")
public class KitKnowledgeDirectoryRole extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String directoryId;
    private String roleCode;
}
