package com.ezgiyilmaz.yllkzintakipuygulamas

data class LeaveRequest(
    val startDate: String,
    val endDate: String,
    val leaveType: String,
    val reason: String,
    val remainingLeaveDays: Int
)

