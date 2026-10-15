package com.suzhong.njl.web.portal.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateIntentRequest(
        @NotNull(message = "请选择房源")
        Long storeId,
        @NotNull(message = "请填写到店日期")
        LocalDate checkIn,
        LocalDate checkOut,
        @Min(value = 1, message = "晚数不正确")
        Integer nights,
        @NotNull(message = "请填写人数")
        @Min(value = 1, message = "人数不正确")
        Integer guests,
        @NotBlank(message = "请填写联系人")
        @Size(max = 64, message = "联系人过长")
        String contactName,
        @NotBlank(message = "请填写联系电话")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
        String contactPhone,
        @Size(max = 500, message = "备注过长")
        String userRemark) {
}
