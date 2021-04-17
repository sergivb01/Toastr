package dev.sergivos.toastr.resolver.impl;

import java.util.concurrent.Callable;

public interface IResolver<T> extends Callable<T> {

    String getSource();

}
