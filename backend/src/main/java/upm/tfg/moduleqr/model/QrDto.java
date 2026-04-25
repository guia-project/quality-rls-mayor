package upm.tfg.moduleqr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class QrDto {
    private String content;
    private RuleType type;
    private String name;
    private String description;
}
