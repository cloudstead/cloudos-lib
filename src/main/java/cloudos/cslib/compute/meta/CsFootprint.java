package cloudos.cslib.compute.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.reflect.ReflectionUtil;

@NoArgsConstructor @Accessors(chain=true)
public class CsFootprint implements Footprint {

    public CsFootprint(CsFootprint other) { ReflectionUtil.copy(this, other); }

    @Getter @Setter private Integer cpus;
    @Getter @Setter private Integer memory;
    @Getter @Setter private CsUsageLevel networkIoLevel;
    @Getter @Setter private CsUsageLevel diskIoLevel;
}
