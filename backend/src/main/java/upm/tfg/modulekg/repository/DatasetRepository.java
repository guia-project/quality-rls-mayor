package upm.tfg.modulekg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upm.tfg.modulekg.model.Dataset;


public interface DatasetRepository extends JpaRepository<Dataset, String> {
}
