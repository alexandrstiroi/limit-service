package org.shtiroy.module1.hm08.repository;

import org.shtiroy.module1.hm08.entity.LimitSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LimitSettingsRepository extends JpaRepository<LimitSettings, Long> {
}
