package com.nutricook.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationDTO {
  private String name;
  private String type;
  private Double latitude;
  private Double longitude;
  private String address;
  private Long osmNodeId;
}
