package com.senya.eatappserver.callback;

import com.senya.eatappserver.model.ShipperModel;

import java.util.List;

public interface IShipperLoadCallbackListener {
    void onShipperLoadSuccess(List<ShipperModel> shipperModelList);
    void onShipperLoadFailed(String message);
}
