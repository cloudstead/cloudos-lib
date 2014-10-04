package cloudos.dao;

import cloudos.model.AccountBase;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;

public abstract class AccountBaseDAO<T extends AccountBase> extends UniquelyNamedEntityDAO<T>  {

    public abstract T authenticate(LoginRequest loginRequest) throws AuthenticationException;

}
