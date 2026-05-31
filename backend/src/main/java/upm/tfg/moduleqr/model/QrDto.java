package upm.tfg.moduleqr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrDto {
    private String content;
    private RuleType type;
    private String name;
    private String description;
    private boolean enable;
    private String datasetId;
}
