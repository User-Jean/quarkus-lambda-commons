package com.example.app.api;

import com.example.api.model.Item;
import com.example.api.model.ItemCreateRequest;
import com.example.app.business.entity.ItemEntity;
import com.xdyan.commons.http.mapper.DateTimeMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Only the api package knows the generated DTOs: the services receive and return entities. */
@Mapper(componentModel = "cdi", uses = DateTimeMapper.class)
public interface ItemResourceMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  ItemEntity toDomain(ItemCreateRequest request);

  Item toResponse(ItemEntity entity);

  List<Item> toResponseList(List<ItemEntity> entities);
}
