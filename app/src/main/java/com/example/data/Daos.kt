package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // ─── Plans & Exercises ───────────────────────────────────────────────────

    @Query("SELECT * FROM workout_plans ORDER BY orderIndex ASC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM exercises WHERE planId = :planId ORDER BY sequenceNumber ASC")
    fun getExercisesForPlan(planId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY planId ASC, sequenceNumber ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<WorkoutPlan>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Query("UPDATE exercises SET sequenceNumber = :seq WHERE id = :id")
    suspend fun updateExerciseSequence(id: String, seq: Int)

    // ─── Set Logs ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: SetLog)

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: Int)

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getLogsForExercise(exerciseId: String): Flow<List<SetLog>>

    @Query("""
        SELECT * FROM set_logs
        WHERE exerciseId = :exerciseId
          AND sessionId = (
              SELECT sessionId FROM set_logs WHERE exerciseId = :exerciseId
              ORDER BY timestamp DESC LIMIT 1
          )
        ORDER BY setNumber ASC
    """)
    fun getLastSessionLogsForExercise(exerciseId: String): Flow<List<SetLog>>

    /** Returns up to 30 distinct sessions for charting — was 8, now 30. */
    @Query("""
        SELECT * FROM set_logs
        WHERE exerciseId = :exerciseId
          AND sessionId IN (
              SELECT DISTINCT sessionId FROM set_logs
              WHERE exerciseId = :exerciseId
              ORDER BY sessionId DESC LIMIT 30
          )
        ORDER BY sessionId DESC, setNumber ASC
    """)
    fun getRecentSessionLogsForExercise(exerciseId: String): Flow<List<SetLog>>

    // ─── Workout Sessions ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutSession(sessionId: Long)

    @Query("SELECT * FROM workout_sessions WHERE completedAt >= :since ORDER BY completedAt DESC")
    fun getSessionsSince(since: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY completedAt DESC LIMIT 50")
    fun getRecentSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE completedAt >= :since")
    fun getSessionCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getTotalSessionCount(): Flow<Int>

    // ─── Nutrition Logs ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionLog(log: NutritionLog): Long

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteNutritionLog(id: Int)

    @Query("""
        UPDATE nutrition_logs
        SET grams = :grams, calories = :calories, protein = :protein, carbs = :carbs, fat = :fat
        WHERE id = :id
    """)
    suspend fun updateNutritionLog(id: Int, grams: Float, calories: Int, protein: Float, carbs: Float, fat: Float)

    @Query("SELECT * FROM nutrition_logs WHERE dayKey = :dayKey ORDER BY loggedAt ASC")
    fun getNutritionLogsForDay(dayKey: Long): Flow<List<NutritionLog>>

    // ─── Body Weight ──────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeightLog(log: BodyWeightLog)

    @Query("DELETE FROM body_weight_logs WHERE id = :id")
    suspend fun deleteBodyWeightLog(id: Int)

    @Query("SELECT * FROM body_weight_logs ORDER BY loggedAt DESC LIMIT 50")
    fun getRecentBodyWeightLogs(): Flow<List<BodyWeightLog>>

    // ─── Personal Records ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonalRecord(pr: PersonalRecord)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deletePersonalRecord(exerciseId: String)

    @Query("SELECT * FROM personal_records ORDER BY achievedAt DESC")
    fun getAllPersonalRecords(): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getPersonalRecord(exerciseId: String): PersonalRecord?

    // ─── Water Logs ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog)

    @Query("SELECT COALESCE(SUM(amountMl),0) FROM water_logs WHERE dayKey = :dayKey")
    fun getTodayWaterMl(dayKey: Long): Flow<Int>

    @Query("DELETE FROM water_logs WHERE dayKey = :dayKey")
    suspend fun clearTodayWater(dayKey: Long)

    // ─── Body Measurements ────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMeasurement(m: BodyMeasurement)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteBodyMeasurement(id: Int)

    @Query("SELECT * FROM body_measurements ORDER BY loggedAt DESC LIMIT 50")
    fun getRecentBodyMeasurements(): Flow<List<BodyMeasurement>>

    // ─── Workout Notes ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutNote(note: WorkoutNote)

    @Query("SELECT * FROM workout_notes WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getNoteForSession(sessionId: Long): WorkoutNote?

    // ─── RPE Logs ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRpeLog(log: RpeLog)

    @Query("SELECT * FROM rpe_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber ASC")
    fun getRpeLogsForSession(sessionId: Long): Flow<List<RpeLog>>

    // ─── Custom Meals ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMeal(meal: CustomMeal)

    @Query("SELECT * FROM custom_meals ORDER BY createdAt DESC")
    fun getAllCustomMeals(): Flow<List<CustomMeal>>

    @Query("DELETE FROM custom_meals WHERE id = :id")
    suspend fun deleteCustomMeal(id: Int)

    @Query("DELETE FROM exercises WHERE id IN (:ids)")
    suspend fun deleteExercisesByIds(ids: List<String>)
}