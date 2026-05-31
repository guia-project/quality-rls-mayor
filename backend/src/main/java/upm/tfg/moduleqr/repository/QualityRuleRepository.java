package upm.tfg.moduleqr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upm.tfg.moduleqr.model.QualityRule;

public interface QualityRuleRepository extends JpaRepository<QualityRule, String> {
}
