package cloudos.cslib.option;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class CsImmutableResourceOption extends CsResourceOption {

    public CsImmutableResourceOption(CsResourceOption option) {
        super(option);
    }

    @Override public CsResourceOption setName (String name) { return die("frozen"); }
    @Override public CsResourceOption setType (CsResourceOptionType type) { return die("frozen"); }
    @Override public CsResourceOption setRequired (boolean required) { return die("frozen"); }
    @Override public CsResourceOption setDefaultValue (String defaultValue) { return die("frozen"); }
    @Override public CsResourceOption setChoices (List<String> choices) { return die("frozen"); }
    @Override public CsResourceOption setValue (String value) { return die("frozen"); }

}
