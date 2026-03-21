package com.example.verticalparking.api;

import com.example.verticalparking.models.GenericResponse;
import com.example.verticalparking.models.ParkRequest;
import com.example.verticalparking.models.ParkResponse;
import com.example.verticalparking.models.RetrieveRequest;
import com.example.verticalparking.models.StatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit API interface for ESP32 communication.
 * Base URL is configurable through Settings.
 */
public interface ApiService {

    @POST("/park")
    Call<ParkResponse> parkVehicle(@Body ParkRequest request);

    @POST("/retrieve")
    Call<GenericResponse> retrieveVehicle(@Body RetrieveRequest request);

    @GET("/status")
    Call<StatusResponse> getStatus();
}
