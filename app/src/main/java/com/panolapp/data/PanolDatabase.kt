package com.panolapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EmpleadoEntity::class, TabletEntity::class, MovimientoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PanolDatabase : RoomDatabase() {
    abstract fun dao(): PanolDao

    companion object {
        const val NOMBRE = "panol.db"

        fun crear(contexto: Context): PanolDatabase =
            Room.databaseBuilder(contexto.applicationContext, PanolDatabase::class.java, NOMBRE)
                .build()
    }
}
