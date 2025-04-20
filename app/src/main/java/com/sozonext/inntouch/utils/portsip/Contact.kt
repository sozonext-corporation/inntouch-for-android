package com.sozonext.inntouch.utils.portsip

class Contact {

    var subRequestDescription: String? = null

    enum class SUBSCRIBE_STATE_FLAG {
        UNSETTLLED,
        ACCEPTED,
        REJECTED,
        UNSUBSCRIBE,
    }

    var sipAddr: String? = null
    var subDescription: String? = null
    var subScribeRemote: Boolean = false

    var subId: Long = 0 //if SubId >0 means received remote subscribe
    var state: SUBSCRIBE_STATE_FLAG = SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE // weigher accept remote subscribe
    //Not being subscripted

    fun currentStatusToString(): String {
        var status = ""

        status += "Subscribe：$subScribeRemote"
        status += "  Remote presence is：$subDescription"


        status += " Subscription received:($subRequestDescription)"
        status += when (state) {
            SUBSCRIBE_STATE_FLAG.ACCEPTED -> "Accepted"
            SUBSCRIBE_STATE_FLAG.REJECTED -> "Rejected"
            SUBSCRIBE_STATE_FLAG.UNSETTLLED -> "Pending"
            SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE -> "Not subscripted"
        }

        return status
    }

    init {
        //Not subscripted
    }
}


