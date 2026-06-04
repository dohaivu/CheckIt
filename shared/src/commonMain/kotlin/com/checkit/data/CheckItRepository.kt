package com.checkit.data

interface CheckItRepository {

}

class RoomCheckItRepository(
    private val dao: CheckItDao,
) : CheckItRepository {



}
