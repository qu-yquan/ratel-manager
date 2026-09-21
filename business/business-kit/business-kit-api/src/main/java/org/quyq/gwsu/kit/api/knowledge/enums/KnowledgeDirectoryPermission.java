package org.quyq.gwsu.kit.api.knowledge.enums;

/** 知识目录权限按检索、上传、管理逐级包含。 */
public enum KnowledgeDirectoryPermission {
    SEARCH,
    UPLOAD,
    MANAGE;

    public boolean allows(KnowledgeDirectoryPermission required) {
        return ordinal() >= required.ordinal();
    }
}
