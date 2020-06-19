package com.chengte99.kotlinbingo

data class Member(var uid: String,
                  var displayName: String,
                  var nickname: String?, var avatar: Int) {
    constructor() : this("", "", null, 0)
}
