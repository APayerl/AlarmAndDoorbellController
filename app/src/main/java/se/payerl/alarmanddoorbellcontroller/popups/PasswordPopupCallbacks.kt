package se.payerl.alarmanddoorbellcontroller.popups

interface PasswordPopupCallbacks {
    fun onSuccess(code: String)
    fun onWrongPassword(enteredPass: String, validCodes: List<String>)
    fun onCancel()
}