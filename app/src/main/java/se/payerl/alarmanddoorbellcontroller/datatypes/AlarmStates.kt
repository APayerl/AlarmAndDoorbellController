package se.payerl.alarmanddoorbellcontroller.datatypes

abstract class AlarmStates {
    companion object {
        val DISARMED: String = "disarmed"
        val PENDING: String = "pending"
        val ARMED_HOME: String = "armed_home"
        val ARMED_AWAY: String = "armed_away"
        val TRIGGERED: String = "triggered"
        val ARMING: String = "arming"
    }
}