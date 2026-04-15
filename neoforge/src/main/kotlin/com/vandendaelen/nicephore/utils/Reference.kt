package com.vandendaelen.nicephore.utils

object Reference {
    const val DOWNLOADS_URLS: String = "https://raw.githubusercontent.com/LotuxPunk/Nicephore/master/references/v1.1/REFERENCES.json"
    const val VERSION: String = "1"
    const val OXIPNG_EXE: String = "oxipng.exe"
    const val ECT_EXE: String = "ect-0.8.3.exe"

    object Command {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }

    object File {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }

    object Version {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }
}
