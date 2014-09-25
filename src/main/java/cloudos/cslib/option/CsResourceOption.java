package cloudos.cslib.option;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @Accessors(chain=true)
public class CsResourceOption {

    @Getter @Setter private String name;
    @Getter @Setter private CsResourceOptionType type;
    @Getter @Setter private boolean required = true;
    @Getter @Setter private String value;
    @Getter @Setter private String defaultValue;
    @Getter @Setter private List<String> choices;

    public CsResourceOption(CsResourceOption option) {
        this.name = option.name;
        this.type = option.type;
        this.required = option.required;
        this.value = option.value;
        this.defaultValue = option.defaultValue;
        this.choices = option.choices == null ? null : new ArrayList<>(option.choices);
    }

    public boolean hasDefaultValue () { return !StringUtil.empty(defaultValue); }

    public CsResourceOption freeze () {
        return new CsImmutableResourceOption(this);
    }

//    public static Map<String, String> toStringMap (List<CsResourceOption> options) {
//        Map<String, String> map = new LinkedHashMap<>();
//        for (CsResourceOption option : options) {
//            map.put(option.getName(), option.getValue());
//        }
//        return map;
//    }
//
//    public static List<CsResourceOption> toList(Map<String, CsResourceOption> options) {
//        List<CsResourceOption> list = new ArrayList<>(options.size());
//        for (String key : options.keySet()) {
//            list.add(options.get(key));
//        }
//        return list;
//    }
}
