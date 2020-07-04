package com.isaiahvonrundstedt.fokus.features.history

import android.os.Parcelable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.database.converter.DateTimeConverter
import kotlinx.android.parcel.Parcelize
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

@Parcelize
@Entity(tableName = "histories")
data class History @JvmOverloads constructor (
    @PrimaryKey
    var historyID: String = UUID.randomUUID().toString(),
    var title: String? = null,
    var content: String? = null,
    var data: String? = null,
    var type: Int = TYPE_GENERIC,
    var isPersistent: Boolean = false,
    @TypeConverters(DateTimeConverter::class)
    var dateTimeTriggered: DateTime? = null
): Parcelable {

    fun tintDrawable(sourceView: ImageView) {
        val colorID = if (type == TYPE_GENERIC) R.color.colorIconReminder else R.color.colorIconWarning
        sourceView.setImageDrawable(sourceView.drawable.mutate().apply {
            colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(ContextCompat.getColor(sourceView.context, colorID),
                    BlendModeCompat.SRC_ATOP)
        })
    }

    fun formatDateTime(): String {
        val currentDateTime = LocalDate.now()

        // Formats the dateTime object for human reading
        return if (dateTimeTriggered!!.toLocalDate().isEqual(currentDateTime))
            DateTimeFormat.forPattern(DateTimeConverter.timeFormat).print(dateTimeTriggered)
        else if (dateTimeTriggered!!.toLocalDate().year == currentDateTime.year)
            DateTimeFormat.forPattern("MMMM d").print(dateTimeTriggered!!)
        else DateTimeFormat.forPattern("MMMM d yyyy").print(dateTimeTriggered!!)
    }

    companion object {
        const val TYPE_GENERIC = 0
        const val TYPE_TASK = 1
        const val TYPE_EVENT = 2
        const val TYPE_CLASS = 3
    }
}