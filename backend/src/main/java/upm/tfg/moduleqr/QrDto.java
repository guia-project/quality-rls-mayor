package upm.tfg.moduleqr;

import lombok.Data;

@Data
public class QrDto {
    private String content;
    private RuleType type;
    private String name;
    private String description;
}
