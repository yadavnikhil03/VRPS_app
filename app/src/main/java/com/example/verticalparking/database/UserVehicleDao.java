package com.example.verticalparking.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserVehicleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserVehicle userVehicle);

    @Update
    void update(UserVehicle userVehicle);

    @Query("SELECT * FROM user_vehicle WHERE userId = :userId LIMIT 1")
    UserVehicle getByUserId(String userId);

    @Query("SELECT * FROM user_vehicle WHERE licensePlate = :plate AND isParked = 1 LIMIT 1")
    UserVehicle getParkedByPlate(String plate);

    @Query("SELECT * FROM user_vehicle WHERE isParked = 1")
    List<UserVehicle> getAllParked();

    @Query("SELECT COUNT(*) FROM user_vehicle WHERE isParked = 1")
    int getParkedCount();

    @Query("DELETE FROM user_vehicle WHERE userId = :userId")
    void deleteByUserId(String userId);

    @Query("DELETE FROM user_vehicle")
    void deleteAll();
}
