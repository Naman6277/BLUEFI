package com.example.data.model

enum class AppLockType(val label: String, val description: String) {
  PIN_4("4-Digit PIN", "Numeric 4-digit code (e.g. 1234)"),
  PIN_6("6-Digit PIN", "Numeric 6-digit code (e.g. 123456)"),
  PASSWORD("Password", "Alphanumeric & special characters (e.g. Pass@123)")
}
