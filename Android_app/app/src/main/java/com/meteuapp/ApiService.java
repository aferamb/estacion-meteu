package com.meteuapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("UbicompServerExample/GetData")
    Call<List<Street>> getItems();
}
