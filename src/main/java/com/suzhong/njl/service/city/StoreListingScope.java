package com.suzhong.njl.service.city;

import java.util.List;

import com.suzhong.njl.domain.enums.DestinationStatus;

public record StoreListingScope(List<Long> regionIds, DestinationStatus destinationStatus, String emptyHint) {

    public static StoreListingScope closed() {
        return new StoreListingScope(List.of(), DestinationStatus.CLOSED, CityRules.EMPTY_HINT);
    }
}
