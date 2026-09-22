package org.quyq.gwsu.common.log.enums;

/**
 * @author Quyq
 * @date 2025/1/13
 * @description 存储媒介
 */
public enum SaveMedium {
    /**
     * Elasticsearch。
     */
    ES("Elasticsearch"),
    /**
     * 数据库。
     */
    DATABASE("数据库");

    private final String name;

    SaveMedium(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
