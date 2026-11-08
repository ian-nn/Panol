package com.panolapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val SELECT_TABLET =
    "SELECT t.*, e.nombre AS nombre_empleado " +
        "FROM tablets t LEFT JOIN empleados e ON e.legajo = t.legajo_actual "

private const val SELECT_MOVIMIENTO =
    "SELECT m.*, e.nombre AS nombre_empleado " +
        "FROM movimientos m LEFT JOIN empleados e ON e.legajo = m.legajo "

@Dao
interface PanolDao {

    // ---- Lecturas puntuales (dentro de transacciones) ----

    @Query("SELECT * FROM tablets WHERE serie = :serie")
    suspend fun tablet(serie: String): TabletEntity?

    @Query(SELECT_TABLET + "WHERE t.serie = :serie")
    suspend fun buscarTablet(serie: String): TabletConNombre?

    @Query("SELECT nombre FROM empleados WHERE legajo = :legajo")
    suspend fun nombreDeEmpleado(legajo: String): String?

    // ---- Lecturas observables (la UI se actualiza sola) ----

    @Query(SELECT_TABLET + "WHERE t.serie = :serie")
    fun observarTablet(serie: String): Flow<TabletConNombre?>

    @Query(
        SELECT_TABLET +
            "WHERE (:estado IS NULL OR t.estado = :estado) " +
            "AND (:consulta = '' " +
            "OR t.serie LIKE '%' || :consulta || '%' " +
            "OR IFNULL(t.legajo_actual, '') LIKE '%' || :consulta || '%' " +
            "OR IFNULL(e.nombre, '') LIKE '%' || :consulta || '%') " +
            "ORDER BY t.serie",
    )
    fun observarInventario(estado: String?, consulta: String): Flow<List<TabletConNombre>>

    @Query("SELECT estado, COUNT(*) AS cantidad FROM tablets GROUP BY estado")
    fun observarConteos(): Flow<List<ConteoEstado>>

    @Query(
        SELECT_MOVIMIENTO +
            "WHERE m.serie = :serie ORDER BY m.fecha DESC, m.id DESC LIMIT :limite",
    )
    fun observarMovimientos(serie: String, limite: Int): Flow<List<MovimientoConNombre>>

    // ---- Escrituras ----

    @Insert
    suspend fun insertarTablet(tablet: TabletEntity)

    @Update
    suspend fun actualizarTablet(tablet: TabletEntity)

    /**
     * Upsert (no REPLACE): REPLACE borraría la fila del empleado y violaría la clave foránea
     * de las tablets que la referencian.
     */
    @Upsert
    suspend fun guardarEmpleado(empleado: EmpleadoEntity)

    @Insert
    suspend fun insertarMovimiento(movimiento: MovimientoEntity): Long
}
