package cloudos.server;

import cloudos.service.TwoFactorAuthService;

public interface HasTwoFactorAuthConfiguration {

    public TwoFactorAuthService getTwoFactorAuthService ();

}
