package com.virgilsecurity.chat.nexmo.db.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "users",
        indices= arrayOf(Index(value = "user_id", name = "idx", unique = true))
)
class User (
        @PrimaryKey(autoGenerate = false)
        @ColumnInfo(name = "user_id")
        var userId: String = "",

        @ColumnInfo(name = "user_name")
        var name: String = "",

        @ColumnInfo(name = "href")
        var href: String = "",

        @ColumnInfo(name = "virgil_card_id")
        var virgilCardId: String = "",

        @ColumnInfo(name = "virgil_card")
        var virgilCard: String = "",

        @ColumnInfo(name = "virgil_key")
        var virgilKey: ByteArray
) {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as User

                if (userId != other.userId) return false
                if (name != other.name) return false
                if (href != other.href) return false
                if (virgilCard != other.virgilCard) return false
                if (virgilCardId != other.virgilCardId) return false
                if (!Arrays.equals(virgilKey, other.virgilKey)) return false

                return true
        }

        override fun hashCode(): Int {
                var result = userId.hashCode()
                result = 31 * result + name.hashCode()
                return result
        }
}