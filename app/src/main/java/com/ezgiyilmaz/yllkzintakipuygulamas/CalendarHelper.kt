package com.ezgiyilmaz.yllkzintakipuygulamas

import java.util.Calendar
import java.util.Date



    object CalendarHelper {

        fun getCurrentDateTime(): Date {
            return Calendar.getInstance().time
        }

        fun getCurrentDateInMills(): Long {
            return Calendar.getInstance().timeInMillis
        }
    }
