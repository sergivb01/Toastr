package services.vortex.toastr.resolver.impl;

import services.vortex.toastr.resolver.Resolver;

public interface IResolver {

    String getSource();

    Resolver.Result check(String username) throws Exception;

}
