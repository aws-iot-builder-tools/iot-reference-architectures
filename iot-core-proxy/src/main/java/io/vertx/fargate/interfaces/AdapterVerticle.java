package io.vertx.fargate.interfaces;

import io.vertx.core.Verticle;

// NOTE: The purpose of this interface is so that we can bind Verticles with Dagger since AbstractVerticle is not an interface
public interface AdapterVerticle extends Verticle {
}
