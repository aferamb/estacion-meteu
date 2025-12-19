package com.meteuapp;

import com.meteuapp.models.Alarm;
import com.meteuapp.models.LiveResponse;
import com.meteuapp.models.SensorReading;
import com.meteuapp.models.TokenResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface mapping the server endpoints used by the web application.
 * Uses cookie-based sessions (login via /login) maintained by the OkHttp CookieJar.
 */
public interface ServerApi {

    @FormUrlEncoded
    @POST("login")
    Call<Void> login(@Field("username") String username, @Field("password") String password);

    @FormUrlEncoded
    @POST("api/login")
    Call<TokenResponse> loginJwt(@Field("username") String username, @Field("password") String password);

    @GET("admin/live")
    Call<LiveResponse> getLive();

    @GET("sensor/readings")
    Call<List<SensorReading>> getSensorReadings(@Query("sensor_id") String sensorId,
                                               @Query("street_id") String streetId,
                                               @Query("start") String start,
                                               @Query("end") String end,
                                               @Query("limit") Integer limit);

    @GET("api/readings/query")
    Call<List<Map<String, Object>>> queryReadings(@Query("start") String start,
                                                   @Query("end") String end,
                                                   @Query("filter") String filter,
                                                   @Query("value") String value,
                                                   @Query("op") String op,
                                                   @Query("sortBy") String sortBy,
                                                   @Query("order") String order,
                                                   @Query("limit") Integer limit,
                                                   @Query("offset") Integer offset);

    @GET("admin/subscriptions")
    Call<List<String>> getSubscriptions();

    @FormUrlEncoded
    @POST("admin/subscribe")
    Call<Map<String, Object>> postSubscribe(@Field("topic") String topic);

    @FormUrlEncoded
    @POST("admin/unsubscribe")
    Call<Map<String, Object>> postUnsubscribe(@Field("topic") String topic);

    @FormUrlEncoded
    @POST("admin/publishAlert")
    Call<Map<String, Object>> publishAlert(@Field("topic") String topic,
                                           @Field("subscription") String subscription,
                                           @Field("alert") String alert,
                                           @Field("message") String message);

    @GET("admin/alarms")
    Call<List<Alarm>> getAlarms(@Query("sensor_id") String sensorId,
                                @Query("parameter") String parameter,
                                @Query("active") String active,
                                @Query("limit") Integer limit);

    @GET("admin/users")
    Call<List<Map<String, Object>>> getUsers();

    @FormUrlEncoded
    @POST("admin/users/create")
    Call<Void> createUser(@Field("username") String username,
                          @Field("password") String password,
                          @Field("role") String role);

    @GET("logout")
    Call<Void> logout();

    @GET("admin/ranges")
    Call<java.util.List<java.util.Map<String, Object>>> getRanges();

    @FormUrlEncoded
    @POST("admin/ranges")
    Call<java.util.Map<String, Object>> postRange(@Field("parameter") String parameter,
                                                 @Field("min") String min,
                                                 @Field("max") String max);

    @GET("Health")
    Call<java.util.Map<String, Object>> getHealth();
}
