package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogSnapshot;

import java.util.concurrent.atomic.AtomicReference;

public final class SkillCatalogHolder {

    private final AtomicReference<SkillCatalogSnapshot> snapshot =
            new AtomicReference<>(SkillCatalogSnapshot.empty());

    public SkillCatalogSnapshot current() {
        return snapshot.get();
    }

    public SkillCatalogSnapshot replace(SkillCatalogSnapshot replacement) {
        return snapshot.getAndSet(replacement);
    }
}
