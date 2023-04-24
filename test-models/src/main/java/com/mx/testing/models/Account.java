package com.mx.testing.model;

import lombok.Data;

import com.mx.common.models.ModelBase;

@Data
public class Account extends ModelBase<Account> {
  private String id;
  private String description;
  private String type;
  private Double balance;
}
