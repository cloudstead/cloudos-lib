package cloudos.cslib.option;

import java.util.List;

public class CsImmutableResourceOption extends CsResourceOption {

    public CsImmutableResourceOption(CsResourceOption option) {
        super(option);
    }

    @Override public CsResourceOption setName (String name) { throw new IllegalStateException("frozen"); }
    @Override public CsResourceOption setType (CsResourceOptionType type) { throw new IllegalStateException("frozen"); }
    @Override public CsResourceOption setRequired (boolean required) { throw new IllegalStateException("frozen"); }
    @Override public CsResourceOption setDefaultValue (String defaultValue) { throw new IllegalStateException("frozen"); }
    @Override public CsResourceOption setChoices (List<String> choices) { throw new IllegalStateException("frozen"); }
    @Override public CsResourceOption setValue (String value) { throw new IllegalStateException("frozen"); }

}
