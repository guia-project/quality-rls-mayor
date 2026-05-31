package upm.tfg.modulekg.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import upm.tfg.moduleqr.model.QualityRule;

import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Builder
public class Dataset {

    @Id
    private String id;

    private String name;

    private String endpointUrl;

    @OneToMany(
            mappedBy = "dataset",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonManagedReference
    private List<QualityRule> rules;

}