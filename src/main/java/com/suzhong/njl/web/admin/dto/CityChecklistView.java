package com.suzhong.njl.web.admin.dto;

public record CityChecklistView(
        boolean destinationReady,
        boolean supplyReady,
        boolean articlesReady,
        boolean supportCovered,
        boolean dirtyDataCleared) {
}
