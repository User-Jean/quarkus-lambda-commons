package com.example.app.api;

import static com.example.api.model.HelloResponse.DatabaseEnum.DOWN;
import static com.example.api.model.HelloResponse.DatabaseEnum.UP;

import com.example.api.HealthResource;
import com.example.api.model.HelloResponse;
import com.example.app.business.service.HealthService;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@XRayTraced
@ApplicationScoped
public class HealthResourceImpl implements HealthResource {

  @Inject HealthService service;

  @Override
  public Response hello() {
    var databaseUp = service.isDatabaseUp();
    var response = new HelloResponse().message("Hello").database(databaseUp ? UP : DOWN);
    var status = databaseUp ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;
    return Response.status(status).entity(response).build();
  }
}
