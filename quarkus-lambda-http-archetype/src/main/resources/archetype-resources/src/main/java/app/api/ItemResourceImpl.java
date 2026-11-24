package com.example.app.api;

import com.example.api.ItemResource;
import com.example.api.model.ItemCreateRequest;
import com.example.app.business.entity.ItemEntity;
import com.example.app.business.service.CreateItemService;
import com.example.app.business.service.GetItemService;
import com.example.app.business.service.ListItemsService;
import com.xdyan.commons.xray.XRayTraced;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@XRayTraced
@ApplicationScoped
public class ItemResourceImpl implements ItemResource {

  @Inject ListItemsService listItemsService;

  @Inject GetItemService getItemService;

  @Inject CreateItemService createItemService;

  @Inject ItemResourceMapper mapper;

  @Override
  public Response listItems() {
    return Response.ok(mapper.toResponseList(listItemsService.listItems())).build();
  }

  @Override
  public Response getItem(Long id) {
    return Response.ok(mapper.toResponse(getItemService.getItem(id))).build();
  }

  @Override
  public Response createItem(ItemCreateRequest itemCreateRequest) {
    ItemEntity item = createItemService.createItem(mapper.toDomain(itemCreateRequest));
    return Response.status(Response.Status.CREATED).entity(mapper.toResponse(item)).build();
  }
}
